package xh.zero.camera2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import xh.zero.databinding.FragmentCamera2PreviewBinding
import xh.zero.widgets.BaseSurfaceView

class Camera2PreviewFragment: Camera2Fragment<FragmentCamera2PreviewBinding>() {
    override val cameraId: String by lazy {
        arguments?.getString("id") ?: "0"
    }


    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCamera2PreviewBinding {
        return FragmentCamera2PreviewBinding.inflate(inflater, container, false)
    }

    override fun getSurfaceView(): BaseSurfaceView {
        return binding.viewfinder
    }

    companion object {
        fun newInstance(id: String) = Camera2PreviewFragment().apply {
            arguments = Bundle().apply {
                putString("id", id)
            }
        }
    }
}