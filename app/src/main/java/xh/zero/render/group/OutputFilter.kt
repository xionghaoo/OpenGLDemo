package xh.zero.render.group

import android.content.Context
import timber.log.Timber

class OutputFilter(context: Context) : GpuImageFilter(context) {
    override fun onCreate() {
    }

    override fun beforeDrawArrays(textureId: Int) {
        Timber.d("OutputFilter: beforeDrawArrays")
    }
}