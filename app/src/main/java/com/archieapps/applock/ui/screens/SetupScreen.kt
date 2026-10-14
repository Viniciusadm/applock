package com.archieapps.applock.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archieapps.applock.R
import com.archieapps.applock.domain.LockerAction
import com.archieapps.applock.security.BiometricAuthenticator
import com.archieapps.applock.security.PinManager
import com.archieapps.applock.service.isAccessibilityServiceEnabled
import com.archieapps.applock.ui.components.LockButton
import com.archieapps.applock.ui.components.PinEntry
import com.archieapps.applock.ui.theme.Shapes
import com.archieapps.applock.ui.theme.Spacing
import com.archieapps.applock.util.DeviceCompatibilityHelper
import com.archieapps.applock.viewmodel.LockControlUiState
import com.archieapps.applock.viewmodel.LockControlViewModel
import com.archieapps.applock.viewmodel.SetupViewModel
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onOpenApps: () -> Unit,
    protectionEnabled: Boolean,
    pinSet: Boolean,
    onRequestEnableProtection: () -> Unit,
    onDisableProtection: () -> Unit,
    viewModel: SetupViewModel = viewModel(factory = SetupViewModel.Factory),
    lockControlViewModel: LockControlViewModel = viewModel(factory = LockControlViewModel.Factory),
) {
    val context = LocalContext.current
    val isPinSet by viewModel.isPinSet.collectAsStateWithLifecycle()

    val lockControlState by lockControlViewModel.uiState.collectAsStateWithLifecycle()
    val authRequest by lockControlViewModel.authRequests.collectAsStateWithLifecycle()
    var showAuthPinDialog by remember { mutableStateOf(false) }
    val activity = context as? FragmentActivity

    LaunchedEffect(authRequest) {
        if (authRequest == null) return@LaunchedEffect
        if (activity != null && BiometricAuthenticator.canAuthenticate(activity)) {
            BiometricAuthenticator.prompt(
                activity = activity,
                title = context.getString(R.string.automation_auth_title),
                subtitle = context.getString(R.string.automation_auth_subtitle),
                negativeButtonText = context.getString(R.string.lock_biometric_cancel),
                onSuccess = { lockControlViewModel.onAuthSucceeded() },
                onError = { _, _ -> showAuthPinDialog = true },
            )
        } else {
            showAuthPinDialog = true
        }
    }

    var refreshKey by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshKey++ }

    val accessibilityEnabled = remember(refreshKey) { isAccessibilityServiceEnabled(context) }
    val overlayGranted = remember(refreshKey) { Settings.canDrawOverlays(context) }
    val batteryUnrestricted = remember(refreshKey) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    var showPinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.setup_title)) },
                actions = {
                    TextButton(onClick = onOpenApps) {
                        Text(stringResource(R.string.setup_open_apps))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.setup_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LockControlCard(
                state = lockControlState,
                onToggle = { lockControlViewModel.onAction(LockerAction.Toggle) },
                onLock30 = {
                    lockControlViewModel.onAction(
                        LockerAction.LockTemporarily(LockerAction.LOCK_30_MINUTES),
                    )
                },
                onLock60 = {
                    lockControlViewModel.onAction(
                        LockerAction.LockTemporarily(LockerAction.LOCK_60_MINUTES),
                    )
                },
                onRestore = { lockControlViewModel.onAction(LockerAction.RestoreNormal) },
                onRequireAuthToggle = lockControlViewModel::setRequireAuth,
            )

            StepCard(
                title = stringResource(R.string.setup_accessibility_title),
                description = stringResource(R.string.setup_accessibility_desc),
                status = stringResource(
                    if (accessibilityEnabled) R.string.status_enabled else R.string.status_disabled,
                ),
                done = accessibilityEnabled,
                buttonText = stringResource(R.string.setup_accessibility_button),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
            )

            StepCard(
                title = stringResource(R.string.setup_overlay_title),
                description = stringResource(R.string.setup_overlay_desc),
                status = stringResource(
                    if (overlayGranted) R.string.status_granted else R.string.status_disabled,
                ),
                done = overlayGranted,
                buttonText = stringResource(R.string.setup_overlay_button),
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                },
            )

            StepCard(
                title = stringResource(R.string.setup_pin_title),
                description = stringResource(R.string.setup_pin_desc),
                status = stringResource(
                    if (isPinSet) R.string.status_pin_set else R.string.status_pin_unset,
                ),
                done = isPinSet,
                buttonText = stringResource(
                    if (isPinSet) R.string.setup_pin_button_change else R.string.setup_pin_button_set,
                ),
                onClick = { showPinDialog = true },
            )

            ProtectionCard(
                enabled = protectionEnabled,
                pinSet = pinSet,
                onToggle = { checked ->
                    if (checked) onRequestEnableProtection() else onDisableProtection()
                },
            )

            StepCard(
                title = stringResource(R.string.setup_battery_title),
                description = stringResource(R.string.setup_battery_desc),
                status = stringResource(
                    if (batteryUnrestricted) R.string.status_battery_ok else R.string.status_battery_on,
                ),
                done = batteryUnrestricted,
                buttonText = stringResource(R.string.setup_battery_button),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                },
            )

            ManufacturerCard(
                onOpenAppSettings = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                },
            )
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                viewModel.savePin(pin)
                showPinDialog = false
            },
        )
    }

    if (showAuthPinDialog) {
        AuthPinDialog(
            onVerify = { pin -> lockControlViewModel.verifyPin(pin) },
            onSuccess = {
                showAuthPinDialog = false
                lockControlViewModel.onAuthSucceeded()
            },
            onDismiss = {
                showAuthPinDialog = false
                lockControlViewModel.onAuthCancelled()
            },
        )
    }
}

@Composable
private fun LockControlCard(
    state: LockControlUiState,
    onToggle: (Boolean) -> Unit,
    onLock30: () -> Unit,
    onLock60: () -> Unit,
    onRestore: () -> Unit,
    onRequireAuthToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.lock_control_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(
                    checked = state.lockingEnabled,
                    onCheckedChange = onToggle,
                )
            }
            Text(
                text = stringResource(R.string.lock_control_master_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    if (state.effectiveEnabled) R.string.status_enabled else R.string.status_disabled,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (state.effectiveEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (state.tempLockActive) {
                val time = remember(state.tempLockUntil) {
                    android.text.format.DateFormat.getTimeFormat(context)
                        .format(Date(state.tempLockUntil))
                }
                Text(
                    text = stringResource(R.string.lock_control_temp_active, time),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                FilledTonalButton(
                    onClick = onLock30,
                    shape = Shapes.large,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.lock_control_lock_30))
                }
                FilledTonalButton(
                    onClick = onLock60,
                    shape = Shapes.large,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.lock_control_lock_60))
                }
            }
            OutlinedButton(
                onClick = onRestore,
                shape = Shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.lock_control_restore))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.lock_control_require_auth_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.lock_control_require_auth_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.requireAuth,
                    onCheckedChange = onRequireAuthToggle,
                    enabled = state.pinSet,
                )
            }
            if (!state.pinSet) {
                Text(
                    text = stringResource(R.string.lock_control_needs_pin),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AuthPinDialog(
    onVerify: suspend (String) -> Boolean,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    var pinError by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.extraLarge,
        title = { Text(stringResource(R.string.automation_auth_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun StatusBadge(done: Boolean) {
    val background = if (done) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val tint = if (done) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (done) Icons.Rounded.Check else Icons.Rounded.Warning,
            contentDescription = stringResource(
                if (done) R.string.cd_step_done else R.string.cd_step_pending,
            ),
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun StepCard(
    title: String,
    description: String,
    status: String,
    done: Boolean,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadge(done = done)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (done) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LockButton(
                text = buttonText,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun ProtectionCard(
    enabled: Boolean,
    pinSet: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.setup_protection_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    enabled = pinSet,
                )
            }
            Text(
                text = stringResource(R.string.setup_protection_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!pinSet) {
                Text(
                    text = stringResource(R.string.setup_protection_needs_pin),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(
                    text = stringResource(
                        if (enabled) R.string.status_enabled else R.string.status_disabled,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun ManufacturerCard(
    onOpenAppSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.compat_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(DeviceCompatibilityHelper.instructionsRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LockButton(
                text = stringResource(R.string.compat_app_settings_button),
                onClick = onOpenAppSettings,
            )
        }
    }
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val tooShort = pin.isNotEmpty() && pin.length < PinManager.PIN_MIN_LENGTH
    val mismatch = confirm.isNotEmpty() && pin != confirm
    val valid = pin.length in PinManager.PIN_MIN_LENGTH..PinManager.PIN_MAX_LENGTH && pin == confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.extraLarge,
        title = { Text(stringResource(R.string.pin_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(PinManager.PIN_MAX_LENGTH) },
                    label = { Text(stringResource(R.string.pin_enter)) },
                    singleLine = true,
                    isError = tooShort,
                    shape = Shapes.medium,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it.filter(Char::isDigit).take(PinManager.PIN_MAX_LENGTH) },
                    label = { Text(stringResource(R.string.pin_confirm)) },
                    singleLine = true,
                    isError = mismatch,
                    shape = Shapes.medium,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                when {
                    tooShort -> Text(
                        text = stringResource(R.string.pin_too_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )

                    mismatch -> Text(
                        text = stringResource(R.string.pin_mismatch),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = valid) {
                Text(stringResource(R.string.pin_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
