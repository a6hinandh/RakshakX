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

sealed class Screen(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    // ── Bottom nav tabs ──
    data object Home       : Screen("home",    "Home",    Icons.Filled.Home,     Icons.Outlined.Home)
    data object Shield     : Screen("shield",  "Shield",  Icons.Filled.Shield,   Icons.Outlined.Shield)
    data object Network    : Screen("network", "Network", Icons.Filled.Hub,      Icons.Outlined.Hub)
    data object Threats    : Screen("threats", "Threats", Icons.Filled.BugReport,Icons.Outlined.BugReport)
    data object More       : Screen("more",    "More",    Icons.Filled.GridView,  Icons.Outlined.GridView)

    // ── Detail screens (hidden from bottom nav) ──
    data object LiveThreat       : Screen("live_threat",      "Live",           Icons.Filled.RadioButtonChecked, Icons.Outlined.RadioButtonChecked)
    data object Settings         : Screen("settings",         "Settings",       Icons.Filled.Settings,           Icons.Outlined.Settings)
    data object Scanning         : Screen("scanning",         "Scan",           Icons.Filled.QrCodeScanner,      Icons.Outlined.QrCodeScanner)
    data object Report           : Screen("report",           "Report",         Icons.Filled.Assessment,         Icons.Outlined.Assessment)
    data object ThreatIntel      : Screen("threat_intel",     "Threat Intel",   Icons.Filled.Cloud,              Icons.Outlined.Cloud)
    data object FamilyProtection : Screen("family",           "Family",         Icons.Filled.FamilyRestroom,     Icons.Outlined.FamilyRestroom)
    data object Correlation      : Screen("correlation",      "Timeline",       Icons.Filled.Timeline,           Icons.Outlined.Timeline)

    // ── New cyber screens ──
    data object DeviceHealth     : Screen("device_health",    "Device Health",  Icons.Filled.PhoneAndroid,       Icons.Outlined.PhoneAndroid)
    data object AppAudit         : Screen("app_audit",        "App Audit",      Icons.Filled.Apps,               Icons.Outlined.Apps)
    data object WifiAudit        : Screen("wifi_audit",       "Wi-Fi Audit",    Icons.Filled.Wifi,               Icons.Outlined.Wifi)
    data object Firewall         : Screen("firewall",         "Firewall",       Icons.Filled.Security,           Icons.Outlined.Security)
    data object NetworkScan      : Screen("network_scan",     "Network Scan",   Icons.Filled.Router,             Icons.Outlined.Router)
    data object TrafficMonitor   : Screen("traffic_monitor",  "Traffic",        Icons.Filled.ShowChart,          Icons.Outlined.ShowChart)
    data object PrivacyDashboard : Screen("privacy_dashboard","Privacy",        Icons.Filled.TrackChanges,       Icons.Outlined.TrackChanges)
    data object Vault            : Screen("vault",            "Vault",          Icons.Filled.VpnKey,             Icons.Outlined.VpnKey)
    data object PasswordStudio   : Screen("password_studio",  "Password Studio",Icons.Filled.Password,           Icons.Outlined.Password)
    data object ThreatAnalytics  : Screen("threat_analytics", "Analytics",      Icons.Filled.Analytics,          Icons.Outlined.Analytics)
    data object ForensicExport   : Screen("forensic_export",  "Forensics",      Icons.Filled.Inventory,          Icons.Outlined.Inventory)
    data object AttackMatrix     : Screen("attack_matrix",    "ATT&CK Matrix",  Icons.Filled.GridOn,             Icons.Outlined.GridOn)
    data object ShieldsControl   : Screen("shields_control",  "Shield Center",  Icons.Filled.Shield,             Icons.Outlined.Shield)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Shield,
    Screen.Network,
    Screen.Threats,
    Screen.More
)

private val hiddenFromNav = setOf(
    Screen.LiveThreat.route, Screen.Settings.route, Screen.Scanning.route,
    Screen.Report.route, Screen.ThreatIntel.route, Screen.FamilyProtection.route,
    Screen.Correlation.route, Screen.DeviceHealth.route, Screen.AppAudit.route,
    Screen.WifiAudit.route, Screen.Firewall.route, Screen.NetworkScan.route,
    Screen.TrafficMonitor.route, Screen.PrivacyDashboard.route, Screen.Vault.route,
    Screen.PasswordStudio.route, Screen.ThreatAnalytics.route, Screen.ForensicExport.route,
    Screen.AttackMatrix.route, Screen.ShieldsControl.route
)

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
                .padding(
                    start = paddingValues.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    bottom = paddingValues.calculateBottomPadding()
                ),
            enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(220)) { 24 } },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) + slideOutVertically(tween(180)) { 24 } },
        ) {
            // ── Main tabs ──
            composable(Screen.Home.route) {
                HomeDashboardScreen(
                    activity = activity,
                    onNavigateToThreats     = {
                        navController.navigate(Screen.Threats.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCorrelation = { navController.navigate(Screen.Correlation.route) { launchSingleTop = true } },
                    onNavigateToLiveThreat  = { navController.navigate(Screen.LiveThreat.route) { launchSingleTop = true } },
                    onNavigateToSettings    = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                    onNavigateToShieldsControl = { navController.navigate(Screen.ShieldsControl.route) { launchSingleTop = true } },
                    onNavigateToScanning    = { navController.navigate(Screen.Scanning.route) { launchSingleTop = true } },
                    onNavigateToReport      = { navController.navigate(Screen.Report.route) { launchSingleTop = true } },
                    onNavigateToThreatIntel = { navController.navigate(Screen.ThreatIntel.route) { launchSingleTop = true } },
                    onNavigateToDeviceHealth = { navController.navigate(Screen.DeviceHealth.route) { launchSingleTop = true } },
                    onNavigateToAttackMatrix = { navController.navigate(Screen.AttackMatrix.route) { launchSingleTop = true } }
                )
            }
            composable(Screen.Shield.route) {
                ShieldsControlScreen(
                    activity = activity,
                    onNavigateToPrivacyDashboard = { navController.navigate(Screen.PrivacyDashboard.route) { launchSingleTop = true } },
                    onBack = null
                )
            }
            composable(Screen.Network.route) {
                NetworkHubScreen(
                    onNavigateToWifiAudit      = { navController.navigate(Screen.WifiAudit.route) { launchSingleTop = true } },
                    onNavigateToFirewall       = { navController.navigate(Screen.Firewall.route) { launchSingleTop = true } },
                    onNavigateToNetworkScan    = { navController.navigate(Screen.NetworkScan.route) { launchSingleTop = true } },
                    onNavigateToTrafficMonitor = { navController.navigate(Screen.TrafficMonitor.route) { launchSingleTop = true } },
                    onNavigateToSettings       = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                    onNavigateToShield         = {
                        navController.navigate(Screen.Shield.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Threats.route) { ThreatLogsScreen() }
            composable(Screen.More.route) {
                MoreHubScreen(
                    onNavigateToDeviceHealth    = { navController.navigate(Screen.DeviceHealth.route) { launchSingleTop = true } },
                    onNavigateToAppAudit        = { navController.navigate(Screen.AppAudit.route) { launchSingleTop = true } },
                    onNavigateToPrivacyDashboard = { navController.navigate(Screen.PrivacyDashboard.route) { launchSingleTop = true } },
                    onNavigateToVault           = { navController.navigate(Screen.Vault.route) { launchSingleTop = true } },
                    onNavigateToThreatAnalytics = { navController.navigate(Screen.ThreatAnalytics.route) { launchSingleTop = true } },
                    onNavigateToPasswordStudio  = { navController.navigate(Screen.PasswordStudio.route) { launchSingleTop = true } },
                    onNavigateToForensicExport  = { navController.navigate(Screen.ForensicExport.route) { launchSingleTop = true } },
                    onNavigateToThreatIntel     = { navController.navigate(Screen.ThreatIntel.route) { launchSingleTop = true } },
                    onNavigateToFamily          = { navController.navigate(Screen.FamilyProtection.route) { launchSingleTop = true } },
                    onNavigateToReport          = { navController.navigate(Screen.Report.route) { launchSingleTop = true } },
                    onNavigateToSettings        = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } }
                )
            }

            // ── Existing detail screens ──
            composable(Screen.LiveThreat.route)       { LiveThreatScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Settings.route)         { SettingsScreen(activity = activity, onBack = { navController.popBackStack() }) }
            composable(Screen.Scanning.route)         { ScanningScreen(activity = activity, onBack = { navController.popBackStack() }) }
            composable(Screen.Report.route)           { ReportScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.ThreatIntel.route)      { ThreatIntelScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.FamilyProtection.route) { FamilyProtectionScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Correlation.route)      { CorrelationScreen(onBack = { navController.popBackStack() }) }

            // ── New cyber screens ──
            composable(Screen.DeviceHealth.route)     { DeviceHealthScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.AppAudit.route)         { AppAuditScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.WifiAudit.route)        { WifiAuditScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Firewall.route)         { FirewallScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.NetworkScan.route)      { NetworkScanScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.TrafficMonitor.route)   { TrafficMonitorScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.PrivacyDashboard.route) { PrivacyDashboardScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Vault.route)            { VaultScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.PasswordStudio.route)   { PasswordStudioScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.ThreatAnalytics.route)  { ThreatAnalyticsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.ForensicExport.route)   { ForensicExportScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.AttackMatrix.route)     { AttackMatrixScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.ShieldsControl.route) {
                ShieldsControlScreen(
                    activity = activity,
                    onNavigateToPrivacyDashboard = { navController.navigate(Screen.PrivacyDashboard.route) { launchSingleTop = true } },
                    onBack = { navController.popBackStack() }
                )
            }
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

    AnimatedVisibility(
        visible = currentRoute !in hiddenFromNav,
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
                            if (screen.route == Screen.Home.route) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
        }
    }
}
