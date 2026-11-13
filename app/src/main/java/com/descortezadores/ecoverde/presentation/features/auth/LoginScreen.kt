package com.descortezadores.ecoverde.presentation.features.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.descortezadores.ecoverde.domain.model.DiagnosticCategory
import com.descortezadores.ecoverde.domain.model.SampleType
import com.descortezadores.ecoverde.presentation.features.about.TermsAndConditionsScreen
import com.descortezadores.ecoverde.presentation.theme.EcoVerdeMotion
import com.descortezadores.ecoverde.presentation.theme.GreenPrimary
import com.descortezadores.ecoverde.presentation.theme.StatusHealthyGreen
import com.descortezadores.ecoverde.presentation.theme.StatusInfestedRed
import com.descortezadores.ecoverde.presentation.theme.StatusUncertainAmber

/**
 * Módulo 1: Acceso e Inicio de Sesión
 *
 * Lineamientos de diseño y reestructuración de pantallas:
 * - Logotipo del proyecto ("EcoVerde") con verde bosque primario (#2D6A3E).
 * - Formulario estricto de dos campos: Correo institucional/asignado y Contraseña.
 * - Botón primario de ancho completo: Iniciar sesión.
 * - Enlace secundario discreto: ¿Olvidaste tu contraseña? (conecta con CU-G02 y el flujo de recuperación de un solo uso de 24h, RN-04/05).
 * - Acceso offline directo para brigadistas de campo (Bosque de Chapultepec).
 * - Pie de pantalla con indicador de estado de conectividad ("Modo local listo / Servidor conectado") para anticipar el comportamiento offline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginAndSessionScreen(
    uiState: AuthUiState,
    onCorreoChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onRecoveryTokenChanged: (String) -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmNewPasswordChanged: (String) -> Unit,
    onSwitchMode: (AuthMode) -> Unit,
    onLoginClick: () -> Unit,
    onQuickOfflineSessionClick: () -> Unit,
    onRequestRecoveryClick: () -> Unit,
    onResetPasswordClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onRefreshInfrastructureClick: () -> Unit,
    onRunSyntheticVisionClick: (SampleType) -> Unit = {},
    onRunGalleryBitmapClick: (android.graphics.Bitmap) -> Unit = {},
    onAlphaChanged: (Float) -> Unit = {},
    onCategoryFilterSelected: (DiagnosticCategory?) -> Unit = {},
    onPhase4TargetNameChanged: (String) -> Unit = {},
    onPhase4ObservationsChanged: (String) -> Unit = {},
    onSaveDetectionToRoomClick: () -> Unit = {},
    onSyncWithBackendClick: () -> Unit = {}
) {
    var showTermsScreen by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showTermsScreen,
        transitionSpec = {
            if (targetState) {
                EcoVerdeMotion.modalOpenTransform()
            } else {
                EcoVerdeMotion.modalCloseTransform()
            }
        },
        label = "AuthTermsAnimatedContent"
    ) { isTermsVisible ->
        if (isTermsVisible) {
            TermsAndConditionsScreen(
                onBackClick = { showTermsScreen = false }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Logotipo del proyecto ("EcoVerde") con el verde bosque primario (#2D6A3E)
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forest,
                                contentDescription = "Logotipo EcoVerde",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "EcoVerde",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenPrimary
                            )
                            Text(
                                text = "Detección Fitosanitaria de Plagas Forestales",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Bosque de Chapultepec • Ciudad de México",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Mensajes de error o informativos
                        uiState.errorMessage?.let { msg ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = StatusInfestedRed.copy(alpha = 0.12f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = StatusInfestedRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        color = StatusInfestedRed,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        uiState.infoMessage?.let { msg ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = StatusHealthyGreen.copy(alpha = 0.12f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusHealthyGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        color = StatusHealthyGreen,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Transición animada direccional entre modos de autenticación (Login, Recuperar, Restablecer)
                        AnimatedContent(
                            targetState = uiState.authMode,
                            transitionSpec = {
                                val forward = when {
                                    initialState == AuthMode.LOGIN -> true
                                    targetState == AuthMode.LOGIN -> false
                                    else -> targetState == AuthMode.RESET_PASSWORD
                                }
                                EcoVerdeMotion.sharedAxisX(forward = forward, slideDistance = 90)
                            },
                            label = "AuthModeAnimatedContent",
                            modifier = Modifier.fillMaxWidth()
                        ) { mode ->
                            when (mode) {
                                AuthMode.LOGIN -> LoginFormCleanCard(
                                    correo = uiState.correo,
                                    password = uiState.password,
                                    isLoading = uiState.isLoading,
                                    onCorreoChanged = onCorreoChanged,
                                    onPasswordChanged = onPasswordChanged,
                                    onLoginClick = onLoginClick,
                                    onQuickOfflineSessionClick = onQuickOfflineSessionClick,
                                    onForgotPasswordClick = { onSwitchMode(AuthMode.RECOVER_PASSWORD) },
                                    onOpenTermsClick = { showTermsScreen = true }
                                )

                                AuthMode.RECOVER_PASSWORD -> RecoverPasswordCleanCard(
                                    correo = uiState.correo,
                                    isLoading = uiState.isLoading,
                                    onCorreoChanged = onCorreoChanged,
                                    onRequestRecoveryClick = onRequestRecoveryClick,
                                    onBackToLoginClick = { onSwitchMode(AuthMode.LOGIN) }
                                )

                                AuthMode.RESET_PASSWORD -> ResetPasswordCleanCard(
                                    token = uiState.recoveryToken,
                                    newPassword = uiState.newPassword,
                                    confirmNewPassword = uiState.confirmNewPassword,
                                    isLoading = uiState.isLoading,
                                    onTokenChanged = onRecoveryTokenChanged,
                                    onNewPasswordChanged = onNewPasswordChanged,
                                    onConfirmNewPasswordChanged = onConfirmNewPasswordChanged,
                                    onResetPasswordClick = onResetPasswordClick,
                                    onBackToLoginClick = { onSwitchMode(AuthMode.LOGIN) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pie con indicador de estado de conectividad ("Modo local listo / Servidor conectado")
                    ConnectivityFooterBanner(
                        isBackendReachable = uiState.isBackendReachable,
                        onRefreshClick = onRefreshInfrastructureClick
                    )
                }
            }
        }
    }
}

/**
 * Formulario estricto de dos campos: Correo institucional y Contraseña.
 */
@Composable
private fun LoginFormCleanCard(
    correo: String,
    password: String,
    isLoading: Boolean,
    onCorreoChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onQuickOfflineSessionClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onOpenTermsClick: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Inicio de sesión",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Campo 1: Correo institucional / asignado
            OutlinedTextField(
                value = correo,
                onValueChange = onCorreoChanged,
                label = { Text("Correo institucional o asignado") },
                placeholder = { Text("brigadista@ecoverde.gob.mx") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            var showPassword by remember { mutableStateOf(false) }

            // Campo 2: Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChanged,
                label = { Text("Contraseña") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showPassword = !showPassword },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showPassword,
                    onCheckedChange = { showPassword = it }
                )
                Text(
                    text = "Mostrar contraseña",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botón primario de ancho completo: Iniciar sesión
            Button(
                onClick = onLoginClick,
                enabled = !isLoading,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Enlace secundario discreto: ¿Olvidaste tu contraseña? (CU-G02, RN-04/05)
            TextButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Acceso rápido modo brigadista offline para campo sin cobertura
            OutlinedButton(
                onClick = onQuickOfflineSessionClick,
                enabled = !isLoading,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Acceso Rápido Brigadista Offline",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Enlace a Términos, Privacidad y Acerca de
            TextButton(
                onClick = onOpenTermsClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Términos, Privacidad y Acerca de",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Pantalla de recuperación de contraseña de un solo uso (CU-G02 / RN-04 / RN-05).
 */
@Composable
private fun RecoverPasswordCleanCard(
    correo: String,
    isLoading: Boolean,
    onCorreoChanged: (String) -> Unit,
    onRequestRecoveryClick: () -> Unit,
    onBackToLoginClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Recuperar contraseña",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Se enviará un código de recuperación con vigencia estricta de 24 horas a tu correo institucional registrado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = correo,
                onValueChange = onCorreoChanged,
                label = { Text("Correo institucional") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onRequestRecoveryClick,
                enabled = !isLoading,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Enviar código de recuperación")
            }

            TextButton(
                onClick = onBackToLoginClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("← Volver a inicio de sesión")
            }
        }
    }
}

/**
 * Formulario para ingresar token y nueva contraseña (CU-G03 / RN-05).
 */
@Composable
private fun ResetPasswordCleanCard(
    token: String,
    newPassword: String,
    confirmNewPassword: String,
    isLoading: Boolean,
    onTokenChanged: (String) -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmNewPasswordChanged: (String) -> Unit,
    onResetPasswordClick: () -> Unit,
    onBackToLoginClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Restablecer contraseña",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ingresa el código de 24 horas y tu nueva contraseña (mínimo 8 caracteres, números y caracteres especiales).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = token,
                onValueChange = onTokenChanged,
                label = { Text("Código de 24 horas") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            var showResetPasswords by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChanged,
                label = { Text("Nueva contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (showResetPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmNewPassword,
                onValueChange = onConfirmNewPasswordChanged,
                label = { Text("Confirmar contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (showResetPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showResetPasswords = !showResetPasswords },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showResetPasswords,
                    onCheckedChange = { showResetPasswords = it }
                )
                Text(
                    text = "Mostrar contraseña",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onResetPasswordClick,
                enabled = !isLoading,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Actualizar contraseña")
            }

            TextButton(
                onClick = onBackToLoginClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Cancelar y volver")
            }
        }
    }
}

/**
 * Indicador de pie de conectividad: "Modo local listo / Servidor conectado".
 */
@Composable
private fun ConnectivityFooterBanner(
    isBackendReachable: Boolean,
    onRefreshClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isBackendReachable) {
            StatusHealthyGreen.copy(alpha = 0.12f)
        } else {
            StatusUncertainAmber.copy(alpha = 0.15f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isBackendReachable) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (isBackendReachable) StatusHealthyGreen else Color(0xFFE65100),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isBackendReachable) {
                        "Servidor conectado • Modo online y sincronización listos"
                    } else {
                        "Modo local listo • Operación 100% offline en bosque"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isBackendReachable) StatusHealthyGreen else Color(0xFFE65100)
                )
            }
            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Verificar conexión",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
