package com.archieapps.applock

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.applock.security.BiometricAuthenticator
import com.archieapps.applock.security.SettingsAuthSession
import com.archieapps.applock.ui.screens.AppListScreen
import com.archieapps.applock.ui.screens.SettingsLockScreen
import com.archieapps.applock.ui.screens.SetupScreen
import com.archieapps.applock.ui.theme.AppLockTheme
import com.archieapps.applock.viewmodel.SettingsAuthMode
import com.archieapps.applock.viewmodel.SettingsLockViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: SettingsLockViewModel by viewModels { SettingsLockViewModel.Factory }

    private var biometricInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this) {
            val state = viewModel.uiState.value
            when {
                state.mode == SettingsAuthMode.ConfirmEnable -> viewModel.cancelEnable()
                state.showLock -> moveTaskToBack(true)
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        setContent {
            AppLockTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val biometricAvailable = remember { BiometricAuthenticator.canAuthenticate(this) }
                var resumeTick by remember { mutableIntStateOf(0) }

                LifecycleEventEffect(Lifecycle.Event.ON_START) {
                    viewModel.refreshGate()
                    resumeTick++
                }
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    SettingsAuthSession.onEnterBackground()
                }
                LaunchedEffect(state.showLock, resumeTick) {
                    if (state.showLock && biometricAvailable) promptSettingsBiometric()
                }

                when {
                    state.showLock -> SettingsLockScreen(
                        uiState = state,
                        biometricAvailable = biometricAvailable,
                        onPinSubmit = viewModel::submitPin,
                        onPinChange = viewModel::clearPinError,
                        onBiometricClick = ::promptSettingsBiometric,
                    )

                    state.showContent -> {
                        var showSetup by rememberSaveable { mutableStateOf(false) }
                        if (showSetup) {
                            SetupScreen(
                                onOpenApps = { showSetup = false },
                                protectionEnabled = state.protectionEnabled,
                                pinSet = state.pinSet,
                                onRequestEnableProtection = viewModel::requestEnableProtection,
                                onDisableProtection = viewModel::disableProtection,
                            )
                        } else {
                            AppListScreen(onOpenSetup = { showSetup = true })
                        }
                    }
                }
            }
        }
    }

    private fun promptSettingsBiometric() {
        if (biometricInFlight) return
        if (!BiometricAuthenticator.canAuthenticate(this)) return
        biometricInFlight = true
        BiometricAuthenticator.prompt(
            activity = this,
            title = getString(R.string.settings_lock_biometric_title),
            subtitle = getString(R.string.settings_lock_biometric_subtitle),
            negativeButtonText = getString(R.string.lock_biometric_cancel),
            onSuccess = {
                biometricInFlight = false
                viewModel.onBiometricSuccess()
            },
            onError = { _, _ -> biometricInFlight = false },
        )
    }
}
