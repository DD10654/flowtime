package com.flowtime.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flowtime.app.ui.analytics.AnalyticsScreen
import com.flowtime.app.ui.habits.HabitsScreen
import com.flowtime.app.ui.planner.PlannerScreen
import com.flowtime.app.ui.settings.SettingsScreen
import com.flowtime.app.ui.tasks.TasksScreen
import com.flowtime.app.ui.theme.FlowtimeTheme

sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Planner : Dest("planner", "Planner", Icons.Outlined.CalendarMonth)
    data object Tasks : Dest("tasks", "Tasks", Icons.Outlined.Checklist)
    data object Habits : Dest("habits", "Habits", Icons.Outlined.Repeat)
    data object Analytics : Dest("analytics", "Analytics", Icons.Outlined.BarChart)
    data object Settings : Dest("settings", "Settings", Icons.Outlined.Settings)
}

private val bottomDests = listOf(Dest.Planner, Dest.Tasks, Dest.Habits, Dest.Analytics, Dest.Settings)

@Composable
fun AppRoot() {
    FlowtimeTheme {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = {
                val backStack by navController.currentBackStackEntryAsState()
                val current = backStack?.destination?.route
                NavigationBar {
                    bottomDests.forEach { dest ->
                        NavigationBarItem(
                            selected = current == dest.route,
                            onClick = {
                                if (current != dest.route) {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { androidx.compose.material3.Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Dest.Planner.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(Dest.Planner.route) { PlannerScreen() }
                composable(Dest.Tasks.route) { TasksScreen() }
                composable(Dest.Habits.route) { HabitsScreen() }
                composable(Dest.Analytics.route) { AnalyticsScreen() }
                composable(Dest.Settings.route) { SettingsScreen() }
            }
        }
    }
}
