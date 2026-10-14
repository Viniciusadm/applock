package com.archieapps.applock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archieapps.applock.R
import com.archieapps.applock.data.model.InstalledApp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.archieapps.applock.ui.components.AppCard
import com.archieapps.applock.ui.components.AppSearchBar
import com.archieapps.applock.ui.components.EmptyState
import com.archieapps.applock.ui.theme.Shapes
import com.archieapps.applock.ui.theme.Spacing
import com.archieapps.applock.viewmodel.AppListUiState
import com.archieapps.applock.viewmodel.AppListViewModel
import kotlinx.coroutines.launch

@Composable
fun AppListScreen(
    onOpenSetup: () -> Unit,
    viewModel: AppListViewModel = viewModel(factory = AppListViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AppListScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onToggleBlock = viewModel::onToggleBlock,
        onOpenSetup = onOpenSetup,
    )

    if (uiState.pendingCriticalPackage != null) {
        CriticalBlockDialog(
            appName = uiState.pendingCriticalAppName,
            onConfirm = viewModel::confirmCriticalBlock,
            onDismiss = viewModel::dismissCriticalBlock,
        )
    }
}

@Composable
private fun CriticalBlockDialog(
    appName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.extraLarge,
        title = { Text(stringResource(R.string.critical_block_title)) },
        text = { Text(stringResource(R.string.critical_block_message, appName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.critical_block_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListScreen(
    uiState: AppListUiState,
    onQueryChange: (String) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit,
    onOpenSetup: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val blockedTemplate = stringResource(R.string.snackbar_app_blocked)
    val unblockedTemplate = stringResource(R.string.snackbar_app_unblocked)

    val onToggleWithFeedback: (String, Boolean) -> Unit = { packageName, checked ->
        onToggleBlock(packageName, checked)
        val appName = uiState.apps.firstOrNull { it.packageName == packageName }?.name ?: packageName
        val template = if (checked) blockedTemplate else unblockedTemplate
        val message = String.format(template, appName)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_list_title)) },
                actions = {
                    IconButton(onClick = onOpenSetup) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            AppSearchBar(
                query = uiState.query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.sm,
                ),
            )

            when {
                uiState.isLoading -> LoadingState()
                uiState.apps.isEmpty() -> NoResultsState()
                else -> AppList(
                    apps = uiState.apps,
                    isSearching = uiState.query.isNotEmpty(),
                    onToggleBlock = onToggleWithFeedback,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NoResultsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = rememberVectorPainter(Icons.Rounded.Search),
            title = stringResource(R.string.empty_no_results_title),
            message = stringResource(R.string.empty_no_results_message),
        )
    }
}

@Composable
private fun AppList(
    apps: List<InstalledApp>,
    isSearching: Boolean,
    onToggleBlock: (String, Boolean) -> Unit,
) {
    val noneBlocked = remember(apps) { apps.none { it.isBlocked } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.xs,
            bottom = Spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (noneBlocked && !isSearching) {
            item(key = "none-locked-hint") {
                EmptyState(
                    icon = painterResource(R.drawable.ic_shield_24),
                    title = stringResource(R.string.empty_none_locked_title),
                    message = stringResource(R.string.empty_none_locked_message),
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
            }
        }
        items(items = apps, key = { it.packageName }) { app ->
            AppCard(
                app = app,
                onToggleBlock = onToggleBlock,
                modifier = Modifier.animateItem(),
            )
        }
    }
}
