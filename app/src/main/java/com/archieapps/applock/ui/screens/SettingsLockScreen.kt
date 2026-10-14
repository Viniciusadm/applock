package com.archieapps.applock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.archieapps.applock.R
import com.archieapps.applock.ui.components.BrandHeader
import com.archieapps.applock.ui.components.LockButton
import com.archieapps.applock.ui.components.PinEntry
import com.archieapps.applock.ui.theme.Spacing
import com.archieapps.applock.viewmodel.SettingsAuthMode
import com.archieapps.applock.viewmodel.SettingsLockUiState

@Composable
fun SettingsLockScreen(
    uiState: SettingsLockUiState,
    biometricAvailable: Boolean,
    onPinSubmit: (String) -> Unit,
    onPinChange: () -> Unit,
    onBiometricClick: () -> Unit,
) {
    val confirmingEnable = uiState.mode == SettingsAuthMode.ConfirmEnable
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandHeader(
                icon = painterResource(R.drawable.ic_shield_24),
                title = stringResource(R.string.app_locker_title),
                subtitle = stringResource(
                    if (confirmingEnable) {
                        R.string.settings_lock_enable_subtitle
                    } else {
                        R.string.settings_lock_subtitle
                    },
                ),
                iconContentDescription = stringResource(R.string.cd_shield),
            )

            if (biometricAvailable) {
                LockButton(
                    text = stringResource(R.string.lock_biometric_button),
                    onClick = onBiometricClick,
                    icon = painterResource(R.drawable.ic_fingerprint_24),
                    modifier = Modifier.padding(top = Spacing.xl),
                )
            }

            if (uiState.pinSet) {
                PinEntry(
                    pinError = uiState.pinError,
                    verifying = uiState.verifying,
                    onPinChange = onPinChange,
                    onPinSubmit = onPinSubmit,
                )
            }
        }
    }
}
