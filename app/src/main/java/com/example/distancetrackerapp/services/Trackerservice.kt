package com.example.distancetrackerapp.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.distancetrackerapp.utils.MapUtil
import com.example.distancetrackerapp.utils.constants.ACTION_SERVICE_START
import com.example.distancetrackerapp.utils.constants.ACTION_SERVICE_STOP
import com.example.distancetrackerapp.utils.constants.LOCATION_FASTEST_UPDATE_INTERVAL
import com.example.distancetrackerapp.utils.constants.LOCATION_UPDATE_INTERVAL
import com.example.distancetrackerapp.utils.constants.NOTIFICATION_CHANNEL_ID
import com.example.distancetrackerapp.utils.constants.NOTIFICATION_CHANNEL_NAME
import com.example.distancetrackerapp.utils.constants.NOTIFICATION_ID
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Trackerservice:LifecycleService() {
    @Inject
    lateinit var notification:NotificationCompat.Builder

    @Inject
    lateinit var notificationManager:NotificationManager

    private lateinit var fusedlocationproviderclient:FusedLocationProviderClient

    companion object{
        val started = MutableLiveData<Boolean>()

        var starttime = MutableLiveData<Long>()
        var stoptime = MutableLiveData<Long>()

        val locationlist = MutableLiveData<MutableList<LatLng>>()
    }

    val locationCallback = object : LocationCallback(){
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result.locations.let { locations ->
                for (location in locations){
                    updateLocationlist(location)
                    updatenotificationperiodidcally()
                }
            }
        }
    }

    private fun setinitialvalue(){
        started.postValue(false)

        starttime.postValue(0L)
        stoptime.postValue(0L)

        locationlist.postValue(mutableListOf())
    }

    override fun onCreate() {
        setinitialvalue()
        fusedlocationproviderclient = LocationServices.getFusedLocationProviderClient(this)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                ACTION_SERVICE_START -> {
                    started.postValue(true)
                    startforegroundservice()
                    startLocationupdates()
                }
                ACTION_SERVICE_STOP -> {
                    started.postValue(false)
                    stopforegroundservice()
                }
                else -> {

                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startforegroundservice(){
        Log.d(TAG, "startforegroundservice: inside startforeground")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification.build())
}

    private fun stopforegroundservice() {
       removelocationupdates()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            NOTIFICATION_ID
        )
        stopForeground(true)
        stopSelf()
        stoptime.postValue(System.currentTimeMillis())
    }

    @SuppressLint("MissingPermission")
    private fun startLocationupdates(){
        val locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_FASTEST_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedlocationproviderclient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        starttime.postValue(System.currentTimeMillis())
    }

    private fun removelocationupdates() {
        fusedlocationproviderclient.removeLocationUpdates(locationCallback)
    }

    private fun updateLocationlist(location:Location){
        val newlatlng = LatLng(location.latitude,location.longitude)
        locationlist.value?.apply {
            add(newlatlng)
            locationlist.postValue(this)
        }
    }

    private fun updatenotificationperiodidcally(){
        notification.apply {
            setContentTitle("Distance travelled")
            setContentText(locationlist.value?.let { MapUtil.calculateDistance(it) } + "km")
        }
        notificationManager.notify(NOTIFICATION_ID,notification.build())
    }

    fun createNotificationChannel(){
        Log.d(TAG, "startnotificationservice: inside notification")
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}