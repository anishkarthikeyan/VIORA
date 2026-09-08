package com.viora.app.core.navigation

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.viora.app.MainActivity
import com.viora.app.presentation.history.HistoryScreen
import com.viora.app.presentation.history.HistoryViewModel
import com.viora.app.presentation.history.HistoryViewModelFactory
import com.viora.app.presentation.home.HomeScreen
import com.viora.app.presentation.home.HomeViewModel
import com.viora.app.presentation.home.HomeViewModelFactory
import com.viora.app.presentation.linkmessage.LinkMessageScreen
import com.viora.app.presentation.result.ResultScreen
import com.viora.app.presentation.scanner.ScannerScreen
import com.viora.app.presentation.scanner.ScannerViewModel
import com.viora.app.presentation.scanner.ScannerViewModelFactory
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun VioraNavigation(
    sharedText: StateFlow<String?>? = null,
    sharedImage: StateFlow<Uri?>? = null,
    navController: NavHostController = rememberNavController()
) {
    // One analysis pipeline shared by Scanner and Result so "View Details" can read
    // the same ThreatAssessment that drove the overlay.
    val activity = LocalContext.current as ComponentActivity
    val scannerViewModel: ScannerViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = ScannerViewModelFactory(activity.applicationContext)
    )

    // Home's "Screenshot" action: Android's built-in photo picker (no storage
    // permission needed), feeding the selected image through the exact same
    // existing OCR/GuardianAI pipeline the Share Sheet's image flow already uses.
    val coroutineScope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val accepted = scannerViewModel.onSharedImage(uri, activity.contentResolver)
                if (accepted) {
                    navController.navigate(NavRoute.Result.route) {
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    // Share-intent pipeline: ACTION_SEND text → input processor → VioraContext →
    // ThreatEngine → Result. Navigates straight to the detailed view; the camera
    // pipeline is untouched.
    if (sharedText != null) {
        val pendingSharedText by sharedText.collectAsStateWithLifecycle()
        LaunchedEffect(pendingSharedText) {
            val text = pendingSharedText ?: return@LaunchedEffect
            val mainActivity = activity as? MainActivity
            if (mainActivity != null) {
                val sourceApp = mainActivity.intent?.`package` // sending app, when available
                val accepted = scannerViewModel.onSharedText(text, sourceApp)
                if (accepted) {
                    navController.navigate(NavRoute.Result.route) {
                        launchSingleTop = true
                    }
                }
                // Clear so the same share is not reprocessed on recomposition.
                mainActivity.clearSharedText()
            }
        }
    }

    // Shared-image pipeline: Uri → SharedImageProcessor → ML Kit OCR → extracted
    // text → VioraContext → ThreatEngine → Result. Runs only while processing is
    // in flight, then navigates; unreadable/no-text images are ignored gracefully.
    if (sharedImage != null) {
        val pendingSharedImage by sharedImage.collectAsStateWithLifecycle()
        LaunchedEffect(pendingSharedImage) {
            val uri = pendingSharedImage ?: return@LaunchedEffect
            val mainActivity = activity as? MainActivity
            if (mainActivity != null) {
                val accepted = scannerViewModel.onSharedImage(uri, activity.contentResolver)
                if (accepted) {
                    navController.navigate(NavRoute.Result.route) {
                        launchSingleTop = true
                    }
                }
                mainActivity.clearSharedImage()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoute.Home.route
    ) {
        composable(NavRoute.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(activity.applicationContext)
            )
            HomeScreen(
                onCheckWithVioraClick = { navController.navigate(NavRoute.Scanner.route) },
                onCheckLinkMessageClick = { navController.navigate(NavRoute.LinkMessage.route) },
                onCheckScreenshotImageClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onViewHistoryClick = { navController.navigate(NavRoute.History.route) },
                viewModel = homeViewModel
            )
        }

        composable(NavRoute.Scanner.route) {
            ScannerScreen(
                onBackClick = { navController.popBackStack() },
                onViewResultClick = { navController.navigate(NavRoute.Result.route) },
                viewModel = scannerViewModel
            )
        }

        composable(NavRoute.LinkMessage.route) {
            LinkMessageScreen(
                onBackClick = { navController.popBackStack() },
                onChecked = {
                    navController.navigate(NavRoute.Result.route) {
                        launchSingleTop = true
                    }
                },
                viewModel = scannerViewModel
            )
        }

        composable(NavRoute.Result.route) {
            val uiState by scannerViewModel.uiState.collectAsStateWithLifecycle()
            ResultScreen(
                assessment = uiState.assessment,
                context = uiState.resultContext,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoute.History.route) {
            val historyViewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModelFactory(activity.applicationContext)
            )
            val history by historyViewModel.history.collectAsStateWithLifecycle()
            HistoryScreen(
                history = history,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
