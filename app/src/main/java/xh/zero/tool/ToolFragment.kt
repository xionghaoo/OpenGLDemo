package xh.zero.tool

import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import xh.zero.databinding.FragmentToolBinding
import xh.zero.view.Camera2Fragment
import xh.zero.widgets.BaseSurfaceView

class ToolFragment : Camera2Fragment<FragmentToolBinding>() {
    override val cameraId: String by lazy {
        arguments?.getString("id") ?: "0"
    }

    fun setSize(w: Int?, h: Int?) {
        cameraWidth = w
        cameraHeight = h
    }

    override fun getBindingView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolBinding {
        return FragmentToolBinding.inflate(inflater, container, false)
    }

    override fun getSurfaceView(): BaseSurfaceView {
        return binding.viewfinder
    }

    companion object {
        fun newInstance(id: String) = ToolFragment().apply {
            arguments = Bundle().apply {
                putString("id", id)
            }
        }
    }
}