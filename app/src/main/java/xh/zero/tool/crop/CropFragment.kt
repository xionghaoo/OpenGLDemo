package xh.zero.tool.crop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import xh.zero.databinding.FragmentCropBinding
import xh.zero.tool.ToolFragment
import xh.zero.view.Camera2Fragment
import xh.zero.widgets.BaseSurfaceView

class CropFragment : Camera2Fragment<FragmentCropBinding>() {
    override val cameraId: String by lazy {
        arguments?.getString("id") ?: "0"
    }

    override fun getBindingView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCropBinding {
        return FragmentCropBinding.inflate(inflater, container, false)
    }

    override fun getSurfaceView(): BaseSurfaceView {
        return binding.viewfinder
    }

    companion object {
        fun newInstance(id: String) = CropFragment().apply {
            arguments = Bundle().apply {
                putString("id", id)
            }
        }
    }
}