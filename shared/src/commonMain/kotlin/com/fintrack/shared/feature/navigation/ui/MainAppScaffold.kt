package com.fintrack.shared.feature.navigation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fintrack.shared.feature.core.ui.LocalSharedTransitionScope
import com.fintrack.shared.feature.core.ui.permission.PermissionRationaleDialog
import com.fintrack.shared.feature.core.ui.permission.SmsPermissionLauncher
import com.fintrack.shared.feature.core.ui.util.navigateThrottled
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.navigation.model.AppBarState
import com.fintrack.shared.feature.navigation.model.Screen
import com.fintrack.shared.feature.navigation.ui.components.AddTransactionFAB
import com.fintrack.shared.feature.navigation.ui.components.AppTopBar
import com.fintrack.shared.feature.transaction.ui.TransactionViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainAppScaffold(
    mainViewModel: MainViewModel,
    transactionsViewModel: TransactionViewModel = koinViewModel(),
    onLogout: () -> Unit = {},
    onUpdateToastPadding: (Dp) -> Unit = {}
) {
    val navBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navController = LocalNavController.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val visibleEntries by navController.visibleEntries.collectAsStateWithLifecycle()
    val currentDestination = navBackStackEntry?.destination
    val importStateMap by transactionsViewModel.importState.collectAsStateWithLifecycle()
    val smsSyncSignal by mainViewModel.smsSyncTrigger.collectAsStateWithLifecycle()

    val currentAccountId = smsSyncSignal?.accountId
    val currentImportState = importStateMap[currentAccountId]
    val isSmsRationaleHidden by mainViewModel.isSmsRationaleHidden.collectAsStateWithLifecycle()

    var showSmsPermissionRequest by remember { mutableStateOf(false) }
    var showSmsRationale by remember { mutableStateOf(false) }

    LaunchedEffect(currentImportState) {
        if (currentImportState is Result.Success) {
            transactionsViewModel.resetImportState(currentAccountId)
        } else if (currentImportState is Result.Error) {
            val exception = currentImportState.exception
            val message = exception.message ?: "Sync failed"
            
            // If it's a permission error, we let HomeScreen trigger the rationale callback
            if (!message.contains("permission", ignoreCase = true)) {
                mainViewModel.showToast(message, isError = true)
            }
        }
    }

    val appBarState = remember(navBackStackEntry) {
        currentDestination?.getAppBarState(navBackStackEntry, navController) ?: AppBarState(title = "Home")
    }

    // Show bars only if any of the visible entries requires them
    val showTopBar = remember(visibleEntries) {
        visibleEntries.any { entry -> !entry.destination.isAuthScreen() }
    }

    // Logic for animating the bars away immediately upon navigation
    val showBottomBarNow = remember(currentDestination) {
        currentDestination?.shouldShowBottomBar() ?: true
    }

    LaunchedEffect(showBottomBarNow, navBarsPadding) {
        onUpdateToastPadding(if (showBottomBarNow) 80.dp + navBarsPadding + 16.dp else 24.dp)
    }

    // Keep the composable in the hierarchy during transitions to stabilize content area
    val keepBottomBarInHierarchy = remember(visibleEntries) {
        visibleEntries.any { entry -> entry.destination.shouldShowBottomBar() }
    }

    val showFABNow = remember(currentDestination) {
        currentDestination?.shouldShowFAB() ?: true
    }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val suiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.secondary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val railItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.secondary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (showTopBar) {
                    AppTopBar(
                        appBarState = appBarState
                    )
                }
            },
            bottomBar = {
                if (suiteType == NavigationSuiteType.NavigationBar && keepBottomBarInHierarchy) {
                    val bottomBarHeight = 80.dp + navBarsPadding
                    Box(
                        modifier = Modifier.height(bottomBarHeight)
                    ) {
                        AnimatedVisibility(
                            visible = showBottomBarNow,
                            enter = slideInVertically(
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) { it },
                            exit = slideOutVertically(
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) { it }
                        ) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = NavigationBarDefaults.Elevation
                            ) {
                                NavigationBarItem(
                                    selected = currentDestination?.hierarchy?.any { it.hasRoute<Screen.Home>() } == true,
                                    onClick = {
                                        navController.navigateThrottled(Screen.Home()) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    colors = itemColors
                                )
                                NavigationBarItem(
                                    selected = currentDestination?.hierarchy?.any { it.hasRoute<Screen.Statistics>() } == true,
                                    onClick = {
                                        navController.navigateThrottled(Screen.Statistics()) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                                    label = { Text("Stats") },
                                    colors = itemColors
                                )

                                // Spacing for the FAB
                                NavigationBarItem(
                                    selected = false,
                                    icon = { Box(Modifier.size(1.dp)) },
                                    label = { Text("") },
                                    enabled = false,
                                    onClick = {},
                                    colors = itemColors
                                )

                                NavigationBarItem(
                                    selected = currentDestination?.hierarchy?.any { it.hasRoute<Screen.Budget>() } == true,
                                    onClick = {
                                        navController.navigateThrottled(Screen.Budget) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Info, contentDescription = "Budget") },
                                    label = { Text("Budget") },
                                    colors = itemColors
                                )
                                NavigationBarItem(
                                    selected = currentDestination?.hierarchy?.any { it.hasRoute<Screen.Profile>() } == true,
                                    onClick = {
                                        navController.navigateThrottled(Screen.Profile) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                    label = { Text("Profile") },
                                    colors = itemColors
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Row(modifier = Modifier.fillMaxSize()) {
                if (suiteType == NavigationSuiteType.NavigationRail) {
                    AnimatedVisibility(
                        visible = showBottomBarNow,
                        enter = slideInHorizontally { -it },
                        exit = slideOutHorizontally { -it }
                    ) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            NavigationRailItem(
                                selected = currentDestination?.hierarchy?.any { it.hasRoute<Screen.Home>() } == true,
                                onClick = {
                                    navController.navigateThrottled(Screen.Home()) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") },
                                colors = railItemColors
                            )
                            NavigationRailItem(
                                selected = currentDestination?.hierarchy?.any { it.hasRoute<Screen.Statistics>() } == true,
                                onClick = {
                                    navController.navigateThrottled(Screen.Statistics()) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                                label = { Text("Stats") },
                                colors = railItemColors
                            )

                            NavigationRailItem(
                                selected = currentDestination?.hierarchy?.any { it.hasRoute<Screen.Budget>() } == true,
                                onClick = {
                                    navController.navigateThrottled(Screen.Budget) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.Info, contentDescription = "Budget") },
                                label = { Text("Budget") },
                                colors = railItemColors
                            )
                            NavigationRailItem(
                                selected = currentDestination?.hierarchy?.any { it.hasRoute<Screen.Profile>() } == true,
                                onClick = {
                                    navController.navigateThrottled(Screen.Profile) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile") },
                                colors = railItemColors
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                            MainNavigation(
                                paddingValues = paddingValues,
                                smsSyncSignal = smsSyncSignal,
                                mainViewModel = mainViewModel,
                                onLogout = onLogout,
                                onSmsPermissionRequired = { force ->
                                    if (force) {
                                        // Manual sync: always request permission
                                        if (isSmsRationaleHidden) {
                                            showSmsPermissionRequest = true
                                        } else {
                                            showSmsRationale = true
                                        }
                                    } else {
                                        // Auto-sync: only show if the user hasn't opted out of the explanation
                                        // If rationale is hidden, we skip the request to avoid nagging on app open
                                        if (!isSmsRationaleHidden) {
                                            showSmsRationale = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Place FAB over everything
        AnimatedVisibility(
            visible = showFABNow,
            enter = slideInVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) { it * 2 } + fadeIn(),
            exit = slideOutVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) { it * 2 } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(100f)
        ) {
            AddTransactionFAB(
                onClick = { navController.navigateThrottled(Screen.AddTransaction()) },
                modifier = Modifier.padding(bottom = 40.dp + navBarsPadding)
            )
        }
    }

    if (showSmsRationale) {
        PermissionRationaleDialog(
            title = "Automatic Transaction Sync",
            message = "FinTrack can automatically keep your transactions up to date by scanning SMS from M-Pesa and your bank. This keeps your dashboard accurate with zero manual effort.",
            icon = Icons.Default.Sms,
            onConfirm = { dontShowAgain ->
                if (dontShowAgain) {
                    mainViewModel.setSmsRationaleHidden(true)
                }
                showSmsRationale = false
                showSmsPermissionRequest = true
            },
            onDismiss = { dontShowAgain ->
                if (dontShowAgain) {
                    mainViewModel.setSmsRationaleHidden(true)
                }
                showSmsRationale = false
            }
        )
    }

    SmsPermissionLauncher(
        trigger = showSmsPermissionRequest,
        onResult = { granted ->
            if (granted) {
                mainViewModel.triggerSmsSync()
            } else {
                mainViewModel.showToast("Permission denied. Enable SMS permissions in Phone Settings to use automatic sync.", true)
            }
            showSmsPermissionRequest = false
        },
        onDismissTrigger = { 
            showSmsPermissionRequest = false 
        }
    )
}
