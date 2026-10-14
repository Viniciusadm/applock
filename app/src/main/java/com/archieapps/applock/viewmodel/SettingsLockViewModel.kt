package com.archieapps.applock.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.archieapps.applock.data.repository.ProtectionSettingsRepository
import com.archieapps.applock.security.PinAuthenticator
import com.archieapps.applock.security.PinManager
import com.archieapps.applock.security.SettingsAuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SettingsAuthMode { ViewGate, ConfirmEnable }

data class SettingsLockUiState(
    val loaded: Boolean = false,
    val protectionEnabled: Boolean = false,
    val pinSet: Boolean = false,
    val authenticated: Boolean = false,
    val pinError: Boolean = false,
    val verifying: Boolean = false,
    val mode: SettingsAuthMode = SettingsAuthMode.ViewGate,
) {
    val showLock: Boolean
        get() = loaded && (mode == SettingsAuthMode.ConfirmEnable || (protectionEnabled && !authenticated))

    val showContent: Boolean
        get() = loaded && !showLock
}

class SettingsLockViewModel(
    private val protectionSettingsRepository: ProtectionSettingsRepository,
    pinManager: PinManager,
) : ViewModel() {

    private val pinAuthenticator = PinAuthenticator(pinManager)

    private val _uiState = MutableStateFlow(
        SettingsLockUiState(authenticated = SettingsAuthSession.isAuthenticated()),
    )
    val uiState: StateFlow<SettingsLockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                protectionSettingsRepository.protectionEnabledFlow,
                pinManager.isPinSetFlow,
            ) { enabled, pinSet -> enabled to pinSet }
                .collect { (enabled, pinSet) ->
                    _uiState.update {
                        it.copy(protectionEnabled = enabled, pinSet = pinSet, loaded = true)
                    }
                }
        }
    }

    fun refreshGate() {
        _uiState.update { it.copy(authenticated = SettingsAuthSession.isAuthenticated()) }
    }

    fun submitPin(pin: String) {
        if (_uiState.value.verifying) return
        viewModelScope.launch {
            _uiState.update { it.copy(verifying = true, pinError = false) }
            if (pinAuthenticator.verify(pin)) {
                onAuthSuccess()
                _uiState.update { it.copy(verifying = false, pinError = false) }
            } else {
                _uiState.update { it.copy(verifying = false, pinError = true) }
            }
        }
    }

    fun onBiometricSuccess() {
        onAuthSuccess()
    }

    fun clearPinError() {
        _uiState.update { it.copy(pinError = false) }
    }

    fun requestEnableProtection() {
        _uiState.update { it.copy(mode = SettingsAuthMode.ConfirmEnable, pinError = false) }
    }

    fun cancelEnable() {
        _uiState.update { it.copy(mode = SettingsAuthMode.ViewGate, pinError = false) }
    }

    fun disableProtection() {
        viewModelScope.launch {
            protectionSettingsRepository.setProtectionEnabled(false)
        }
    }

    private fun onAuthSuccess() {
        SettingsAuthSession.markAuthenticated()
        val enabling = _uiState.value.mode == SettingsAuthMode.ConfirmEnable
        _uiState.update {
            it.copy(authenticated = true, mode = SettingsAuthMode.ViewGate, pinError = false)
        }
        if (enabling) {
            viewModelScope.launch { protectionSettingsRepository.setProtectionEnabled(true) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                SettingsLockViewModel(
                    protectionSettingsRepository = ProtectionSettingsRepository(application),
                    pinManager = PinManager(application),
                )
            }
        }
    }
}
