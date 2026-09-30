package com.br.samsung_kernelsu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.br.samsung_kernelsu.data.*
import com.br.samsung_kernelsu.i18n.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BuildViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GitHubRepository()
    private val settings = SettingsRepository(application)

    private val _buildState = MutableStateFlow<BuildState>(BuildState.Idle)
    val buildState: StateFlow<BuildState> = _buildState

    private val _osrcUploads = MutableStateFlow<List<ReleaseInfo>>(emptyList())
    val osrcUploads: StateFlow<List<ReleaseInfo>> = _osrcUploads

    private val _bootUploads = MutableStateFlow<List<ReleaseInfo>>(emptyList())
    val bootUploads: StateFlow<List<ReleaseInfo>> = _bootUploads

    private val _loadingUploads = MutableStateFlow(false)
    val loadingUploads: StateFlow<Boolean> = _loadingUploads

    private val _recentRuns = MutableStateFlow<List<WorkflowRun>>(emptyList())
    val recentRuns: StateFlow<List<WorkflowRun>> = _recentRuns

    private val _loadingRuns = MutableStateFlow(false)
    val loadingRuns: StateFlow<Boolean> = _loadingRuns

    private val _currentJobs = MutableStateFlow<List<WorkflowJob>>(emptyList())
    val currentJobs: StateFlow<List<WorkflowJob>> = _currentJobs

    private val _currentRun = MutableStateFlow<WorkflowRun?>(null)
    val currentRun: StateFlow<WorkflowRun?> = _currentRun

    private val _currentArtifacts = MutableStateFlow<List<Artifact>>(emptyList())
    val currentArtifacts: StateFlow<List<Artifact>> = _currentArtifacts

    private var monitorJob: Job? = null
    private var detailJob: Job? = null


    fun resumeLastBuildIfAny(session: UserSession): Long {
        val lastRunId = settings.getLastRunId()
        if (lastRunId <= 0) return -1L

        viewModelScope.launch {
            try {
                val run = withContext(Dispatchers.IO) {
                    repository.getWorkflowRunById(
                        session.token, session.login, session.repoName, lastRunId
                    )
                }
                if (run != null && (run.status == "in_progress" || run.status == "queued")) {
                    _buildState.value = BuildState.Running(run.id, run.status)
                    monitorBuild(session, null)
                } else {
                    settings.clearLastRun()
                }
            } catch (_: Exception) {}
        }
        return lastRunId
    }


    fun loadRecentRuns(session: UserSession) {
        viewModelScope.launch {
            _loadingRuns.value = true
            try {
                val runs = withContext(Dispatchers.IO) {
                    repository.listRuns(session.token, session.login, session.repoName, 30)
                }
                _recentRuns.value = runs
            } catch (_: Exception) {
                _recentRuns.value = emptyList()
            } finally {
                _loadingRuns.value = false
            }
        }
    }


    fun watchRun(session: UserSession, runId: Long) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val (run, jobs, artifacts) = withContext(Dispatchers.IO) {
                        val r = repository.getWorkflowRunById(
                            session.token, session.login, session.repoName, runId
                        )
                        val j = repository.getWorkflowJobs(
                            session.token, session.login, session.repoName, runId
                        )
                        val a = if (r?.status == "completed") {
                            repository.getArtifacts(
                                session.token, session.login, session.repoName, runId
                            )
                        } else emptyList()
                        Triple(r, j, a)
                    }

                    _currentRun.value = run
                    _currentJobs.value = jobs
                    _currentArtifacts.value = artifacts

                    if (run?.status == "completed") {
                        settings.clearLastRun()
                        return@launch
                    }
                    delay(5_000)
                } catch (_: Exception) {
                    delay(5_000)
                }
            }
        }
    }

    fun stopWatchingRun() {
        detailJob?.cancel()
        detailJob = null
    }


    fun loadUploads(session: UserSession) {
        viewModelScope.launch {
            _loadingUploads.value = true
            try {
                android.util.Log.d("LOAD_UPLOADS", "▶ Iniciando (user=${session.login})")

                val osrc = withContext(Dispatchers.IO) {
                    repository.listUploads(
                        session.token, session.login, session.repoName, listOf("osrc-")
                    )
                }
                android.util.Log.d("LOAD_UPLOADS", "OSRC: ${osrc.size} releases encontradas")

                val boot = withContext(Dispatchers.IO) {
                    repository.listUploads(
                        session.token, session.login, session.repoName, listOf("boot-")
                    )
                }
                android.util.Log.d("LOAD_UPLOADS", "BOOT: ${boot.size} releases encontradas")

                osrc.forEach { r ->
                    android.util.Log.d("LOAD_UPLOADS", "  OSRC release: tag=${r.tagName}, assets=${r.assets.size}")
                }
                boot.forEach { r ->
                    android.util.Log.d("LOAD_UPLOADS", "  BOOT release: tag=${r.tagName}, assets=${r.assets.size}")
                }

                _osrcUploads.value = osrc
                _bootUploads.value = boot
            } catch (e: Exception) {
                android.util.Log.e("LOAD_UPLOADS", "✗ Erro: ${e.message}", e)
            } finally {
                _loadingUploads.value = false
            }
        }
    }


    fun startBuild(session: UserSession, config: BuildConfig, strings: AppStrings) {
        monitorJob?.cancel()
        viewModelScope.launch {
            try {
                _buildState.value = BuildState.Dispatching
                when (config.mode) {
                    BuildMode.WILDKERNELS -> startWildKernelsBuild(session, config, strings)
                    BuildMode.CUSTOM -> startCustomBuild(session, config, strings)
                }
                delay(5_000)
                monitorBuild(session, strings)
            } catch (e: Exception) {
                _buildState.value = BuildState.Error(e.message ?: strings.vmErrorDispatch)
            }
        }
    }

    private suspend fun startWildKernelsBuild(
        session: UserSession,
        config: BuildConfig,
        strings: AppStrings
    ) {
        try {
            repository.enableActionsInFork(session.token, session.login, session.repoName)
        } catch (_: Exception) {}

        try {
            repository.syncKsunActionYml(
                token = session.token,
                forkOwner = session.login,
                forkRepo = session.repoName,
                forkBranch = session.defaultBranch,
                keepFixStep = config.fixKsun
            )
        } catch (e: Exception) {
            android.util.Log.w("BuildViewModel", "Falha ao sincronizar action.yml: ${e.message}")
        }

        _buildState.value = BuildState.Uploading(strings.vmCreatingSingleDevice)
        repository.createOrUpdateSingleDeviceWorkflow(
            token = session.token,
            owner = session.login,
            repo = session.repoName,
            branch = session.defaultBranch
        )

        delay(10_000)

        val workflowId = repository.getWorkflowId(
            session.token, session.login, session.repoName,
            WorkflowTemplate.SINGLE_DEVICE_WORKFLOW_FILENAME
        ) ?: throw Exception(
            strings.vmWorkflowNotFound.format(WorkflowTemplate.SINGLE_DEVICE_WORKFLOW_FILENAME)
        )

        val inputs = mutableMapOf(
            "device_branch" to config.deviceBranch,
            "device_model" to config.deviceModel,
            "build_type" to config.deviceBuildType,
            "release_type" to config.releaseType,
            "lto" to config.lto,
            "ksun" to config.ksun.toString(),
            "susfs" to config.susfs.toString(),
            "zeromount" to config.zeromount.toString(),
            "nomount" to config.nomount.toString(),
            "bbg" to config.bbg.toString(),
            "unicodefix" to config.unicodefix.toString(),
            "droidspace" to config.droidspace.toString(),
            "ntsync" to config.ntsync.toString(),
            "bbrv3" to config.bbrv3.toString(),
            "cache" to config.cache.toString(),
            "ipv6_nat" to config.ipv6Nat.toString(),
            "optimization" to config.optimization.toString(),
            "ttl" to config.ttl.toString()
        )

        if (config.enableBootRepack) {
            if (config.bootSource == FileSource.REUSE_EXISTING && config.bootReuseUrl.isNotBlank()) {
                inputs["boot_img_url"] = config.bootReuseUrl
            } else if (config.bootSource == FileSource.NEW_UPLOAD && config.bootImagePath.isNotBlank()) {
                val bootFile = File(config.bootImagePath)
                if (bootFile.exists()) {
                    if (bootFile.length() < 6 * 1024 * 1024) {
                        inputs["boot_img_b64"] = android.util.Base64.encodeToString(
                            bootFile.readBytes(), android.util.Base64.NO_WRAP
                        )
                    } else {
                        val url = uploadWithProgress(
                            session = session,
                            file = bootFile,
                            tag = "boot-${System.currentTimeMillis()}",
                            label = "boot.img",
                            strings = strings
                        )
                        inputs["boot_img_url"] = url
                    }
                }
            }
        }

        _buildState.value = BuildState.Dispatching
        repository.dispatchBuild(
            session.token, session.login, session.repoName,
            workflowId, session.defaultBranch, inputs
        )
    }

    private suspend fun startCustomBuild(
        session: UserSession,
        config: BuildConfig,
        strings: AppStrings
    ) {
        val inputs = mutableMapOf(
            "device_model" to config.deviceModel,
            "device_branch" to config.deviceBranch,
            "enable_susfs" to config.susfs.toString(),
            "enable_zeromount" to config.zeromount.toString(),
            "enable_bbg" to config.bbg.toString(),
            "enable_nomount" to config.nomount.toString(),
            "enable_unicodefix" to config.unicodefix.toString(),
            "enable_droidspace" to config.droidspace.toString(),
            "enable_ntsync" to config.ntsync.toString(),
            "enable_bbrv3" to config.bbrv3.toString(),
            "enable_ipv6_nat" to config.ipv6Nat.toString(),
            "enable_optimization" to config.optimization.toString(),
            "enable_ttl" to config.ttl.toString()
        )

        val osrcUrl: String = when (config.osrcSource) {
            FileSource.REUSE_EXISTING -> config.osrcReuseUrl.takeIf { it.isNotBlank() }
                ?: throw Exception(strings.vmErrorNoOsrcSelected)
            FileSource.NEW_UPLOAD -> {
                val osrcFile = File(config.osrcFilePath)
                if (!osrcFile.exists()) throw Exception(strings.vmErrorOsrcNotFound)
                val url = uploadWithProgress(
                    session = session,
                    file = osrcFile,
                    tag = "osrc-${System.currentTimeMillis()}",
                    label = "OSRC (.zip)",
                    strings = strings
                )
                loadUploads(session)
                url
            }
        }
        inputs["osrc_url"] = osrcUrl

        when (config.bootSource) {
            FileSource.REUSE_EXISTING -> {
                if (config.bootReuseUrl.isBlank()) throw Exception(strings.vmErrorNoBootSelected)
                inputs["boot_img_url"] = config.bootReuseUrl
            }
            FileSource.NEW_UPLOAD -> {
                val bootFile = File(config.bootImagePath)
                if (!bootFile.exists()) throw Exception(strings.vmErrorBootNotFound)
                if (bootFile.length() < 6 * 1024 * 1024) {
                    inputs["boot_img_b64"] = android.util.Base64.encodeToString(
                        bootFile.readBytes(), android.util.Base64.NO_WRAP
                    )
                } else {
                    val url = uploadWithProgress(
                        session = session,
                        file = bootFile,
                        tag = "boot-${System.currentTimeMillis()}",
                        label = "boot.img",
                        strings = strings
                    )
                    inputs["boot_img_url"] = url
                    loadUploads(session)
                }
            }
        }

        _buildState.value = BuildState.Uploading(strings.vmPreparingWorkflow)
        try {
            repository.createOrUpdateCustomWorkflow(
                session.token, session.login, session.repoName, session.defaultBranch
            )
        } catch (_: Exception) {}
        try {
            repository.enableActionsInFork(session.token, session.login, session.repoName)
        } catch (_: Exception) {}

        delay(8_000)

        val customId = repository.getWorkflowId(
            session.token, session.login, session.repoName,
            WorkflowTemplate.CUSTOM_WORKFLOW_FILENAME
        ) ?: WorkflowTemplate.CUSTOM_WORKFLOW_FILENAME

        _buildState.value = BuildState.Dispatching
        repository.dispatchBuild(
            session.token, session.login, session.repoName,
            customId, session.defaultBranch, inputs
        )
    }

    // ============================================================
    //       UPLOAD COM PROGRESSO
    // ============================================================

    private suspend fun uploadWithProgress(
        session: UserSession,
        file: File,
        tag: String,
        label: String,
        strings: AppStrings
    ): String {
        val totalBytes = file.length()

        _buildState.value = BuildState.Uploading(
            message = strings.vmUploadingFile.format(label),
            progress = 0f,
            bytesUploaded = 0L,
            bytesTotal = totalBytes,
            fileName = file.name
        )

        return withContext(Dispatchers.IO) {
            repository.uploadBootImageAsReleaseAsset(
                token = session.token,
                owner = session.login,
                repo = session.repoName,
                file = file,
                tag = tag,
                onProgress = { sent, total ->
                    val frac = if (total > 0) sent.toFloat() / total.toFloat() else 0f
                    _buildState.value = BuildState.Uploading(
                        message = strings.vmUploadingFile.format(label),
                        progress = frac.coerceIn(0f, 1f),
                        bytesUploaded = sent,
                        bytesTotal = total,
                        fileName = file.name
                    )
                }
            )
        }
    }

    private suspend fun monitorBuild(session: UserSession, strings: AppStrings?) {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 2160

            while (attempts < maxAttempts && isActive) {
                delay(5_000)
                attempts++

                try {
                    val run = repository.getLatestRun(
                        session.token, session.login, session.repoName
                    )
                    if (run != null) {
                        _buildState.value = BuildState.Running(run.id, run.status)
                        if (run.status == "in_progress" || run.status == "queued") {
                            settings.saveLastRunId(run.id)
                        }
                        if (run.status == "completed") {
                            val artifacts = repository.getArtifacts(
                                session.token, session.login, session.repoName, run.id
                            )
                            _buildState.value = BuildState.Completed(run.id, run.conclusion, artifacts)
                            settings.clearLastRun()
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    _buildState.value = BuildState.Error(
                        e.message ?: strings?.vmErrorMonitoring ?: "Monitoring error"
                    )
                    return@launch
                }
            }
            _buildState.value = BuildState.Error(
                strings?.vmErrorTimeout ?: "Monitoring timeout exceeded."
            )
        }
    }

    fun reset() {
        monitorJob?.cancel()
        monitorJob = null
        _buildState.value = BuildState.Idle
    }
}