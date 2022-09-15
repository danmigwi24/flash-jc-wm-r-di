package com.ekenya.rnd.tijara.ui.auth.registration

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ekenya.rnd.tijara.R
import com.ekenya.rnd.tijara.databinding.FragmentSelfieBinding
import com.ekenya.rnd.tijara.utils.*
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.IOException

class SelfieFragment : Fragment() {
    private lateinit var binding:FragmentSelfieBinding
    private lateinit var viewModel:RegistrationViewModel
    private lateinit var destinationUri: Uri
    private lateinit var selfieFile: File
    private var selfiePath: String? = null
    companion object {
        private const val CAMERA_REQUEST_CODE = 2
        private const val PERMISSION_REQUEST_CODE = 0
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       binding= FragmentSelfieBinding.inflate(layoutInflater)
        viewModel= ViewModelProvider(requireActivity()).get(RegistrationViewModel::class.java)
        binding.apply {
            binding.imageBack.setOnClickListener {
                findNavController().navigateUp()
            }


            btnContinue.setOnClickListener {
                if (!::selfieFile.isInitialized) {
                    toastyInfos("please take a selfie")
                } else {
                    viewModel.selfiePhoto=selfieFile
                    findNavController().navigate(R.id.action_selfieFragment_to_moreDetailFragment)
                }

            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mContextWrapper = ContextWrapper(requireContext())
        val mDirectory: File = mContextWrapper.getDir("Pictures",
            Context.MODE_PRIVATE)
        val photoPath = File(mDirectory, "Selfiephoto.png")
        destinationUri = Uri.parse(photoPath.path)
        binding.btnTakeSelfie.setOnClickListener {
            cameraCheckPermission()
        }
    }
    private fun takePictures(){
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireContext().packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    toastyErrors("Failed to capture photo")
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.eclectics.rnd.baseapp.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent,
                        SelfieFragment.CAMERA_REQUEST_CODE
                    )
                }
            }
        }
    }
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val directory = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${uniqueFileName()}_", /* prefix */
            ".jpg", /* suffix */
            directory /* directory */
        ).apply {
            selfiePath=absolutePath
        }
    }

    private fun cameraCheckPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            takePictures()
        } else {
            /**else if we dont have permission to use the cam we request for it*/
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
               SelfieFragment.PERMISSION_REQUEST_CODE
            )

        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            SelfieFragment.PERMISSION_REQUEST_CODE -> {
                // permission was granted
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                    /**we use camera functionality*/
                    takePictures()
                } else {
                    toastyInfos(
                        "Camera Permission was denied!! \n But don't worry you can allow this permission on the app setting",
                    )
                }
            }
        }

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            /**we display the data as bitmap and set our image to that data
             * nb// you are not supposed to change the key here ,else the app will crash with
             * kotlin.TypeCastException: null cannot be cast to non-null type android.graphics.Bitmap*/
            when(requestCode){
                SelfieFragment.CAMERA_REQUEST_CODE ->{
                    val file = File(selfiePath!!)
                    val uri = Uri.fromFile(file)
                    UCrop.of(uri, destinationUri)
                        .withAspectRatio(5f, 5f)
                        .start(requireContext(),this)

                }
                UCrop.REQUEST_CROP->{
                    val uri: Uri = UCrop.getOutput(data!!)!!
                    val filePath = getRealPathFromURIPath(uri, requireActivity())
                    selfieFile = File(filePath)
                    val bitmap = if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        MediaStore.Images.Media.getBitmap(
                            requireActivity().contentResolver,
                            uri
                        )

                    } else {
                        val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    }
                    binding.selfieIv.makeGone()
                    binding.selfieTaken.makeVisible()
                    binding.btnContinue.visibility=View.VISIBLE
                    binding.btnTakeSelfie.makeGone()
                    binding.selfieTaken.setImageBitmap(bitmap)

                }
            }

        }
    }


}