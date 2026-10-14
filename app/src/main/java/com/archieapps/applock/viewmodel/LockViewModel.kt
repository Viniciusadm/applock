package com.archieapps.applock.viewmodel

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.archieapps.applock.data.repository.InstalledAppsRepository
import com.archieapps.applock.security.PinAuthenticator
import com.archieapps.applock.security.PinManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockUiState(
    val packageName: String = "",
    val appName: String = "",
    val icon: Drawable? = null,
    val isPinSet: Boolean = false,
    val pinError: Boolean = false,
    val verifying: Boolean = false,
    val unlocked: Boolean = false,
)

class LockViewModel(
    private val installedAppsRepository: InstalledAppsRepository,
    private val pinManager: PinManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private val pinAuthenticator = PinAuthenticator(pinManager)

    fun bind(packageName: String) {
        if (_uiState.value.packageName == packageName && _uiState.value.appName.isNotEmpty()) return
        _uiState.value = LockUiState(packageName = packageName, appName = packageName)
        viewModelScope.launch {
            val app = installedAppsRepository.getApp(packageName)
            val pinSet = pinManager.isPinSet()
            _uiState.update {
                it.copy(appName = app?.name ?: packageName, icon = app?.icon, isPinSet = pinSet)
            }
        }
    }

    fun submitPin(pin: String) {
        if (_uiState.value.verifying) return
        viewModelScope.launch {
            _uiState.update { it.copy(verifying = true, pinError = false) }
            if (pinAuthenticator.verify(pin)) {
                _uiState.update { it.copy(pinError = false, verifying = false, unlocked = true) }
            } else {
                _uiState.update { it.copy(pinError = true, verifying = false) }
            }
        }
    }

    fun onBiometricSuccess() {
        _uiState.update { it.copy(unlocked = true) }
    }

    fun clearPinError() {
        _uiState.update { it.copy(pinError = false) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                LockViewModel(
                    installedAppsRepository = InstalledAppsRepository(application),
                    pinManager = PinManager(application),
                )
            }
        }
    }
}
