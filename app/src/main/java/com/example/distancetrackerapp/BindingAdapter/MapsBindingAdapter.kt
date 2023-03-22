package com.example.distancetrackerapp.BindingAdapter

import android.opengl.Visibility
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.TextView
import androidx.databinding.BindingAdapter

class MapsBindingAdapter {

    companion object{
        @BindingAdapter("android:observcetracking")
        @JvmStatic
        fun observceTracking(view:View,started:Boolean){
            if(started && view is Button){
                view.visibility = VISIBLE
            }else if(started && view is TextView){
                view.visibility = INVISIBLE
            }
        }
    }
}