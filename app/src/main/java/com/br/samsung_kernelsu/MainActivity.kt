package com.br.samsung_kernelsu

import com.br.samsung_kernelsu.data.DeviceListCache
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.br.samsung_kernelsu.data.SettingsRepository
import com.br.samsung_kernelsu.i18n.AppLanguage
import com.br.samsung_kernelsu.i18n.EnglishStrings
import com.br.samsung_kernelsu.i18n.LocalLanguage
import com.br.samsung_kernelsu.i18n.LocalStrings
import com.br.samsung_kernelsu.i18n.PortugueseStrings
import com.br.samsung_kernelsu.ui.screens.BuildScreen
import com.br.samsung_kernelsu.ui.screens.BuildsScreen
import com.br.samsung_kernelsu.ui.screens.ConfigScreen
import com.br.samsung_kernelsu.ui.screens.LoginScreen
import com.br.samsung_kernelsu.ui.screens.RunDetailScreen
import com.br.samsung_kernelsu.ui.theme.KernelSU_NextTheme
import com.br.samsung_kernelsu.viewmodel.AuthViewModel
import com.br.samsung_kernelsu.viewmodel.BuildViewModel

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val settings = SettingsRepository(applicationContext)
        val initialLanguage = AppLanguage.fromCode(settings.getLanguage())
        DeviceListCache.init(applicationContext)
        setContent {
            var currentLanguage by remember { mutableStateOf(initialLanguage) }

            val strings = when (currentLanguage) {
                AppLanguage.ENGLISH -> EnglishStrings
                AppLanguage.PORTUGUESE -> PortugueseStrings
            }

            LaunchedEffect(currentLanguage) {
                settings.saveLanguage(currentLanguage.code)
            }

            CompositionLocalProvider(
                LocalStrings provides strings,
                LocalLanguage provides currentLanguage
            ) {
                KernelSU_NextTheme {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()
                    val buildViewModel: BuildViewModel = viewModel()

                    val currentSession by authViewModel.currentSession.collectAsStateWithLifecycle()
                    val startDestination = if (currentSession != null) "config" else "login"

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable("login") {
                            LoginScreen(
                                authViewModel = authViewModel,
                                onLoginSuccess = {
                                    navController.navigate("config") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onLanguageChange = { currentLanguage = it }
                            )
                        }

                        composable("config") {
                            val session = currentSession
                            if (session == null) {
                                LaunchedEffect(Unit) {
                                    navController.navigate("login") {
                                        popUpTo("config") { inclusive = true }
                                    }
                                }
                            } else {
                                LaunchedEffect(session.login) {
                                    buildViewModel.resumeLastBuildIfAny(session)
                                }

                                ConfigScreen(
                                    session = session,
                                    buildViewModel = buildViewModel,
                                    onStartBuild = { s, config, strings ->
                                        buildViewModel.startBuild(s, config, strings)
                                        navController.navigate("build")
                                    },
                                    onOpenBuilds = {
                                        navController.navigate("builds")
                                    },
                                    onLogout = {
                                        authViewModel.logout()
                                    },
                                    onLanguageChange = { currentLanguage = it }
                                )
                            }
                        }

                        composable("build") {
                            BuildScreen(
                                buildViewModel = buildViewModel,
                                onBack = { navController.popBackStack() },
                                onOpenRun = { runId ->
                                    navController.navigate("run/$runId")
                                }
                            )
                        }

                        composable("builds") {
                            val session = currentSession
                            if (session != null) {
                                BuildsScreen(
                                    session = session,
                                    buildViewModel = buildViewModel,
                                    onOpenRun = { runId ->
                                        navController.navigate("run/$runId")
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        composable(
                            "run/{runId}",
                            arguments = listOf(navArgument("runId") { type = NavType.LongType })
                        ) { entry ->
                            val runId = entry.arguments?.getLong("runId") ?: -1L
                            val session = currentSession
                            if (session != null && runId > 0) {
                                RunDetailScreen(
                                    session = session,
                                    runId = runId,
                                    buildViewModel = buildViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}