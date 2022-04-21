package xh.zero.filterchain

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import xh.zero.R
import xh.zero.camera2.Camera2Fragment
import xh.zero.databinding.FragmentFilterChainBinding
import xh.zero.widgets.BaseSurfaceView

class FilterChainFragment : Camera2Fragment<FragmentFilterChainBinding>() {

    override val cameraId: String by lazy {
        arguments?.getString("cameraId") ?: "0"
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFilterChainBinding {
        return FragmentFilterChainBinding.inflate(inflater, container, false)
    }

    override fun getSurfaceView(): BaseSurfaceView = binding.viewfinder

    companion object {
        fun newInstance(cameraId: String) =
            FilterChainFragment().apply {
                arguments = Bundle().apply {
                    putString("cameraId", cameraId)
                }
            }
    }
}