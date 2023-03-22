package com.example.distancetrackerapp.utils

import android.graphics.Camera
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.text.DecimalFormat

object MapUtil {

    fun setcameraposition(location:LatLng):CameraPosition{
        return CameraPosition.Builder()
            .target(location)
            .zoom(20f)
            .build()
    }

    fun CalculateElapsedTime(starttime:Long,stoptime:Long):String{
        val elapsedtime = stoptime - starttime

        val sec = (elapsedtime / 1000).toInt() % 60
        val min = (elapsedtime / (1000 * 60) % 60)
        val hr = (elapsedtime / (1000 * 60 * 60) % 24)

        return "$hr:$min:$sec"
    }

    fun calculateDistance(locationlist:MutableList<LatLng>):String{
        if(locationlist.size > 1){
            val meters = SphericalUtil.computeDistanceBetween(locationlist.first(),locationlist.last())

            val kilometer = meters / 1000

            return DecimalFormat("#.##").format(kilometer)
        }
        return "0.00"
    }
}