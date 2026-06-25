package com.smartorders.driverhelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.*
import com.smartorders.driverhelper.data.AppPreferences
import com.smartorders.driverhelper.ui.*
import com.smartorders.driverhelper.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Rules : Screen("rules", "Rules", Icons.Default.Rule)
    object Debug : Screen("debug", "Debug", Icons.Default.BugReport)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved stats into AppState
        AppState.updateStats(
            detected = AppPreferences.getDetectedTrips(this),
            accepted = AppPreferences.getAcceptedTrips(this),
            rejected = AppPreferences.getRejectedTrips(this),
            totalSar = AppPreferences.getTotalSar(this)
        )
        AppState.setAutoAccept(AppPreferences.isAutoAccept(this))

        setContent {
            SmartOrdersTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val items = listOf(Screen.Dashboard, Screen.Rules, Screen.Debug, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Surface,
                contentColor = OnSurface
            ) {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Purple60,
                            selectedTextColor = Purple60,
                            indicatorColor = SurfaceVariant,
                            unselectedIconColor = OnSurfaceVariant,
                            unselectedTextColor = OnSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Rules.route) { RulesScreen() }
            composable(Screen.Debug.route) { DebugLogScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
