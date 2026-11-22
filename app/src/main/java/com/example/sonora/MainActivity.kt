package com.example.sonora

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModelProvider
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.sonora.data.settings.AppTheme
import com.example.sonora.player.PlayerConnection
import com.example.sonora.player.RadioPlaybackService
import com.example.sonora.presentation.SonoraApp
import com.example.sonora.presentation.SonoraViewModel
import com.example.sonora.presentation.SonoraViewModelFactory
import com.example.sonora.presentation.theme.SonoraTheme
import com.example.sonora.presentation.theme.paletteFor
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

class MainActivity : ComponentActivity() {
    private lateinit var sonoraViewModel: SonoraViewModel

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val mainExecutor = Executor { command -> Handler(Looper.getMainLooper()).post(command) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4101)
        }
        val container = (application as SonoraApplication).container
        sonoraViewModel = ViewModelProvider(
            this,
            SonoraViewModelFactory(container.radioRepository, container.settingsRepository)
        )[SonoraViewModel::class.java]
        setContent {
            val uiState by sonoraViewModel.uiState.collectAsState()
            val palette = paletteFor(uiState.appTheme)
            SideEffect {
                window.statusBarColor = palette.background.toArgb()
                window.navigationBarColor = palette.background.toArgb()
                val systemBars = WindowInsetsControllerCompat(window, window.decorView)
                val useDarkIcons = uiState.appTheme == AppTheme.LIGHT
                systemBars.isAppearanceLightStatusBars = useDarkIcons
                systemBars.isAppearanceLightNavigationBars = useDarkIcons
            }
            SonoraTheme(themeMode = uiState.appTheme) {
                SonoraApp(viewModel = sonoraViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (controllerFuture != null) return
        val token = SessionToken(this, ComponentName(this, RadioPlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            if (controllerFuture === future) {
                runCatching { future.get() }
                    .onSuccess { PlayerConnection.controller = it }
            }
        }, mainExecutor)
    }

    override fun onStop() {
        super.onStop()
        // Unbind only when there is no playback to keep alive. Active or buffering
        // audio remains connected to its foreground MediaSessionService.
        if (PlayerConnection.controller?.playWhenReady != true) {
            releasePlayerController()
        }
    }

    private fun releasePlayerController() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        PlayerConnection.controller = null
    }

    override fun onDestroy() {
        releasePlayerController()
        super.onDestroy()
    }
}
