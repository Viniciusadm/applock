package com.archieapps.applock

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.archieapps.applock.security.BiometricAuthenticator
import com.archieapps.applock.service.AppLockSession
import com.archieapps.applock.ui.screens.LockScreen
import com.archieapps.applock.ui.theme.AppLockTheme
import com.archieapps.applock.viewmodel.LockViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LockActivity : FragmentActivity() {

    private lateinit var lockedPackage: String

    private val viewModel: LockViewModel by viewModels { LockViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockedPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        if (lockedPackage.isEmpty()) {
            finish()
            return
        }

        AppLockSession.onLockShowing(lockedPackage)
        viewModel.bind(lockedPackage)
        onBackPressedDispatcher.addCallback(this) { goHome() }

        setContent {
            AppLockTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val biometricAvailable = remember { canAuthenticate() }
                LockScreen(
                    uiState = uiState,
                    biometricAvailable = biometricAvailable,
                    onPinSubmit = viewModel::submitPin,
                    onPinChange = viewModel::clearPinError,
                    onBiometricClick = ::promptBiometric,
                )
            }
        }

        observeUnlock()
        if (savedInstanceState == null && canAuthenticate()) {
            promptBiometric()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        if (newPackage.isEmpty()) return
        lockedPackage = newPackage
        AppLockSession.onLockShowing(newPackage)
        viewModel.bind(newPackage)
        if (canAuthenticate()) promptBiometric()
    }

    private fun observeUnlock() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .map { it.unlocked }
                    .distinctUntilChanged()
                    .collect { unlocked -> if (unlocked) unlock() }
            }
        }
    }

    private fun canAuthenticate(): Boolean = BiometricAuthenticator.canAuthenticate(this)

    private fun promptBiometric() {
        BiometricAuthenticator.prompt(
            activity = this,
            title = getString(R.string.lock_biometric_title),
            subtitle = getString(R.string.lock_biometric_subtitle),
            negativeButtonText = getString(R.string.lock_biometric_cancel),
            onSuccess = { viewModel.onBiometricSuccess() },
        )
    }

    private fun unlock() {
        AppLockSession.unlock(lockedPackage)
        finish()
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(home)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::lockedPackage.isInitialized) {
            AppLockSession.onLockDismissed(lockedPackage)
        }
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }
}
