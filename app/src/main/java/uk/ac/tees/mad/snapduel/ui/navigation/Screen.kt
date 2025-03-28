package uk.ac.tees.mad.snapduel.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import uk.ac.tees.mad.snapduel.ui.screens.AuthScreen
import uk.ac.tees.mad.snapduel.ui.screens.ChallengeScreen
import uk.ac.tees.mad.snapduel.ui.screens.SplashScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Auth : Screen("auth_screen")
    object Challenge : Screen("challenge_screen")
    object Voting : Screen("voting_screen")
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Auth.route) { AuthScreen(navController) }
        composable(Screen.Challenge.route) { ChallengeScreen(navController) }
        composable(Screen.Voting.route) {}

        // all other screens will be added later
    }
}
