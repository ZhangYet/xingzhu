package com.xingzhu.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xingzhu.ui.add.AddScreen
import com.xingzhu.ui.library.LibraryScreen
import com.xingzhu.ui.reader.ReaderScreen

object Routes {
    const val LIBRARY = "library"
    const val ADD = "add"
    const val READER = "reader/{poemId}"
    fun reader(poemId: Long) = "reader/$poemId"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY,
    ) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onAddClick = { navController.navigate(Routes.ADD) },
                onPoemClick = { id -> navController.navigate(Routes.reader(id)) },
            )
        }
        composable(Routes.ADD) {
            AddScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("poemId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val poemId = backStackEntry.arguments?.getLong("poemId") ?: 0L
            ReaderScreen(poemId = poemId, onBack = { navController.popBackStack() })
        }
    }
}
