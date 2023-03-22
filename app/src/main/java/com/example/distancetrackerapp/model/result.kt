package com.example.distancetrackerapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class result(
    var distance:String,
    var time:String
):Parcelable
