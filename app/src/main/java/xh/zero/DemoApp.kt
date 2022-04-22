package xh.zero

import android.app.Application
import timber.log.Timber

class DemoApp : Application() {

    init {
        System.loadLibrary("opencv_java4")
        System.loadLibrary("zero")
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}