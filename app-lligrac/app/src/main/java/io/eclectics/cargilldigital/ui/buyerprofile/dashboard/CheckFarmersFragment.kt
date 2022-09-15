package io.eclectics.cargilldigital.ui.buyerprofile.dashboard

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import dagger.hilt.android.AndroidEntryPoint
import io.eclectics.cargilldigital.R
import io.eclectics.cargilldigital.adapter.FarmerListener
import io.eclectics.cargilldigital.adapter.FarmersAdapter
import io.eclectics.cargilldigital.databinding.FragmentCheckFarmersBinding
import io.eclectics.cargilldigital.data.model.Section
import io.eclectics.cargilldigital.data.model.UserDetailsObj
import io.eclectics.cargilldigital.network.ApiEndpointObj
import io.eclectics.cargilldigital.ui.bottomsheet.BottomSheetFragment
import io.eclectics.cargilldigital.ui.bottomsheet.OnActionTaken
import io.eclectics.cargilldigital.utils.ToolBarMgmt
import io.eclectics.cargilldigital.utils.UtilPreference
import io.eclectics.cargilldigital.viewmodel.BuyerRoomViewModel
import io.eclectics.cargilldigital.viewmodel.BuyerViewModel
import io.eclectics.cargilldigital.viewmodel.ViewModelWrapper
import io.eclectics.cargill.model.FarmerModelObj
import io.eclectics.cargill.model.FarmersModel
import io.eclectics.cargilldigital.utils.GlobalMethods
import io.eclectics.cargilldigital.utils.LoggerHelper
import io.eclectics.cargill.utils.NetworkUtility
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * A simple [Fragment] subclass.
 * Use the [CheckFarmersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class CheckFarmersFragment : Fragment(), OnActionTaken {

    private var _binding: FragmentCheckFarmersBinding? = null
    private val binding get() = _binding!!
    lateinit var  farmersList:List<FarmerModelObj>
    lateinit var  searchFList:ArrayList<FarmerModelObj>
    lateinit var  adapter : FarmersAdapter
    private lateinit var bottomSheetFragment: BottomSheetFragment
    @Inject
    lateinit var navOptions:NavOptions
    val buyerViewModel: BuyerViewModel by viewModels()
    val buyerRoomVeiwModel:BuyerRoomViewModel by viewModels()

    lateinit var userData: UserDetailsObj
    @Inject
    lateinit var pdialog: SweetAlertDialog
    private var globalMethods = GlobalMethods()
    var isBottomSheetUp by Delegates.notNull<Boolean>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckFarmersBinding.inflate(inflater, container, false)
       // farmViewModel = ViewModelProvider(requireActivity()).get(FarmViewModel::class.java)
        pdialog = SweetAlertDialog(requireActivity(), SweetAlertDialog.PROGRESS_TYPE)
        ToolBarMgmt.setPayfarmerToolbarTitle(resources.getString(R.string.pay_farmer_directly_title),resources.getString(R.string.pay_or_scan),binding.mainLayoutToolbar,requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // we can find the outer NavController passing the owning Activity
        // and the id of a view associated to that NavController,
        // for example the NavHostFragment id:
       // mainNavController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
        var userJson  = UtilPreference().getUserData(activity)
        userData = NetworkUtility.jsonResponse(userJson)
        var sectionObj: Section = NetworkUtility.jsonResponse(userData.section.toString())
         lifecycleScope.launch {
            //NetworkUtility().sendRequest(pDialog)
            //sendFarmregRequest() cooperativeid
            var endpoint  =  ApiEndpointObj.buyerFarmersList
            var lookupJson = JSONObject()
            lookupJson.put("cooperativeid",userData.cooperativeId)
            lookupJson.put("sectionid",sectionObj.id.toString())
            lookupJson.put("phonenumber",userData.phoneNumber)
            lookupJson.put("endPoint",endpoint)
            // setDummyData()
            pdialog.show()
            getFarmerList(lookupJson,endpoint)
        }
        setupListeners()

    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.fab.setOnClickListener {
            showDropDown(null, "add_farmer")
        }

        binding.tvFarmerNo.text = Html.fromHtml(
            getString(
                R.string.total_no,
                globalMethods.getColoredSpanned("4", "#000000")
            )
        )

        binding.ivFilter.setOnClickListener {
            val filter = binding.etSearch.text.toString()
            Toast.makeText(requireContext(), filter, Toast.LENGTH_SHORT).show()

        }

    }

    private suspend fun getFarmerList(lookupJson: JSONObject, endpoint: String) {
        try {
            buyerViewModel.getFarmersList(lookupJson,endpoint,requireActivity()).observe(requireActivity(), Observer {
                pdialog.dismiss()
                when (it) {
                    is ViewModelWrapper.error -> GlobalMethods().transactionWarning(
                        requireActivity(),
                        "${it.error}"
                    )//LoggerHelper.loggerError("error","error")
                    is ViewModelWrapper.response -> processRequest(it.value)//LoggerHelper.loggerSuccess("success","success ${it.value}")
                }
            })
        }catch (ex:Exception){
            LoggerHelper.loggerError("fetchErorr"," ${ex.message} error fetching data")}
    }

    private fun processRequest(response: String) {
        farmersList = NetworkUtility.jsonResponse(response)
        /*var adapter = FarmersAdapter(FarmerListener { farmer, action ->
            handleAction(farmer, action)
        })*/
        if(farmersList.isNotEmpty()){
            searchFList = ArrayList(farmersList)
            adapter = FarmersAdapter(FarmerListener { farmer, action ->
                handleAction(farmer, action)
            },farmersList)
            binding.rvFarmers.adapter = adapter
            binding.tvFarmerNo.text = Html.fromHtml(
                getString(
                    R.string.total_no,
                    globalMethods.getColoredSpanned(farmersList.size.toString(), "#000000")
                )
            )
        }else{
            binding.tvErrorResponse.visibility = View.VISIBLE
            binding.rvFarmers.visibility = View.GONE
        }
        //save farmers list offline
        try {
             lifecycleScope.launch {
                buyerRoomVeiwModel.insertFarmerList(requireActivity(), response)
            }
        }catch (ex:Exception){}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            if(isBottomSheetUp) {
                bottomSheetFragment.dismiss()
            }
        }catch (ex:Exception){}
    }

    private fun handleAction(farmer2: FarmerModelObj, action: String) {
//        var bundle = bundleOf("farmer" to FarmersModel)
        var farmer = FarmersModel(farmer2.firstName,farmer2.lastName,farmer2.phoneNumber,farmer2.location,farmer2.certificationnumber,"","","",farmer2.datecreated,farmer2.emailAddress,3)
        when (action) {
            "Pay Farmer" -> {
                val args = Bundle()
                //override the parcelable and pass json string ni ju ya demo
                var farmerJson = NetworkUtility.getJsonParser().toJson(farmer2)
                args.putParcelable("farmerobj", farmer)
                args.putString("farmerobj",farmerJson)//nav_ffQRpayfarmer    nav_payFarmer
                findNavController().navigate(R.id.nav_ffQRpayfarmer,args,navOptions)
                //nav_agentCocoPayment  action_agentFragment2_to_collectionFragment
                //nav_agentCocoPayment
            }
            "farmer_details" -> {
                val args = Bundle()
                args.putParcelable("farmer", farmer)
                //add profile tag to distinguish farm detail fromfarmer and agent profile
                args.putString("profile","agent")

                /*mainNavController.navigate(
                    R.id.action_agentFragment2_to_farmerProfileFragment,
                    args
                )*/
            }
            "farm_details" -> showDropDown(farmer, action)
        }

    }

    private fun showDropDown(farmer: FarmersModel?, action: String) {
        // using BottomSheetDialogFragment
        bottomSheetFragment = BottomSheetFragment(this, farmer, action)
        isBottomSheetUp = true
        bottomSheetFragment.show(
            requireActivity().supportFragmentManager,
            bottomSheetFragment.tag
        )
    }

    override fun onActionChosen(value: String?) {
        value?.let {
            when (it) {
                "Pay" -> {
                    globalMethods.confirmTransactionEnd("Payment sent successfully", requireActivity())
                    bottomSheetFragment.dismiss()
                }
                "add_farmer" -> {
                    globalMethods.confirmTransactionEnd("Farmer Added successfully", requireActivity())
                    bottomSheetFragment.dismiss()
                }
                else -> {
                    bottomSheetFragment.dismiss()
                }
            }
        }
    }

    private fun setupListeners() = binding.etSearch.addTextChangedListener {
        adapter.filter.filter(it.toString())
    }

}