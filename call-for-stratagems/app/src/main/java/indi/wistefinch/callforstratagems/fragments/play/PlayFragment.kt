package indi.wistefinch.callforstratagems.fragments.play

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import indi.wistefinch.callforstratagems.MainActivity
import indi.wistefinch.callforstratagems.databinding.FragmentPlayBinding

class PlayFragment : Fragment() {

    private var _binding: FragmentPlayBinding? = null
    private val binding get() = _binding!!

    private var oriSystemUiVisibility: Int = 0

    private lateinit var viewExit: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Set full screen
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        oriSystemUiVisibility = activity?.window?.decorView?.systemUiVisibility!!
        activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        // Hide toolbar
        (activity as MainActivity).supportActionBar?.hide()

        // Inflate the layout for this fragment
        _binding = FragmentPlayBinding.inflate(inflater, container, false)
        val view = binding.root

        viewExit = binding.playExit

        setupEventListener()

        return view
    }

    private fun setupEventListener() {
        // Exit button
        viewExit.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroy() {
        // Cancel full screen
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.window?.decorView?.systemUiVisibility = oriSystemUiVisibility

        // Show toolbar
        (activity as MainActivity).supportActionBar?.show()

        super.onDestroy()
    }
}