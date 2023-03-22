package com.example.distancetrackerapp.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.distancetrackerapp.R
import com.example.distancetrackerapp.databinding.FragmentResultBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class resultFragment : BottomSheetDialogFragment() {

    private var Rbinding : FragmentResultBinding?=null
    private val binding get() = Rbinding!!

    private val args:resultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        Rbinding = FragmentResultBinding.inflate(inflater, container, false)

        binding.distancevalueTextView.text = getString(R.string.result , args.result.distance)
        binding.timevalueTextView.text = args.result.time

        binding.shareButton.setOnClickListener {
            shareresult()
        }

        return binding.root
    }

    private fun shareresult() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT,"I went ${args.result.distance}km in ${args.result.time}!")
        }
        startActivity(shareIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Rbinding = null
    }
}