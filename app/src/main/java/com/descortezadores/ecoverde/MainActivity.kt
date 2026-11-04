package com.descortezadores.ecoverde

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.descortezadores.ecoverde.presentation.features.analysis.AnalysisResultScreen
import com.descortezadores.ecoverde.presentation.features.analysis.AnalysisResultViewModel
import com.descortezadores.ecoverde.presentation.features.auth.AuthViewModel
import com.descortezadores.ecoverde.presentation.features.auth.LoginAndSessionScreen
import com.descortezadores.ecoverde.presentation.theme.EcoVerdeMotion
import com.descortezadores.ecoverde.presentation.theme.EcoVerdeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Forzar tasa de refresco a 120 Hz respetando la resolución nativa configurada por el usuario
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.attributes = window.attributes.apply {
                preferredRefreshRate = 120f
            }
        }

        setContent {
            EcoVerdeTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
                val activeSession = authUiState.activeSession

                AnimatedContent(
                    targetState = activeSession,
                    transitionSpec = {
                        if (targetState != null) {
                            (fadeIn(animationSpec = tween(durationMillis = EcoVerdeMotion.DurationStandardEnter, easing = EcoVerdeMotion.EmphasizedDecelerateEasing)) +
                                scaleIn(initialScale = 0.96f, animationSpec = tween(durationMillis = EcoVerdeMotion.DurationStandardEnter, easing = EcoVerdeMotion.EmphasizedDecelerateEasing)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(durationMillis = EcoVerdeMotion.DurationQuickExit, easing = EcoVerdeMotion.EmphasizedAccelerateEasing))
                                )
                        } else {
                            (fadeIn(animationSpec = tween(durationMillis = EcoVerdeMotion.DurationStandardEnter, easing = EcoVerdeMotion.EmphasizedDecelerateEasing)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(durationMillis = EcoVerdeMotion.DurationQuickExit, easing = EcoVerdeMotion.EmphasizedAccelerateEasing)) +
                                        scaleOut(targetScale = 0.96f, animationSpec = tween(durationMillis = EcoVerdeMotion.DurationQuickExit, easing = EcoVerdeMotion.EmphasizedAccelerateEasing))
                                )
                        }
                    },
                    label = "SessionMainScreenAnimatedContent"
                ) { session ->
                    if (session != null) {
                        val analysisViewModel: AnalysisResultViewModel = hiltViewModel()
                        val analysisUiState by analysisViewModel.uiState.collectAsStateWithLifecycle()

                        AnalysisResultScreen(
                            session = session,
                            uiState = analysisUiState,
                        onSelectDestination = analysisViewModel::selectMainDestination,
                        onSelectTab = analysisViewModel::selectAnalysisTab,
                        onSelectSampleType = analysisViewModel::selectSampleType,
                        onAnalyzeBitmap = analysisViewModel::analyzeCapturedOrImportedBitmap,
                        onAnalyzeGalleryUri = analysisViewModel::analyzeGalleryUri,
                        onAnalyzeCameraFile = analysisViewModel::analyzeCameraCaptureFile,
                        onRefreshDeviceLocation = analysisViewModel::refreshCurrentDeviceLocation,
                        onRunSyntheticSample = analysisViewModel::analyzeSyntheticFieldSample,
                        onCategoryFilterSelected = analysisViewModel::onCategoryFilterSelected,
                        onAlphaChanged = analysisViewModel::onOverlayAlphaChanged,
                        onToggleOriginalComparison = analysisViewModel::toggleOriginalComparison,
                        onAssociationModeChanged = analysisViewModel::onAssociationModeChanged,
                        onSelectExistingTree = analysisViewModel::onSelectExistingTree,
                        onHomeTreeFilterChanged = analysisViewModel::onHomeTreeFilterChanged,
                        onOpenTreeDetailClick = analysisViewModel::openTreeDetailSection,
                        onCloseTreeDetailClick = analysisViewModel::closeTreeDetailSection,
                        onStartNewCaptureForTreeClick = analysisViewModel::startNewCaptureForTree,
                        onCreateTreeSectionDirectlyClick = analysisViewModel::registerNewTreeSectionDirectly,
                        onNewTreeNameChanged = analysisViewModel::onNewTreeNameChanged,
                        onNewTreeSpeciesChanged = analysisViewModel::onNewTreeSpeciesChanged,
                        onSelectExistingZone = analysisViewModel::onSelectExistingZone,
                        onNewZoneNameChanged = analysisViewModel::onNewZoneNameChanged,
                        onNewZoneRadiusChanged = analysisViewModel::onNewZoneRadiusChanged,
                        onObservationsChanged = analysisViewModel::onObservationsChanged,
                        onSaveToRoomClick = analysisViewModel::saveCurrentAnalysisToRoom,
                        onSyncNowClick = analysisViewModel::syncPendingDetectionsNow,
                        onHistoryFilterChanged = analysisViewModel::onHistoryFilterChanged,
                        onInspectRecordClick = analysisViewModel::inspectHistoricalRecord,
                        onSimulateCoordinatesClick = analysisViewModel::updateSimulatedCoordinates,
                        onClearLocalDatabaseClick = analysisViewModel::clearAllLocalDatabaseFromProfile,
                        onLogoutClick = authViewModel::logout,
                        onStartWizard = analysisViewModel::startWizard,
                        onSetWizardStep = analysisViewModel::setWizardStep,
                        onRepeatCapture = analysisViewModel::repeatCapture,
                        onUpdateCroppedBitmap = analysisViewModel::updateCroppedBitmap,
                        onRestoreOriginalBitmap = analysisViewModel::restoreOriginalBitmap,
                        onRotateBitmap = analysisViewModel::rotateCurrentBitmap,
                        onMapTreeSelected = analysisViewModel::onMapTreeSelected,
                        onDismissMapBottomSheet = analysisViewModel::dismissMapBottomSheet,
                        onSearchQueryTreeChanged = analysisViewModel::setSearchQueryTree,
                        onDeleteTreeClick = analysisViewModel::deleteTreeSection,
                        onSaveTreeObservationsClick = analysisViewModel::updateTreeObservations,
                        onHomeWorkTabChanged = analysisViewModel::onHomeWorkTabChanged,
                        onOpenZoneDetailClick = analysisViewModel::openZoneDetailSection,
                        onCloseZoneDetailClick = analysisViewModel::closeZoneDetailSection,
                        onStartNewCaptureForZoneClick = analysisViewModel::startNewCaptureForZone,
                        onDeleteZoneClick = analysisViewModel::deleteZoneSection,
                        onSaveZoneObservationsClick = analysisViewModel::updateZoneObservations,
                        onFocusTreeOnMap = analysisViewModel::focusTreeOnMap,
                        onCenterOnUserGps = analysisViewModel::centerOnUserGps
                    )
                } else {
                    LoginAndSessionScreen(
                        uiState = authUiState,
                        onCorreoChanged = authViewModel::onCorreoChanged,
                        onPasswordChanged = authViewModel::onPasswordChanged,
                        onRecoveryTokenChanged = authViewModel::onRecoveryTokenChanged,
                        onNewPasswordChanged = authViewModel::onNewPasswordChanged,
                        onConfirmNewPasswordChanged = authViewModel::onConfirmNewPasswordChanged,
                        onSwitchMode = authViewModel::switchMode,
                        onLoginClick = authViewModel::login,
                        onQuickOfflineSessionClick = authViewModel::startQuickOfflineBrigadistaSession,
                        onRequestRecoveryClick = authViewModel::requestPasswordRecovery,
                        onResetPasswordClick = authViewModel::resetPassword,
                        onLogoutClick = authViewModel::logout,
                        onRefreshInfrastructureClick = authViewModel::verifyInfrastructure,
                        onRunSyntheticVisionClick = authViewModel::runPhase3SyntheticSampleAnalysis,
                        onRunGalleryBitmapClick = authViewModel::runPhase3CustomBitmapAnalysis,
                        onAlphaChanged = authViewModel::onPhase3AlphaChanged,
                        onCategoryFilterSelected = authViewModel::onPhase3CategoryFilterSelected,
                        onPhase4TargetNameChanged = authViewModel::onPhase4TargetNameChanged,
                        onPhase4ObservationsChanged = authViewModel::onPhase4ObservationsChanged,
                        onSaveDetectionToRoomClick = authViewModel::saveCurrentAnalysisToRoom,
                        onSyncWithBackendClick = authViewModel::syncWithBackendNow
                    )
                }
            }
        }
    }
}
}
