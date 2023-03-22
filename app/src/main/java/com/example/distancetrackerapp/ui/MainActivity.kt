package com.example.distancetrackerapp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.distancetrackerapp.R
import com.example.distancetrackerapp.utils.Permissions.hasLocationPermission

class MainActivity : AppCompatActivity() {

    private lateinit var  navController:NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navhostfragment = supportFragmentManager.findFragmentById(R.id.navhostfragment) as NavHostFragment
        navController = navhostfragment.findNavController()

        if(hasLocationPermission(this)){
          navController.navigate(R.id.action_permissionFragment_to_mapsFragment)
        }

    }
}