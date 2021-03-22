package xyz.ivaniskandar.shouko

import android.app.Application
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.ivaniskandar.shouko.service.TeaTileService
import xyz.ivaniskandar.shouko.util.isRootAvailable

class ShoukoApplication : Application() {
    private val wallpaperColorListener = WallpaperManager.OnColorsChangedListener { colors, which ->
        if (which == WallpaperManager.FLAG_SYSTEM) {
            wallpaperColors = colors
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Prepare Shell builder
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR or if (!isRootAvailable) Shell.FLAG_NON_ROOT_SHELL else 0)
                .setTimeout(10)
        )

        // Enable when proximity exists
        val sensorManager = getSystemService(SensorManager::class.java)
        if (sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, TeaTileService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // Prepare wallpaper colors
        WallpaperManager.getInstance(this).apply {
            addOnColorsChangedListener(wallpaperColorListener, Handler(Looper.getMainLooper()))
            GlobalScope.launch(Dispatchers.Default) {
                wallpaperColors = getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            }
        }
    }

    companion object {
        var wallpaperColors: WallpaperColors? by mutableStateOf(null)
            private set
    }
}
