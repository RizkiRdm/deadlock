package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.DeadlockApp
import com.example.ui.creation.LockCreationViewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.detail.LockDetailScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.selector.AppSelectorScreen
import com.example.ui.settings.DeviceSettingsScreen
import kotlinx.coroutines.launch

@Composable
fun DeadlockNavGraph(
    navController: NavHostController = rememberNavController(),
    dashboardViewModel: DashboardViewModel = viewModel(),
    creationViewModel: LockCreationViewModel = viewModel()
) {
    val context = LocalContext.current
    val app = context.applicationContext as DeadlockApp
    val onboardingCompleted by app.onboardingPreferences.isOnboardingCompleted.collectAsState(initial = null)
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val startDestination = when (onboardingCompleted) {
        null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
            return
        }
        true -> Routes.DASHBOARD
        false -> Routes.ONBOARDING
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onCompleteOnboarding = {
                    scope.launch {
                        app.onboardingPreferences.setOnboardingCompleted(true)
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToCreateLock = {
                    creationViewModel.resetCreationState()
                    navController.navigate(Routes.APP_SELECTOR)
                },
                onNavigateToLockDetail = { policyId ->
                    navController.navigate(Routes.lockDetail(policyId))
                },
                onNavigateToDeviceSettings = {
                    navController.navigate(Routes.DEVICE_SETTINGS)
                }
            )
        }

        composable(Routes.APP_SELECTOR) {
            AppSelectorScreen(
                viewModel = creationViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLockActivated = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.DASHBOARD) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.LOCK_DETAIL,
            arguments = listOf(navArgument("policyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val policyId = backStackEntry.arguments?.getString("policyId")
            val policy = dashboardState.policies.find { it.id == policyId }

            LockDetailScreen(
                policy = policy,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DEVICE_SETTINGS) {
            DeviceSettingsScreen(
                isDeviceOwner = dashboardState.isDeviceOwner,
                isDeviceAdmin = dashboardState.isDeviceAdmin,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
