package com.example.distancetrackerapp.ui.maps

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.distancetrackerapp.R
import com.example.distancetrackerapp.databinding.FragmentMapsBinding
import com.example.distancetrackerapp.model.result
import com.example.distancetrackerapp.services.Trackerservice
import com.example.distancetrackerapp.utils.Extensionfunction.disable
import com.example.distancetrackerapp.utils.Extensionfunction.enable
import com.example.distancetrackerapp.utils.Extensionfunction.hide
import com.example.distancetrackerapp.utils.Extensionfunction.show
import com.example.distancetrackerapp.utils.MapUtil.CalculateElapsedTime
import com.example.distancetrackerapp.utils.MapUtil.calculateDistance
import com.example.distancetrackerapp.utils.MapUtil.setcameraposition
import com.example.distancetrackerapp.utils.Permissions.hasbackgroundLocationPermission
import com.example.distancetrackerapp.utils.Permissions.requestbackgroundlocationpermisson
import com.example.distancetrackerapp.utils.constants.ACTION_SERVICE_START
import com.example.distancetrackerapp.utils.constants.ACTION_SERVICE_STOP
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MapsFragment : Fragment(),OnMapReadyCallback,OnMyLocationButtonClickListener,EasyPermissions.PermissionCallbacks,OnMarkerClickListener {

    private var Mapsbinding:FragmentMapsBinding? = null
    private val binding get() = Mapsbinding!!
    private lateinit var map:GoogleMap
    private var locationlist = mutableListOf<LatLng>()
    private var polylinelist = mutableListOf<Polyline>()
    private var markerlist = mutableListOf<Marker>()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var startTime = 0L
    private var stopTime = 0L

    val started = MutableLiveData(false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Mapsbinding =  FragmentMapsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.tracking =this

        binding.startButton.setOnClickListener {
            onstartbuttonclicked()
        }
        binding.stopButton.setOnClickListener {
            onstopbuttonclicked()
        }
        binding.resetButton.setOnClickListener {
            onresetbuttonclicked()
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission", "PotentialBehaviorOverride")
    override fun onMapReady(googlemap: GoogleMap) {
        map = googlemap
        map.apply {
            isMyLocationEnabled = true
        }
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMarkerClickListener(this)
        map.uiSettings.apply {
            isZoomControlsEnabled   = false
            isZoomGesturesEnabled   = false
            isTiltGesturesEnabled   = false
            isCompassEnabled        = false
            isRotateGesturesEnabled = false
            isScrollGesturesEnabled = false
        }
        observetrackerService()
    }

    private fun observetrackerService(){
        Trackerservice.locationlist.observe(viewLifecycleOwner, {
            if(it != null){
                locationlist = it
                if(locationlist.size > 1){
                    binding.stopButton.enable()
                }
                drawpolyline()
                followPolyline()
            }
        })

        Trackerservice.starttime.observe(viewLifecycleOwner,  {
            startTime = it
        })

        Trackerservice.stoptime.observe(viewLifecycleOwner,  {
            stopTime = it
            if(stopTime != 0L){
                showbiggerpicture()
                displayresults()
            }
        })

        Trackerservice.started.observe(viewLifecycleOwner,{
            started.value = it
        })
    }

    private fun showbiggerpicture() {
        val bounds = LatLngBounds.builder()
        for(location in locationlist){
            bounds.include(location)
        }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),100
            ),2000,null
        )
        addMarker(locationlist.first())
        addMarker(locationlist.last())
    }

    private fun addMarker(location : LatLng){
        val marker = map.addMarker(
            MarkerOptions().position(location)
        )
        if (marker != null) {
            markerlist.add(marker)
        }
    }

    private fun displayresults(){
        val result = result(
            calculateDistance(locationlist),
            CalculateElapsedTime(startTime,stopTime)
        )

        lifecycleScope.launch{
            delay(2500)
            val action = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(action)
            binding.startButton.apply {
                hide()
                enable()
            }
            binding.stopButton.hide()
            binding.resetButton.show()
        }



    }

    private fun drawpolyline(){
        val polyline = map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.BLUE)
                jointType(JointType.ROUND)
                startCap(ButtCap())
                endCap(ButtCap())
                addAll(locationlist)
            }
        )

        polylinelist.add(polyline)
    }

    private fun followPolyline(){
        if(locationlist.isNotEmpty()){
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    setcameraposition(locationlist.last())
                ),1000,null
            )
        }
    }

    private fun onstartbuttonclicked() {
        if(hasbackgroundLocationPermission(requireContext())){
            startcountdown()
            binding.startButton.hide()
            binding.startButton.disable()
            binding.stopButton.show()
        }else{
            requestbackgroundlocationpermisson(this)
        }
    }

    private fun startcountdown() {
        binding.timerTextView.show()
        binding.stopButton.disable()

        val timer:CountDownTimer = object : CountDownTimer(4000,1000){
            override fun onTick(millsuntilfinished: Long) {
                val currsec = millsuntilfinished / 1000
                if(currsec.toString() == "0"){
                    binding.timerTextView.text = "GO"
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),R.color.black))
                }else{
                    binding.timerTextView.text = currsec.toString()
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),R.color.red))
                }
            }
            override fun onFinish() {
                sendActionCommandtoTrackerservice(ACTION_SERVICE_START)
                binding.timerTextView.hide()
            }
        }
        timer.start()
    }

    private fun onstopbuttonclicked() {
        stopforegroundservice()
    }

    private fun stopforegroundservice() {
        binding.startButton.show()
        binding.stopButton.hide()
        binding.startButton.disable()
        sendActionCommandtoTrackerservice(ACTION_SERVICE_STOP)
    }

    private fun onresetbuttonclicked() {
        Mapreset()
    }

    @SuppressLint("MissingPermission")
    private fun Mapreset() {
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val lastknownlocation = LatLng(
                it.result.latitude,
                it.result.longitude
            )
            for(polyline in polylinelist){
                polyline.remove()
            }
            for(marker in markerlist){
                marker.remove()
            }

            map.animateCamera(CameraUpdateFactory.newCameraPosition(setcameraposition(lastknownlocation)))

            locationlist.clear()
            markerlist.clear()

            binding.resetButton.hide()
            binding.startButton.show()
        }
    }

    private fun sendActionCommandtoTrackerservice(action:String){
        Intent(requireContext(), Trackerservice::class.java).apply {
            this.action = action
            requireContext().startService(this)
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        binding.hintTextView.animate().alpha(0f).duration = 1500
        lifecycleScope.launch {
            delay(2000)
            binding.hintTextView.hide()
            binding.startButton.show()
        }
        return false
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
            SettingsDialog.Builder(requireActivity()).build()
        }else{
            requestbackgroundlocationpermisson(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onstartbuttonclicked()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Mapsbinding = null
    }
}