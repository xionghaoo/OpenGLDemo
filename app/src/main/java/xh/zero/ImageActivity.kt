package xh.zero

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import xh.zero.core.utils.SystemUtil
import xh.zero.databinding.ActivityImageBinding

class ImageActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context?, path: String?) {
            if (path == null) return
            context?.startActivity(Intent(context, ImageActivity::class.java).apply {
                putExtra("path", path)
            })
        }
    }

    private val imagePath by lazy {
        intent.getStringExtra("path")
    }

    private lateinit var binding: ActivityImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
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