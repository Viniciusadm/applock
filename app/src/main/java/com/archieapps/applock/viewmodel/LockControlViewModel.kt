package com.archieapps.applock.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.archieapps.applock.data.repository.isProtectionActive
import com.archieapps.applock.domain.ActionAuthPolicy
import com.archieapps.applock.domain.LockController
import com.archieapps.applock.domain.LockerAction
import com.archieapps.applock.security.PinAuthenticator
import com.archieapps.applock.security.PinManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class LockControlUiState(
    val lockingEnabled: Boolean = true,
    val tempLockUntil: Long = 0L,
    val requireAuth: Boolean = false,
    val pinSet: Boolean = false,
    val now: Long = 0L,
) {
    val tempLockActive: Boolean get() = tempLockUntil > now
    val effectiveEnabled: Boolean get() = isProtectionActive(lockingEnabled, tempLockUntil, now)
}

class LockControlViewModel(
    private val controller: LockController,
    pinManager: PinManager,
) : ViewModel() {

    private val pinAuthenticator = PinAuthenticator(pinManager)

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    val uiState: StateFlow<LockControlUiState> = combine(
        controller.lockingEnabledFlow,
        controller.tempLockUntilFlow,
        controller.requireAuthForActionsFlow,
        pinManager.isPinSetFlow,
        ticker,
    ) { enabled, until, requireAuth, pinSet, now ->
        LockControlUiState(
            lockingEnabled = enabled,
            tempLockUntil = until,
            requireAuth = requireAuth,
            pinSet = pinSet,
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LockControlUiState())

    private val _authRequests = MutableStateFlow<LockerAction?>(null)
    val authRequests: StateFlow<LockerAction?> = _authRequests

    fun onAction(action: LockerAction) {
        val state = uiState.value
        if (ActionAuthPolicy.requiresAuth(action, state.requireAuth, state.lockingEnabled)) {
            _authRequests.value = action
        } else {
            dispatch(action)
        }
    }

    fun onAuthSucceeded() {
        val action = _authRequests.value ?: return
        _authRequests.value = null
        dispatch(action)
    }

    fun onAuthCancelled() {
        _authRequests.value = null
    }

    suspend fun verifyPin(pin: String): Boolean = pinAuthenticator.verify(pin)

    fun setRequireAuth(enabled: Boolean) {
        viewModelScope.launch { controller.setRequireAuthForActions(enabled) }
    }

    private fun dispatch(action: LockerAction) {
        viewModelScope.launch { controller.dispatch(action) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                LockControlViewModel(
                    controller = LockController(application),
                    pinManager = PinManager(application),
                )
            }
        }
    }
}
