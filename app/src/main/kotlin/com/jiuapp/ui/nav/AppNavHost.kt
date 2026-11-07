package com.jiuapp.ui.nav

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.MaterialTheme
import com.jiuapp.R
import com.jiuapp.ui.common.LoadingBox
import com.jiuapp.ui.onboarding.RootViewModel
import com.jiuapp.ui.competition.CompetitionFormScreen
import com.jiuapp.ui.competition.CompetitionListScreen
import com.jiuapp.ui.competition.CompetitionDetailScreen
import com.jiuapp.ui.data.DataScreen
import com.jiuapp.ui.data.PrivacyScreen
import com.jiuapp.ui.health.HealthScreen
import com.jiuapp.ui.home.HomeScreen
import com.jiuapp.ui.onboarding.OnboardingScreen
import com.jiuapp.ui.professor.ProfessorScreen
import com.jiuapp.ui.profile.ProfileScreen
import com.jiuapp.ui.settings.SettingsScreen
import com.jiuapp.ui.sessions.SessionDetailScreen
import com.jiuapp.ui.sessions.SessionFormScreen
import com.jiuapp.ui.sessions.SessionsScreen
import com.jiuapp.ui.stats.StatsScreen
import com.jiuapp.ui.techniques.OpponentFormScreen
import com.jiuapp.ui.techniques.PlanEditorScreen
import com.jiuapp.ui.techniques.TechniqueFormScreen
import com.jiuapp.ui.techniques.TechniquesScreen
import kotlinx.serialization.Serializable

/** Destinos de nível superior (barra inferior). */
@Serializable object HomeRoute
@Serializable object SessionsRoute
@Serializable object StatsRoute
@Serializable object TechniquesRoute
@Serializable object ProfileRoute

/** Telas de detalhe e formulários. Ids vazios significam "novo". */
@Serializable data class SessionDetailRoute(val id: String)
@Serializable data class SessionFormRoute(val id: String = NEW)
@Serializable data class TechniqueFormRoute(val id: String = NEW)
@Serializable data class PlanEditorRoute(val id: String = NEW)
@Serializable data class OpponentFormRoute(val id: String = NEW)
@Serializable data class CompetitionDetailRoute(val id: String)
@Serializable data class CompetitionFormRoute(val id: String = NEW)
@Serializable object CompetitionListRoute
@Serializable object HealthRoute
@Serializable object ProfessorRoute
@Serializable object SettingsRoute
@Serializable object DataRoute
@Serializable object PrivacyRoute

const val NEW = "new"

private data class TabItem(val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: Any)

private val tabs = listOf(
    TabItem(R.string.tab_home, Icons.Filled.Home, HomeRoute),
    TabItem(R.string.tab_sessions, Icons.AutoMirrored.Filled.List, SessionsRoute),
    TabItem(R.string.tab_stats, Icons.Filled.BarChart, StatsRoute),
    TabItem(R.string.tab_techniques, Icons.AutoMirrored.Filled.MenuBook, TechniquesRoute),
    TabItem(R.string.tab_profile, Icons.Filled.Person, ProfileRoute),
)

/** Raiz: mostra o onboarding enquanto o usuário não o concluiu, depois o app principal. */
@Composable
fun AppRoot(viewModel: RootViewModel = hiltViewModel()) {
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
    when (onboardingDone) {
        null -> LoadingBox()
        false -> OnboardingScreen()
        true -> MainScaffold()
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    Scaffold(
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = backStack?.destination?.let { destination ->
                            destination.hasRoute(tab.route::class)
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        JiuNavHost(navController, Modifier.padding(padding))
    }
}

@Composable
private fun JiuNavHost(navController: NavHostController, modifier: Modifier) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> {
            HomeScreen(onOpenCompetitions = { navController.navigate(CompetitionListRoute) }, onOpenHealth = { navController.navigate(HealthRoute) })
        }
        composable<SessionsRoute> {
            SessionsScreen(
                onOpenSession = { navController.navigate(SessionDetailRoute(it)) },
                onAddSession = { navController.navigate(SessionFormRoute()) },
            )
        }
        composable<StatsRoute> { StatsScreen() }
        composable<TechniquesRoute> {
            TechniquesScreen(
                onOpenTechnique = { navController.navigate(TechniqueFormRoute(it)) },
                onAddTechnique = { navController.navigate(TechniqueFormRoute()) },
                onOpenPlan = { navController.navigate(PlanEditorRoute(it)) },
                onAddPlan = { navController.navigate(PlanEditorRoute()) },
                onOpenOpponent = { navController.navigate(OpponentFormRoute(it)) },
                onAddOpponent = { navController.navigate(OpponentFormRoute()) },
            )
        }
        composable<ProfileRoute> {
            ProfileScreen(
                onOpenHealth = { navController.navigate(HealthRoute) },
                onOpenCompetitions = { navController.navigate(CompetitionListRoute) },
                onOpenProfessor = { navController.navigate(ProfessorRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenData = { navController.navigate(DataRoute) },
                onOpenPrivacy = { navController.navigate(PrivacyRoute) },
            )
        }
        composable<SessionDetailRoute> {
            SessionDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(SessionFormRoute(it)) },
            )
        }
        composable<SessionFormRoute> {
            SessionFormScreen(onDone = { navController.popBackStack() })
        }
        composable<TechniqueFormRoute> {
            TechniqueFormScreen(onDone = { navController.popBackStack() })
        }
        composable<PlanEditorRoute> {
            PlanEditorScreen(onDone = { navController.popBackStack() })
        }
        composable<OpponentFormRoute> {
            OpponentFormScreen(onDone = { navController.popBackStack() })
        }
        composable<CompetitionListRoute> {
            CompetitionListScreen(
                onBack = { navController.popBackStack() },
                onOpen = { navController.navigate(CompetitionDetailRoute(it)) },
                onAdd = { navController.navigate(CompetitionFormRoute()) },
            )
        }
        composable<CompetitionDetailRoute> {
            CompetitionDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(CompetitionFormRoute(it)) },
            )
        }
        composable<CompetitionFormRoute> {
            CompetitionFormScreen(onDone = { navController.popBackStack() })
        }
        composable<HealthRoute> { HealthScreen(onBack = { navController.popBackStack() }) }
        composable<ProfessorRoute> { ProfessorScreen(onBack = { navController.popBackStack() }) }
        composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable<DataRoute> { DataScreen(onBack = { navController.popBackStack() }) }
        composable<PrivacyRoute> { PrivacyScreen(onBack = { navController.popBackStack() }) }
    }
}

private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
