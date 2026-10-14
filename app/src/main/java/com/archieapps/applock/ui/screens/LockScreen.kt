package com.archieapps.applock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.archieapps.applock.R
import com.archieapps.applock.ui.components.BrandHeader
import com.archieapps.applock.ui.components.LockButton
import com.archieapps.applock.ui.components.PinEntry
import com.archieapps.applock.ui.theme.Spacing
import com.archieapps.applock.viewmodel.LockUiState

@Composable
fun LockScreen(
    uiState: LockUiState,
    biometricAvailable: Boolean,
    onPinSubmit: (String) -> Unit,
    onPinChange: () -> Unit,
    onBiometricClick: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandHeader(
                icon = rememberVectorPainter(Icons.Rounded.Lock),
                title = uiState.appName,
                subtitle = stringResource(R.string.lock_protected_message),
                iconContentDescription = stringResource(R.string.cd_lock),
            )

            if (biometricAvailable) {
                LockButton(
                    text = stringResource(R.string.lock_biometric_button),
                    onClick = onBiometricClick,
                    icon = painterResource(R.drawable.ic_fingerprint_24),
                    modifier = Modifier.padding(top = Spacing.xl),
                )
            }

            if (uiState.isPinSet) {
                PinEntry(
                    pinError = uiState.pinError,
                    verifying = uiState.verifying,
                    onPinChange = onPinChange,
                    onPinSubmit = onPinSubmit,
                )
            } else {
                Text(
                    text = stringResource(R.string.lock_pin_unset),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.lg),
                )
            }
        }
    }
}
