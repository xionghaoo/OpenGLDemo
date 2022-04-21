package xh.zero.camera1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import xh.zero.databinding.FragmentCamera1PreviewBinding
import xh.zero.widgets.BaseSurfaceView

class Camera1PreviewFragment : Camera1Fragment<FragmentCamera1PreviewBinding>() {

    override val cameraId: Int by lazy { arguments?.getInt(ARG_CAMERA_ID) ?: 0 }

    override fun surfaceView(): BaseSurfaceView = binding.viewfinder

    override fun getBindingView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCamera1PreviewBinding {
        return FragmentCamera1PreviewBinding.inflate(inflater, container, false)
    }

    companion object {

        private const val ARG_CAMERA_ID = "ARG_CAMERA_ID"


        fun newInstance(cameraId: Int) =
            Camera1PreviewFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_CAMERA_ID, cameraId)
                }
            }
    }
}