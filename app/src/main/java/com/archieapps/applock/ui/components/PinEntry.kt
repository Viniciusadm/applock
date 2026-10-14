package com.archieapps.applock.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.archieapps.applock.R
import com.archieapps.applock.security.PinManager
import com.archieapps.applock.ui.theme.Shapes
import com.archieapps.applock.ui.theme.Spacing

@Composable
fun PinEntry(
    pinError: Boolean,
    verifying: Boolean,
    onPinChange: () -> Unit,
    onPinSubmit: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    OutlinedTextField(
        value = pin,
        onValueChange = {
            pin = it.filter(Char::isDigit).take(PinManager.PIN_MAX_LENGTH)
            onPinChange()
        },
        label = { Text(stringResource(R.string.pin_enter)) },
        singleLine = true,
        isError = pinError,
        enabled = !verifying,
        shape = Shapes.medium,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.lg),
    )
    if (pinError) {
        Text(
            text = stringResource(R.string.lock_pin_error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
    LockButton(
        text = stringResource(R.string.lock_pin_submit),
        onClick = { onPinSubmit(pin) },
        enabled = !verifying && pin.length in PinManager.PIN_MIN_LENGTH..PinManager.PIN_MAX_LENGTH,
        modifier = Modifier.padding(top = Spacing.md),
    )
}
