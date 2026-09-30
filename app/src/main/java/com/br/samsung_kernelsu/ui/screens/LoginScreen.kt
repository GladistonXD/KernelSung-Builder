package com.br.samsung_kernelsu.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.samsung_kernelsu.data.AuthState
import com.br.samsung_kernelsu.data.UserSession
import com.br.samsung_kernelsu.i18n.AppLanguage
import com.br.samsung_kernelsu.i18n.AppStrings
import com.br.samsung_kernelsu.i18n.LocalLanguage
import com.br.samsung_kernelsu.i18n.LocalStrings
import com.br.samsung_kernelsu.ui.components.LanguageSwitcher
import com.br.samsung_kernelsu.viewmodel.AuthViewModel

private val LoginAccentBlue   = Color(0xFF3B82F6)
private val LoginAccentPurple = Color(0xFF8B5CF6)
private val LoginAccentGreen  = Color(0xFF10B981)

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (UserSession) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    val strings = LocalStrings.current
    val currentLanguage = LocalLanguage.current

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val savedClientId by authViewModel.savedClientId.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var clientId by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(savedClientId) {
        if (savedClientId.isNotBlank() && clientId.isBlank()) {
            clientId = savedClientId
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess((authState as AuthState.Success).session)
        }
    }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            LoginHero(strings)

            Spacer(Modifier.height(32.dp))

            when (val state = authState) {
                is AuthState.Idle,
                is AuthState.Error -> {
                    LoginFormCard(
                        strings = strings,
                        clientId = clientId,
                        onClientIdChange = { clientId = it },
                        savedClientId = savedClientId,
                        errorMessage = (state as? AuthState.Error)?.message,
                        onStartLogin = { authViewModel.startDeviceFlow(clientId, "") },
                        onClearSaved = { authViewModel.clearSavedClientId() }
                    )
                }

                is AuthState.RequestingCode -> {
                    StatusCard(
                        emoji = "🔄",
                        title = strings.loginRequestingCode,
                        subtitle = strings.loginConnectingGithub,
                        showProgress = true
                    )
                }

                is AuthState.DeviceCodeReady -> {
                    DeviceCodeCard(
                        strings = strings,
                        userCode = state.userCode,
                        verificationUri = state.verificationUri,
                        attempt = 0,
                        copied = copied,
                        onCopyCode = {
                            clipboard.setText(AnnotatedString(state.userCode))
                            copied = true
                        },
                        onOpenBrowser = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.verificationUri))
                            context.startActivity(intent)
                        }
                    )
                }

                is AuthState.Polling -> {
                    DeviceCodeCard(
                        strings = strings,
                        userCode = state.userCode,
                        verificationUri = state.verificationUri,
                        attempt = state.attempt,
                        copied = copied,
                        onCopyCode = {
                            clipboard.setText(AnnotatedString(state.userCode))
                            copied = true
                        },
                        onOpenBrowser = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.verificationUri))
                            context.startActivity(intent)
                        }
                    )
                }

                is AuthState.ProvisioningStep -> {
                    StatusCard(
                        emoji = "⚙️",
                        title = strings.loginPreparingEnv,
                        subtitle = state.message,
                        showProgress = true
                    )
                }

                is AuthState.Success -> {
                    StatusCard(
                        emoji = "✅",
                        title = strings.loginAllReady,
                        subtitle = strings.loginRedirecting,
                        showProgress = true
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // Botão de idioma no canto superior direito
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            LanguageSwitcher(
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange
            )
        }
    }
}

@Composable
private fun LoginHero(strings: AppStrings) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(LoginAccentBlue, LoginAccentPurple))),
            contentAlignment = Alignment.Center
        ) {
            Text("🐧", fontSize = 44.sp)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            strings.loginAppTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            strings.loginAppSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoginFormCard(
    strings: AppStrings,
    clientId: String,
    onClientIdChange: (String) -> Unit,
    savedClientId: String,
    errorMessage: String?,
    onStartLogin: () -> Unit,
    onClearSaved: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Brush.horizontalGradient(listOf(LoginAccentBlue, LoginAccentPurple)))
            )

            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LoginAccentBlue.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔑", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.loginAuthSectionTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            strings.loginAuthSectionSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (!errorMessage.isNullOrBlank()) {
                    ErrorBanner(errorMessage)
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = clientId,
                    onValueChange = onClientIdChange,
                    label = { Text(strings.loginClientIdLabel) },
                    placeholder = { Text(strings.loginClientIdPlaceholder) },
                    leadingIcon = { Text("🔐") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Text(
                            strings.loginClientIdHelper,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onStartLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = clientId.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoginAccentBlue,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        strings.loginButton,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (savedClientId.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onClearSaved,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            "🗑️  ${strings.loginClearSaved}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCodeCard(
    strings: AppStrings,
    userCode: String,
    verificationUri: String,
    attempt: Int,
    copied: Boolean,
    onCopyCode: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Brush.horizontalGradient(listOf(LoginAccentBlue, LoginAccentPurple)))
            )

            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StepHeader(number = 1, title = strings.loginStep1Title)
                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LoginAccentBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌐", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        verificationUri,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LoginAccentBlue,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = onOpenBrowser,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoginAccentBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.loginOpenBrowser, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(20.dp))

                StepHeader(number = 2, title = strings.loginStep2Title)
                Spacer(Modifier.height(14.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    LoginAccentPurple.copy(alpha = 0.15f),
                                    LoginAccentBlue.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(1.dp, LoginAccentPurple.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userCode,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp
                        ),
                        color = LoginAccentPurple,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(12.dp))

                val copyColor by animateColorAsState(
                    targetValue = if (copied) LoginAccentGreen else LoginAccentPurple,
                    animationSpec = tween(250),
                    label = "copyColor"
                )

                OutlinedButton(
                    onClick = onCopyCode,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = copyColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, copyColor.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (copied) "✅  ${strings.loginCodeCopied}" else strings.loginCopyCode,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(20.dp))

                StepHeader(number = 3, title = strings.loginStep3Title)
                Spacer(Modifier.height(14.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LoginAccentBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LoginAccentBlue
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.loginVerifyingAuth,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (attempt > 0) {
                            Text(
                                "${strings.loginAttempt} $attempt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(number: Int, title: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(LoginAccentPurple.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = LoginAccentPurple
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusCard(
    emoji: String,
    title: String,
    subtitle: String,
    showProgress: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Brush.horizontalGradient(listOf(LoginAccentBlue, LoginAccentPurple)))
            )

            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(LoginAccentPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 26.sp)
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (showProgress) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(
                        color = LoginAccentPurple,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚠️", fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}