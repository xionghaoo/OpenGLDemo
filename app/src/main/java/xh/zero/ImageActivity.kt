package xh.zero

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import xh.zero.core.utils.SystemUtil
import xh.zero.databinding.ActivityImageBinding

class ImageActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context?, path: String?, orientation: Int) {
            if (path == null) return
            context?.startActivity(Intent(context, ImageActivity::class.java).apply {
                putExtra("path", path)
                putExtra("orientation", orientation)
            })
        }
    }

    private val imagePath by lazy {
        intent.getStringExtra("path")
    }

    private val orientation by lazy {
        intent.getIntExtra("orientation", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    private lateinit var binding: ActivityImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        requestedOrientation = orientation

        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        Glide.with(this)
            .load(imagePath)
            .into(binding.ivResult)
    }
}