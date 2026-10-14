package com.archieapps.applock.automation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.archieapps.applock.R
import com.archieapps.applock.domain.LockController
import com.archieapps.applock.domain.LockerAction
import com.archieapps.applock.security.BiometricAuthenticator
import com.archieapps.applock.security.PinAuthenticator
import com.archieapps.applock.security.PinManager
import com.archieapps.applock.ui.components.PinEntry
import com.archieapps.applock.ui.theme.AppLockTheme
import com.archieapps.applock.ui.theme.Shapes
import com.archieapps.applock.ui.theme.Spacing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AutomationActivity : FragmentActivity() {

    private val controller by lazy { LockController(applicationContext) }
    private val pinManager by lazy { PinManager(applicationContext) }
    private val pinAuthenticator by lazy { PinAuthenticator(pinManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = resolveAction()
        if (action == null) {
            Toast.makeText(this, R.string.automation_invalid_action, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            if (controller.requiresAuth(action)) {
                beginAuth(action)
            } else {
                perform(action)
            }
        }
    }

    private fun resolveAction(): LockerAction? {
        intent?.data?.let { return LockerAction.fromUri(it) }
        val minutes = intent?.getIntExtra(LockerAction.EXTRA_MINUTES, LockerAction.LOCK_30_MINUTES)
            ?: LockerAction.LOCK_30_MINUTES
        return LockerAction.fromExplicit(intent?.action, minutes)
    }

    private suspend fun perform(action: LockerAction) {
        controller.dispatch(action)
        Toast.makeText(this, confirmationMessage(action), Toast.LENGTH_SHORT).show()
        finish()
    }

    private suspend fun confirmationMessage(action: LockerAction): String = when (action) {
        LockerAction.Enable -> getString(R.string.automation_toast_enabled)
        LockerAction.Disable -> getString(R.string.automation_toast_disabled)
        LockerAction.RestoreNormal -> getString(R.string.automation_toast_restored)
        is LockerAction.LockTemporarily ->
            getString(R.string.automation_toast_temp_locked, action.minutes)
        LockerAction.Toggle -> if (controller.lockingEnabledFlow.first()) {
            getString(R.string.automation_toast_enabled)
        } else {
            getString(R.string.automation_toast_disabled)
        }
    }

    private suspend fun beginAuth(action: LockerAction) {
        val pinSet = pinManager.isPinSet()
        if (BiometricAuthenticator.canAuthenticate(this)) {
            BiometricAuthenticator.prompt(
                activity = this,
                title = getString(R.string.automation_auth_title),
                subtitle = getString(R.string.automation_auth_subtitle),
                negativeButtonText = getString(
                    if (pinSet) R.string.lock_biometric_cancel else R.string.cancel,
                ),
                onSuccess = { lifecycleScope.launch { perform(action) } },
                onError = { _, _ -> if (pinSet) showPinPrompt(action) else finish() },
            )
        } else if (pinSet) {
            showPinPrompt(action)
        } else {
            Toast.makeText(this, R.string.automation_auth_unavailable, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun showPinPrompt(action: LockerAction) {
        setContent {
            AppLockTheme {
                AutomationPinPrompt(
                    onVerify = { pin -> pinAuthenticator.verify(pin) },
                    onSuccess = { lifecycleScope.launch { perform(action) } },
                    onCancel = { finish() },
                )
            }
        }
    }
}

@Composable
private fun AutomationPinPrompt(
    onVerify: suspend (String) -> Boolean,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
) {
    var pinError by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = Shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(Spacing.xl),
        ) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.automation_auth_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.automation_auth_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PinEntry(
                    pinError = pinError,
                    verifying = verifying,
                    onPinChange = { pinError = false },
                    onPinSubmit = { pin ->
                        if (verifying) return@PinEntry
                        scope.launch {
                            verifying = true
                            pinError = false
                            if (onVerify(pin)) {
                                onSuccess()
                            } else {
                                pinError = true
                                verifying = false
                            }
                        }
                    },
                )
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
