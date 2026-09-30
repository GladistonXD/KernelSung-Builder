package com.br.samsung_kernelsu.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.samsung_kernelsu.data.Artifact
import com.br.samsung_kernelsu.data.BuildState
import com.br.samsung_kernelsu.i18n.AppStrings
import com.br.samsung_kernelsu.i18n.LocalStrings
import com.br.samsung_kernelsu.viewmodel.BuildViewModel

private val BuildAccentBlue   = Color(0xFF3B82F6)
private val BuildAccentPurple = Color(0xFF8B5CF6)
private val BuildAccentGreen  = Color(0xFF10B981)
private val BuildAccentOrange = Color(0xFFF59E0B)
private val BuildAccentRed    = Color(0xFFEF4444)

@Composable
fun BuildScreen(
    buildViewModel: BuildViewModel,
    onBack: () -> Unit,
    onOpenRun: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val buildState by buildViewModel.buildState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BuildHero(buildState, strings)

        Spacer(Modifier.height(24.dp))

        when (val state = buildState) {
            is BuildState.Idle -> {
                InfoCard(
                    emoji = "💤",
                    title = strings.buildIdleCard,
                    subtitle = strings.buildIdleCardSubtitle
                )
            }

            is BuildState.Dispatching -> {
                LoadingCard(
                    emoji = "🚀",
                    title = strings.buildDispatchingCard,
                    subtitle = strings.buildDispatchingCardSubtitle,
                    accent = BuildAccentBlue
                )
            }

            is BuildState.Uploading -> {
                UploadingCard(state, strings)
            }

            is BuildState.Running -> {
                RunningCard(state, strings, onOpenRun)
            }

            is BuildState.Completed -> {
                CompletedCard(state, strings, onOpenRun)
            }

            is BuildState.Error -> {
                ErrorCard(state.message, strings)
            }
        }

        Spacer(Modifier.height(32.dp))

        OutlinedButton(
            onClick = {
                buildViewModel.reset()
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                strings.back,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BuildHero(state: BuildState, strings: AppStrings) {
    val (emoji, gradient) = when (state) {
        is BuildState.Idle -> "💤" to listOf(BuildAccentBlue, BuildAccentPurple)
        is BuildState.Dispatching -> "🚀" to listOf(BuildAccentBlue, BuildAccentPurple)
        is BuildState.Uploading -> "📤" to listOf(BuildAccentBlue, BuildAccentPurple)
        is BuildState.Running -> "🔨" to listOf(BuildAccentOrange, BuildAccentRed)
        is BuildState.Completed -> {
            if (state.conclusion == "success") "✅" to listOf(BuildAccentGreen, BuildAccentBlue)
            else "❌" to listOf(BuildAccentRed, BuildAccentOrange)
        }
        is BuildState.Error -> "⚠️" to listOf(BuildAccentRed, BuildAccentOrange)
    }

    val title = when (state) {
        is BuildState.Idle -> strings.buildIdleTitle
        is BuildState.Dispatching -> strings.buildDispatchingTitle
        is BuildState.Uploading -> strings.buildUploadingTitle
        is BuildState.Running -> strings.buildRunningTitle
        is BuildState.Completed -> if (state.conclusion == "success") strings.buildCompletedTitle else strings.buildFailedTitle
        is BuildState.Error -> strings.buildErrorTitle
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 48.sp)
        }

        Spacer(Modifier.height(14.dp))

        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
private fun UploadingCard(state: BuildState.Uploading, strings: AppStrings) {
    val progress = state.progress ?: 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200),
        label = "uploadProgress"
    )

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
                    .background(Brush.horizontalGradient(listOf(BuildAccentBlue, BuildAccentPurple)))
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(180.dp),
                        color = BuildAccentBlue.copy(alpha = 0.12f),
                        strokeWidth = 14.dp
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(180.dp),
                        color = BuildAccentBlue,
                        strokeWidth = 14.dp,
                        trackColor = Color.Transparent
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = BuildAccentBlue
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            formatBytes(state.bytesUploaded) + " / " + formatBytes(state.bytesTotal),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    state.message.ifBlank { "Enviando arquivo..." },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (state.fileName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        state.fileName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BuildAccentBlue.copy(alpha = 0.12f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ℹ️", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        strings.buildUploadHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ============================================================
//              CARD DE RUNNING
// ============================================================

@Composable
private fun RunningCard(
    state: BuildState.Running,
    strings: AppStrings,
    onOpenRun: (Long) -> Unit
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
                    .background(Brush.horizontalGradient(listOf(BuildAccentOrange, BuildAccentRed)))
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = BuildAccentOrange
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        strings.buildRunningCard,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(
                        label = strings.buildStatusLabel,
                        value = statusLabel(state.status, strings),
                        accent = BuildAccentOrange,
                        modifier = Modifier.weight(1.4f)
                    )
                    StatusChip(
                        label = strings.buildRunIdLabel,
                        value = state.runId.toString(),
                        accent = BuildAccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { onOpenRun(state.runId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BuildAccentOrange,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        strings.buildViewProgress,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    strings.buildCloseAppHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CompletedCard(
    state: BuildState.Completed,
    strings: AppStrings,
    onOpenRun: (Long) -> Unit
) {
    val isSuccess = state.conclusion == "success"
    val accent = if (isSuccess) BuildAccentGreen else BuildAccentRed
    val title = if (isSuccess) strings.buildSuccessTitle else strings.buildFailureTitle
    val subtitle = if (isSuccess) strings.buildSuccessSubtitle
    else strings.buildFailurePrefix.format(state.conclusion ?: "—")

    Column(Modifier.fillMaxWidth()) {
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
                        .background(
                            Brush.horizontalGradient(
                                if (isSuccess) listOf(BuildAccentGreen, BuildAccentBlue)
                                else listOf(BuildAccentRed, BuildAccentOrange)
                            )
                        )
                )

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (state.artifacts.isNotEmpty()) {
            Text(
                strings.buildArtifactsTitle.format(state.artifacts.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            state.artifacts.forEach { artifact ->
                ArtifactCard(artifact)
                Spacer(Modifier.height(8.dp))
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📭", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        strings.buildNoArtifacts,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onOpenRun(state.runId) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Color.White
            )
        ) {
            Text(
                strings.buildViewDetails,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ArtifactCard(artifact: Artifact) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BuildAccentGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📦", fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    artifact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "ID: ${artifact.id} • ${formatBytes(artifact.sizeInBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun ErrorCard(message: String, strings: AppStrings) {
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
                    .background(Brush.horizontalGradient(listOf(BuildAccentRed, BuildAccentOrange)))
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = BuildAccentRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        strings.buildErrorCardTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BuildAccentRed
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@Composable
private fun InfoCard(emoji: String, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 40.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingCard(
    emoji: String,
    title: String,
    subtitle: String,
    accent: Color
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
                    .background(Brush.horizontalGradient(listOf(accent, BuildAccentPurple)))
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(emoji, fontSize = 40.sp)
                Spacer(Modifier.height(14.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(
                    color = accent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = 2,                        // ← MUDOU
            softWrap = true,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}


private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}

private fun statusLabel(status: String, strings: AppStrings): String {
    return when (status) {
        "queued" -> strings.buildsStatusQueued
        "in_progress" -> strings.buildsStatusInProgress
        "completed" -> strings.buildsStatusCompleted
        "waiting", "requested", "pending" -> strings.buildsStatusQueued
        else -> status
    }
}