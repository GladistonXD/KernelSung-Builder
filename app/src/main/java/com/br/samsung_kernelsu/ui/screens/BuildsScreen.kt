package com.br.samsung_kernelsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.samsung_kernelsu.data.UserSession
import com.br.samsung_kernelsu.data.WorkflowRun
import com.br.samsung_kernelsu.i18n.AppStrings
import com.br.samsung_kernelsu.i18n.LocalStrings
import com.br.samsung_kernelsu.viewmodel.BuildViewModel

private val BuildsAccentBlue   = Color(0xFF3B82F6)
private val BuildsAccentPurple = Color(0xFF8B5CF6)
private val BuildsAccentGreen  = Color(0xFF10B981)
private val BuildsAccentOrange = Color(0xFFF59E0B)
private val BuildsAccentRed    = Color(0xFFEF4444)
private val BuildsAccentGray   = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildsScreen(
    session: UserSession,
    buildViewModel: BuildViewModel,
    onOpenRun: (Long) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current

    val runs by buildViewModel.recentRuns.collectAsStateWithLifecycle()
    val loading by buildViewModel.loadingRuns.collectAsStateWithLifecycle()

    LaunchedEffect(session.login) {
        buildViewModel.loadRecentRuns(session)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📋", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            strings.buildsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.buildsBack)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                loading && runs.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = BuildsAccentPurple)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            strings.buildsLoading,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                runs.isEmpty() -> {
                    EmptyBuildsState(strings)
                }

                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(runs, key = { it.id }) { run ->
                            RunCard(
                                run = run,
                                strings = strings,
                                onClick = { onOpenRun(run.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunCard(
    run: WorkflowRun,
    strings: AppStrings,
    onClick: () -> Unit
) {
    val accent = statusColor(run.status, run.conclusion)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accent)
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon(run.status, run.conclusion),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        "${strings.buildsRunNumber} #${run.runNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(2.dp))

                    // Status
                    Text(
                        statusLabel(run.status, run.conclusion, strings),
                        style = MaterialTheme.typography.bodySmall,
                        color = accent,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        formatDateTime(run.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    "›",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}


@Composable
private fun EmptyBuildsState(strings: AppStrings) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            BuildsAccentBlue.copy(alpha = 0.15f),
                            BuildsAccentPurple.copy(alpha = 0.15f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("📭", fontSize = 44.sp)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            strings.buildsEmpty,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}


private fun statusIcon(status: String, conclusion: String?): ImageVector {
    return when {
        status == "completed" && conclusion == "success" -> Icons.Default.CheckCircle
        status == "completed" && conclusion == "failure" -> Icons.Default.Cancel
        status == "completed" && conclusion == "cancelled" -> Icons.Default.Cancel
        status == "completed" && conclusion == "skipped" -> Icons.Default.RadioButtonUnchecked
        status == "in_progress" -> Icons.Default.PlayArrow
        status == "queued" -> Icons.Default.HourglassEmpty
        else -> Icons.Default.RadioButtonUnchecked
    }
}

private fun statusColor(status: String, conclusion: String?): Color {
    return when {
        status == "completed" && conclusion == "success" -> BuildsAccentGreen
        status == "completed" && conclusion == "failure" -> BuildsAccentRed
        status == "completed" && conclusion == "cancelled" -> BuildsAccentOrange
        status == "in_progress" -> BuildsAccentBlue
        status == "queued" -> BuildsAccentGray
        else -> BuildsAccentGray
    }
}

private fun statusLabel(
    status: String,
    conclusion: String?,
    strings: AppStrings
): String {
    return when {
        status == "completed" && conclusion == "success" -> strings.buildsStatusSuccess
        status == "completed" && conclusion == "failure" -> strings.buildsStatusFailure
        status == "completed" && conclusion == "cancelled" -> strings.buildsStatusCancelled
        status == "completed" && conclusion == "skipped" -> strings.buildsStatusSkipped
        status == "completed" -> strings.buildsStatusCompleted
        status == "in_progress" -> strings.buildsStatusInProgress
        status == "queued" -> strings.buildsStatusQueued
        else -> strings.buildsStatusUnknown
    }
}

private fun formatDateTime(iso: String): String {
    return try {
        val date = iso.substring(0, 10)
        val time = iso.substring(11, 16)
        val parts = date.split("-")
        "${parts[2]}/${parts[1]} $time"
    } catch (_: Exception) {
        iso
    }
}