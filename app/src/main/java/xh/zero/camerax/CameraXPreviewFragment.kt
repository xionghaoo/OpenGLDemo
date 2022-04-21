package xh.zero.camerax

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import xh.zero.databinding.FragmentCameraXPreviewBinding
import xh.zero.view.CameraXFragment
import xh.zero.widgets.BaseSurfaceView

class CameraXPreviewFragment : CameraXFragment<FragmentCameraXPreviewBinding>() {

    override val cameraId: String by lazy { arguments?.getString("cameraId") ?: "0" }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCameraXPreviewBinding {
        return FragmentCameraXPreviewBinding.inflate(inflater, container, false)
    }

    override fun getSurfaceView(): BaseSurfaceView = binding.viewfinder

    companion object {
        fun newInstance(id: String) = CameraXPreviewFragment().apply {
            arguments = Bundle().apply {
                putString("cameraId", id)
            }
        }
    }
}