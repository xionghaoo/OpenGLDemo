package xh.zero.tools

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import xh.zero.R
import xh.zero.camera2.Camera2Fragment
import xh.zero.camera2.Camera2SilentFragment
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