package com.nowlsantos.aiconcierge

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowlsantos.aiconcierge.presentation.ConciergeUiEvent
import com.nowlsantos.aiconcierge.presentation.ConciergeViewModel
import com.nowlsantos.aiconcierge.ui.component.widget.ShowExitDialog
import com.nowlsantos.aiconcierge.ui.screen.ConciergeScreen
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaTheme
import com.nowlsantos.aiconcierge.utils.Logger
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ConciergeViewModel by viewModel()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Permission denied logic if necessary
            println("Permission denied logic if necessary")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Android 12+ Splash Screen API and dismiss instantly without zoom-out distortion!
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
        }

        enableEdgeToEdge()
        Logger.i("MainActivity created. Setting up EdgeToEdge & Immersive Mode.")

        // Set up immersive mode
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        super.onCreate(savedInstanceState)

        checkAudioPermission()
        setupStationaryMode()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var showExitDialog by remember { mutableStateOf(false) }

            BackHandler {
                showExitDialog = true
            }

            PeninsulaTheme(darkTheme = uiState.isDarkMode) {
                ConciergeScreen(viewModel = viewModel)

                if (showExitDialog) {
                    ShowExitDialog(
                        onDismiss = { showExitDialog = false }
                    )
                }
            }
        }
    }

    private var lastKeyActionTimeMs = 0L

    private val powerConnectionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            updateStationaryWindowFlag()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(powerConnectionReceiver, filter)
        updateStationaryWindowFlag()
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(powerConnectionReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver already unregistered
        }
    }

    /**
     * STATIONARY MODE: Keeps the screen on if the device is plugged in.
     * This turns Aris into a permanent hotel fixture when docked.
     */
    private fun setupStationaryMode() {
        updateStationaryWindowFlag()
    }

    private fun updateStationaryWindowFlag() {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = registerReceiver(null, intentFilter)

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        if (isCharging) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * SUMMON BUTTON: Hardware key listener for Bluetooth buttons.
     * Most smart buttons send a MEDIA_PLAY_PAUSE signal.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                if (event?.repeatCount == 0) {
                    val now = System.currentTimeMillis()
                    if (now - lastKeyActionTimeMs >= 500L) {
                        lastKeyActionTimeMs = now
                        viewModel.onEvent(ConciergeUiEvent.OnMicClicked)
                    }
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
