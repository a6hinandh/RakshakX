package com.security.rakshakx.ui.navigation

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.security.rakshakx.ui.screens.*
import com.security.rakshakx.ui.theme.*

/** Navigation routes */
sealed class Screen(val route: String, val label: String, val filledIcon: ImageVector, val outlinedIcon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Filled.Shield, Icons.Outlined.Shield)
    data object Threats : Screen("threats", "Threats", Icons.Filled.Warning, Icons.Outlined.Warning)
    data object Correlation : Screen("correlation", "Timeline", Icons.Filled.Timeline, Icons.Outlined.Timeline)
    data object Privacy : Screen("privacy", "Privacy", Icons.Filled.Lock, Icons.Outlined.Lock)
    // Non-tab destinations
    data object LiveThreat : Screen("live_threat", "Live", Icons.Filled.RadioButtonChecked, Icons.Outlined.RadioButtonChecked)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavItems = listOf(Screen.Home, Screen.Threats, Screen.Correlation, Screen.Privacy)

@Composable
fun RakshakXNavHost(
    activity: Activity,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val colors = LocalRakshakXColors.current

    Scaffold(
        modifier = modifier.background(colors.background),
        containerColor = colors.background,
        bottomBar = { RakshakXBottomBar(navController) },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
        ) {
            composable(Screen.Home.route) {
                HomeDashboardScreen(
                    activity = activity,
                    onNavigateToThreats = {
                        navController.navigate(Screen.Threats.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToCorrelation = {
                        navController.navigate(Screen.Correlation.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLiveThreat = {
                        navController.navigate(Screen.LiveThreat.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Threats.route) {
                ThreatLogsScreen()
            }
            composable(Screen.Correlation.route) {
                CorrelationScreen()
            }
            composable(Screen.Privacy.route) {
                PrivacyScreen()
            }
            composable(Screen.LiveThreat.route) {
                LiveThreatScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun RakshakXBottomBar(navController: NavHostController) {
    val colors = LocalRakshakXColors.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show bottom bar on main tabs
    val currentRoute = currentDestination?.route
    if (currentRoute == Screen.Settings.route || currentRoute == Screen.LiveThreat.route) return

    NavigationBar(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        containerColor = colors.cardBackground,
        contentColor = colors.textPrimary,
        tonalElevation = 0.dp,
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) screen.filledIcon else screen.outlinedIcon,
                        contentDescription = screen.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        screen.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    unselectedIconColor = colors.textMuted,
                    unselectedTextColor = colors.textMuted,
                    indicatorColor = colors.primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}
