package xh.zero.camerax

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import xh.zero.databinding.FragmentCameraXPreviewBinding
import xh.zero.view.CameraXFragment
import xh.zero.widgets.BaseSurfaceView

class CameraXPreviewFragment : CameraXFragment<FragmentCameraXPreviewBinding>() {

    override val cameraId: String by lazy { arguments?.getString("cameraId") ?: "0" }
    private var listener: OnFragmentActionListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentActionListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun getBindingView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCameraXPreviewBinding {
        return FragmentCameraXPreviewBinding.inflate(inflater, container, false)
    }

    override fun getSurfaceView(): BaseSurfaceView = binding.viewfinder

    override var captureSize: Size? = Size(1024, 768)

    override val surfaceRatio: Size = Size(4, 3)

    override fun onFocusTap(x: Float, y: Float) {
    }

    override fun onAnalysisImage(bitmap: Bitmap) {
        listener?.onAnalysisImage(bitmap)
    }

    interface OnFragmentActionListener {
        fun onAnalysisImage(bitmap: Bitmap)
    }

    companion object {
        fun newInstance(id: String) = CameraXPreviewFragment().apply {
            arguments = Bundle().apply {
                putString("cameraId", id)
            }
        }
    }
}