package xh.zero.silentcamera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import xh.zero.databinding.FragmentSilentCaptureBinding
import xh.zero.view.Camera2SilentFragment

/**
 * 无预览拍照
 */
class SilentCaptureFragment : Camera2SilentFragment<FragmentSilentCaptureBinding>() {

    override val cameraId: String by lazy {
        arguments?.getString("cameraId") ?: "0"
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSilentCaptureBinding {
        return FragmentSilentCaptureBinding.inflate(inflater, container, false)
    }

    companion object {
        fun newInstance(cameraId: String) =
            SilentCaptureFragment().apply {
                arguments = Bundle().apply {
                    putString("cameraId", cameraId)
                }
            }
    }
}