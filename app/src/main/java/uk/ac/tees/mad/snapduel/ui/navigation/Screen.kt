package uk.ac.tees.mad.snapduel.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import uk.ac.tees.mad.snapduel.data.Submission
import uk.ac.tees.mad.snapduel.ui.screens.AuthScreen
import uk.ac.tees.mad.snapduel.ui.screens.ChallengeScreen
import uk.ac.tees.mad.snapduel.ui.screens.PhotoDetailsScreen
import uk.ac.tees.mad.snapduel.ui.screens.SplashScreen
import uk.ac.tees.mad.snapduel.ui.screens.VotingScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Auth : Screen("auth_screen")
    object Challenge : Screen("challenge_screen")
    object Voting : Screen("voting_screen")
    object PhotoDetails : Screen("photo_details_screen")
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Auth.route) { AuthScreen(navController) }
        composable(Screen.Challenge.route) { ChallengeScreen(navController) }
        composable(Screen.Voting.route) { VotingScreen(navController) }
        composable(Screen.PhotoDetails.route) { backStackEntry ->
            val submission = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<Submission>("submission")

            submission?.let {
                PhotoDetailsScreen(navController, it)
            }
        }


        // all other screens will be added later
    }
}
