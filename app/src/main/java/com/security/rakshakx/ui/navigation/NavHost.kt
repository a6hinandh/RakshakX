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
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.screens.*
import com.security.rakshakx.ui.theme.*

sealed class Screen(val route: String, val label: String, val filledIcon: ImageVector, val outlinedIcon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Filled.Shield, Icons.Outlined.Shield)
    data object Threats : Screen("threats", "Threats", Icons.Filled.Warning, Icons.Outlined.Warning)
    data object Correlation : Screen("correlation", "Timeline", Icons.Filled.Timeline, Icons.Outlined.Timeline)
    data object Privacy : Screen("privacy", "Privacy", Icons.Filled.Lock, Icons.Outlined.Lock)
    data object LiveThreat : Screen("live_threat", "Live", Icons.Filled.RadioButtonChecked, Icons.Outlined.RadioButtonChecked)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    data object Scanning : Screen("scanning", "Scanning", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner)
    data object Report : Screen("report", "Report", Icons.Filled.Assessment, Icons.Outlined.Assessment)
    data object ThreatIntel : Screen("threat_intel", "Threat Intel", Icons.Filled.Cloud, Icons.Outlined.Cloud)
    data object FamilyProtection : Screen("family", "Family", Icons.Filled.FamilyRestroom, Icons.Outlined.FamilyRestroom)
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
        modifier = modifier.background(colors.backgroundDeep),
        containerColor = colors.backgroundDeep,
        bottomBar = { RakshakXBottomBar(navController) },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            enterTransition = { fadeIn(tween(250)) + slideInVertically(tween(250)) { 30 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically(tween(200)) { 30 } },
        ) {
            composable(Screen.Home.route) {
                HomeDashboardScreen(
                    activity = activity,
                    onNavigateToThreats = { navController.navigate(Screen.Threats.route) { launchSingleTop = true } },
                    onNavigateToCorrelation = { navController.navigate(Screen.Correlation.route) { launchSingleTop = true } },
                    onNavigateToLiveThreat = { navController.navigate(Screen.LiveThreat.route) { launchSingleTop = true } },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                    onNavigateToScanning = { navController.navigate(Screen.Scanning.route) { launchSingleTop = true } },
                    onNavigateToReport = { navController.navigate(Screen.Report.route) { launchSingleTop = true } },
                    onNavigateToThreatIntel = { navController.navigate(Screen.ThreatIntel.route) { launchSingleTop = true } }
                )
            }
            composable(Screen.Threats.route) { ThreatLogsScreen() }
            composable(Screen.Correlation.route) { CorrelationScreen() }
            composable(Screen.Privacy.route) { PrivacyScreen() }
            composable(Screen.LiveThreat.route) { LiveThreatScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Settings.route) { SettingsScreen(activity = activity, onBack = { navController.popBackStack() }) }
            composable(Screen.Scanning.route) { ScanningScreen(activity = activity, onBack = { navController.popBackStack() }) }
            composable(Screen.Report.route) { ReportScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.ThreatIntel.route) { ThreatIntelScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.FamilyProtection.route) { FamilyProtectionScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
private fun RakshakXBottomBar(navController: NavHostController) {
    val colors = LocalRakshakXColors.current
    val haptics = rememberHaptics()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val hiddenRoutes = listOf(Screen.Settings.route, Screen.LiveThreat.route, Screen.Scanning.route, Screen.Report.route, Screen.ThreatIntel.route, Screen.FamilyProtection.route)

    AnimatedVisibility(
        visible = currentRoute !in hiddenRoutes,
        enter = slideInVertically(tween(200)) { it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(150)) { it } + fadeOut(tween(150))
    ) {
    Column {
    HorizontalDivider(color = SlateBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Charcoal,
        contentColor = TextWhite,
        tonalElevation = 0.dp,
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    haptics.tick()
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) screen.filledIcon else screen.outlinedIcon,
                        contentDescription = screen.label,
                        modifier = Modifier.size(21.dp)
                    )
                },
                label = {
                    Text(screen.label, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    unselectedIconColor = colors.textMuted,
                    unselectedTextColor = colors.textMuted,
                    indicatorColor = colors.primaryMuted
                )
            )
        }
    }
    } // Column
    } // AnimatedVisibility
}
