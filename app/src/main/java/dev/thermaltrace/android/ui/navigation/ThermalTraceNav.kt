package dev.thermaltrace.android.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.thermaltrace.android.AppContainer
import dev.thermaltrace.android.ui.home.HomeScreen
import dev.thermaltrace.android.ui.home.HomeViewModel
import dev.thermaltrace.android.ui.login.LoginScreen
import dev.thermaltrace.android.ui.login.LoginViewModel

object Routes {
    const val Login = "login"
    const val Home = "home"
}

@Composable
fun ThermalTraceNav(container: AppContainer) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        container.authRepository.restoreSession()
        val signedIn = container.sessionStore.current() != null
        startDestination = if (signedIn) Routes.Home else Routes.Login
    }

    val start = startDestination
    if (start == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = start) {
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
                factory = HomeViewModel.factory(
                    container.readingsRepository,
                    container.authRepository,
                ),
            )
            HomeScreen(
                viewModel = vm,
                onSignedOut = {
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Home) { inclusive = true }
                    }
                },
            )
        }
    }
}
