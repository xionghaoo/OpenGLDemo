package xh.zero.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import xh.zero.view.Camera2Fragment
import xh.zero.databinding.FragmentSilentCaptureBinding
import xh.zero.widgets.BaseSurfaceView

class SilentCaptureFragment : Camera2Fragment<FragmentSilentCaptureBinding>() {

    override val cameraId: String by lazy {
        arguments?.getString("cameraId") ?: "0"
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSilentCaptureBinding {
        return FragmentSilentCaptureBinding.inflate(inflater, container, false)
    }

    override fun getSurfaceView(): BaseSurfaceView = binding.viewfinder

    companion object {
        fun newInstance(cameraId: String) =
            SilentCaptureFragment().apply {
                arguments = Bundle().apply {
                    putString("cameraId", cameraId)
                }
            }
    }
}