package dev.thermaltrace.android.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.thermaltrace.android.AppContainer
import dev.thermaltrace.android.DeepLinks
import dev.thermaltrace.android.ui.account.AccountScreen
import dev.thermaltrace.android.ui.account.AccountViewModel
import dev.thermaltrace.android.ui.alerts.AlertsScreen
import dev.thermaltrace.android.ui.alerts.AlertsViewModel
import dev.thermaltrace.android.ui.devices.DevicesScreen
import dev.thermaltrace.android.ui.devices.DevicesViewModel
import dev.thermaltrace.android.ui.history.HistoryScreen
import dev.thermaltrace.android.ui.history.HistoryViewModel
import dev.thermaltrace.android.ui.home.HomeScreen
import dev.thermaltrace.android.ui.home.HomeViewModel
import dev.thermaltrace.android.ui.household.HouseholdScreen
import dev.thermaltrace.android.ui.household.HouseholdViewModel
import dev.thermaltrace.android.ui.login.LoginScreen
import dev.thermaltrace.android.ui.login.LoginViewModel

object Routes {
    const val Login = "login"
    const val Home = "home"
    const val History = "history"
    const val Devices = "devices"
    const val Alerts = "alerts"
    const val Household = "household"
    const val Account = "account"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val mainTabs = listOf(
    Tab(Routes.Home, "Home", Icons.Default.Home),
    Tab(Routes.History, "History", Icons.AutoMirrored.Filled.ShowChart),
    Tab(Routes.Alerts, "Alerts", Icons.Default.Notifications),
    Tab(Routes.Devices, "Devices", Icons.Default.Devices),
    Tab(Routes.Account, "Account", Icons.Default.Person),
)

@Composable
fun ThermalTraceNav(
    container: AppContainer,
    deepLinkDestination: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        container.authRepository.restoreSession()
        val signedIn = container.sessionStore.current() != null
        startDestination = if (signedIn) Routes.Home else Routes.Login
    }

    LaunchedEffect(deepLinkDestination, startDestination) {
        val dest = deepLinkDestination ?: return@LaunchedEffect
        if (startDestination == null || startDestination == Routes.Login) return@LaunchedEffect
        val route = when (dest) {
            DeepLinks.ALERTS -> Routes.Alerts
            DeepLinks.HISTORY -> Routes.History
            DeepLinks.HOME -> Routes.Home
            else -> Routes.Alerts
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        onDeepLinkConsumed()
    }

    val start = startDestination
    if (start == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in mainTabs.map { it.route } || currentRoute == Routes.Household

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    mainTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.Login) {
                val vm: LoginViewModel = viewModel(
                    factory = LoginViewModel.factory(container.authRepository),
                )
                LoginScreen(
                    viewModel = vm,
                    onSignedIn = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.Home) {
                val vm: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(container.readingsRepository),
                )
                HomeScreen(viewModel = vm)
            }
            composable(Routes.History) {
                val vm: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.factory(container.historyRepository),
                )
                HistoryScreen(viewModel = vm)
            }
            composable(Routes.Devices) {
                val vm: DevicesViewModel = viewModel(
                    factory = DevicesViewModel.factory(container.devicesRepository),
                )
                DevicesScreen(viewModel = vm)
            }
            composable(Routes.Alerts) {
                val vm: AlertsViewModel = viewModel(
                    factory = AlertsViewModel.factory(
                        container.settingsRepository,
                        container.alertsInboxRepository,
                    ),
                )
                AlertsScreen(viewModel = vm)
            }
            composable(Routes.Household) {
                val vm: HouseholdViewModel = viewModel(
                    factory = HouseholdViewModel.factory(container.householdRepository),
                )
                HouseholdScreen(viewModel = vm)
            }
            composable(Routes.Account) {
                val vm: AccountViewModel = viewModel(
                    factory = AccountViewModel.factory(
                        container.settingsRepository,
                        container.authRepository,
                        container.pushRegistrar,
                    ),
                )
                AccountScreen(
                    viewModel = vm,
                    onOpenHousehold = { navController.navigate(Routes.Household) },
                    onSignedOut = {
                        navController.navigate(Routes.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
