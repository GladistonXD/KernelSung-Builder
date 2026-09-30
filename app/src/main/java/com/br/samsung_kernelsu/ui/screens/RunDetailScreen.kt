package com.br.samsung_kernelsu.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.samsung_kernelsu.data.UserSession
import com.br.samsung_kernelsu.data.WorkflowJob
import com.br.samsung_kernelsu.data.WorkflowStep
import com.br.samsung_kernelsu.i18n.AppStrings
import com.br.samsung_kernelsu.i18n.LocalStrings
import com.br.samsung_kernelsu.viewmodel.BuildViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailScreen(
    session: UserSession,
    runId: Long,
    buildViewModel: BuildViewModel,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val run by buildViewModel.currentRun.collectAsStateWithLifecycle()
    val jobs by buildViewModel.currentJobs.collectAsStateWithLifecycle()
    val artifacts by buildViewModel.currentArtifacts.collectAsStateWithLifecycle()

    LaunchedEffect(runId) {
        buildViewModel.watchRun(session, runId)
    }
    DisposableEffect(Unit) {
        onDispose { buildViewModel.stopWatchingRun() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(strings.runDetailTitle.format(run?.runNumber ?: runId))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val url = run?.htmlUrl
                        if (url != null) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }) {
                        Text("↗", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            run?.let { r ->
                Card(Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            statusLabel(r.status, r.conclusion, strings),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor(r.status, r.conclusion)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            strings.runDetailStartedAt.format(
                                r.createdAt.replace("T", " ").take(19)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (r.updatedAt != null) {
                            Text(
                                strings.runDetailUpdatedAt.format(
                                    r.updatedAt.replace("T", " ").take(19)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isRunActive(r.status)) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    strings.runDetailUpdatingEvery5s,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (jobs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(jobs, key = { it.id }) { job ->
                        JobCard(job, strings)
                    }

                    if (artifacts.isNotEmpty()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        strings.runDetailArtifactsReady,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        strings.runDetailTapToDownload,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))

                                    artifacts.forEach { art ->
                                        val downloadUrl = buildArtifactDownloadUrl(
                                            runHtmlUrl = run?.htmlUrl,
                                            runId = runId,
                                            artifactId = art.id
                                        )

                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = downloadUrl != null) {
                                                    if (downloadUrl != null) {
                                                        context.startActivity(
                                                            Intent(
                                                                Intent.ACTION_VIEW,
                                                                Uri.parse(downloadUrl)
                                                            )
                                                        )
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = strings.runDetailDownload,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    art.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    strings.runDetailSizeTapToDownload.format(
                                                        art.sizeInBytes / 1024 / 1024
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val url = run?.htmlUrl
                                            if (url != null) {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(strings.runDetailOpenOnGithub)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Retorna true se o run ainda está "ativo" (na fila, rodando, ou aguardando).
 * Quando o status é "completed" (ou desconhecido), consideramos parado.
 */
private fun isRunActive(status: String?): Boolean {
    return when (status) {
        "queued", "in_progress", "requested", "waiting", "pending" -> true
        else -> false
    }
}

/**
 * Monta a URL de download direto do artefato no site do GitHub.
 */
private fun buildArtifactDownloadUrl(
    runHtmlUrl: String?,
    runId: Long,
    artifactId: Long
): String? {
    val base = runHtmlUrl?.substringBefore("/actions/runs/") ?: return null
    if (base.isBlank()) return null
    return "$base/actions/runs/$runId/artifacts/$artifactId"
}

@Composable
private fun JobCard(job: WorkflowJob, strings: AppStrings) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon(job.status, job.conclusion),
                    contentDescription = null,
                    tint = statusColor(job.status, job.conclusion),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    job.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    statusLabel(job.status, job.conclusion, strings),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor(job.status, job.conclusion)
                )
            }
            Spacer(Modifier.height(8.dp))
            job.steps.forEach { step -> StepRow(step) }
        }
    }
}

@Composable
private fun StepRow(step: WorkflowStep) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = statusIcon(step.status, step.conclusion),
            contentDescription = null,
            tint = statusColor(step.status, step.conclusion),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            step.name,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        if (step.status == "in_progress") {
            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
        }
    }
}

private fun statusIcon(status: String, conclusion: String?): ImageVector {
    return when {
        status == "completed" && conclusion == "success" -> Icons.Default.CheckCircle
        status == "completed" && conclusion == "failure" -> Icons.Default.Cancel
        status == "completed" && conclusion == "skipped" -> Icons.Default.RadioButtonUnchecked
        status == "completed" && conclusion == "cancelled" -> Icons.Default.Cancel
        status == "in_progress" -> Icons.Default.PlayArrow
        status == "queued" -> Icons.Default.HourglassEmpty
        else -> Icons.Default.RadioButtonUnchecked
    }
}

private fun statusColor(status: String, conclusion: String?): Color {
    return when {
        status == "completed" && conclusion == "success" -> Color(0xFF4CAF50)
        status == "completed" && conclusion == "failure" -> Color(0xFFF44336)
        status == "completed" && conclusion == "cancelled" -> Color(0xFFFF9800)
        status == "in_progress" -> Color(0xFF2196F3)
        status == "queued" -> Color(0xFF9E9E9E)
        else -> Color.Gray
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
        status == "in_progress" -> strings.buildsStatusInProgress
        status == "queued" -> strings.buildsStatusQueued
        status == "completed" -> strings.buildsStatusCompleted
        else -> strings.buildsStatusUnknown
    }
}