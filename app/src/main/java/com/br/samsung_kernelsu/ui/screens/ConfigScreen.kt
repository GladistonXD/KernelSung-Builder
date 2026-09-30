package com.br.samsung_kernelsu.ui.screens

import androidx.compose.material3.MenuAnchorType
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.samsung_kernelsu.data.*
import com.br.samsung_kernelsu.i18n.AppLanguage
import com.br.samsung_kernelsu.i18n.AppStrings
import com.br.samsung_kernelsu.i18n.LocalLanguage
import com.br.samsung_kernelsu.i18n.LocalStrings
import com.br.samsung_kernelsu.ui.components.LanguageSwitcher
import com.br.samsung_kernelsu.util.DeviceDetector
import com.br.samsung_kernelsu.util.Lz4Utils
import com.br.samsung_kernelsu.viewmodel.BuildViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val AccentGreen   = Color(0xFF10B981)
private val AccentBlue    = Color(0xFF3B82F6)
private val AccentPurple  = Color(0xFF8B5CF6)
private val AccentOrange  = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    session: UserSession,
    buildViewModel: BuildViewModel,
    onStartBuild: (UserSession, BuildConfig, AppStrings) -> Unit,
    onOpenBuilds: () -> Unit,
    onLogout: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val strings = LocalStrings.current
    val currentLanguage = LocalLanguage.current

    LaunchedEffect(session.login) {
        buildViewModel.loadUploads(session)
    }

    val osrcUploads by buildViewModel.osrcUploads.collectAsStateWithLifecycle()
    val bootUploads by buildViewModel.bootUploads.collectAsStateWithLifecycle()
    val loadingUploads by buildViewModel.loadingUploads.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(BuildMode.WILDKERNELS) }

    val autoDetected = remember { DeviceDetector.detect() }
    var deviceBranch by remember { mutableStateOf(autoDetected?.branch ?: DeviceList.default.branch) }
    var deviceBuildType by remember { mutableStateOf(autoDetected?.buildType ?: DeviceList.default.buildType) }
    var deviceWasAutoDetected by remember { mutableStateOf(autoDetected != null) }

    val releaseType = "Actions"
    val lto = "default"

    var deviceModel by remember {
        mutableStateOf(
            DeviceDetector.getCurrentModel().ifBlank {
                DeviceDetector.getModelFromDetectedBranch()
                    ?: deviceBranch.substringBefore("-")
            }
        )
    }

    // ---------- WildKernels: Repack ----------
    var enableBootRepack by remember { mutableStateOf(false) }

    // ---------- Custom: OSRC ----------
    var osrcSource by remember { mutableStateOf(FileSource.NEW_UPLOAD) }
    var osrcFilePath by remember { mutableStateOf<String?>(null) }
    var osrcReuseUrl by remember { mutableStateOf("") }
    var isCopyingOsrc by remember { mutableStateOf(false) }
    var osrcError by remember { mutableStateOf<String?>(null) }

    // ---------- boot.img (compartilhado) ----------
    var bootSource by remember { mutableStateOf(FileSource.NEW_UPLOAD) }
    var bootImgPath by remember { mutableStateOf<String?>(null) }
    var bootReuseUrl by remember { mutableStateOf("") }
    var isConvertingBoot by remember { mutableStateOf(false) }
    var bootMessage by remember { mutableStateOf<String?>(null) }

    // ---------- Features ----------
    var ksun by remember { mutableStateOf(true) }
    var susfs by remember { mutableStateOf(true) }
    var zeromount by remember { mutableStateOf(false) }
    var nomount by remember { mutableStateOf(true) }
    var bbg by remember { mutableStateOf(true) }
    var unicodefix by remember { mutableStateOf(true) }
    var droidspace by remember { mutableStateOf(true) }
    var ntsync by remember { mutableStateOf(true) }
    var bbrv3 by remember { mutableStateOf(true) }
    var cache by remember { mutableStateOf(true) }
    var ipv6Nat by remember { mutableStateOf(true) }
    var optimization by remember { mutableStateOf(true) }
    var ttl by remember { mutableStateOf(true) }
    var fixKsun by remember { mutableStateOf(false) }

    var featuresExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(mode) {
        featuresExpanded = false
    }

    LaunchedEffect(osrcUploads, osrcSource) {
        if (osrcSource == FileSource.REUSE_EXISTING &&
            osrcReuseUrl.isBlank() &&
            osrcUploads.isNotEmpty()) {
            osrcUploads.firstOrNull()?.assets?.firstOrNull()?.let {
                osrcReuseUrl = it.browserDownloadUrl
            }
        }
    }

    LaunchedEffect(bootUploads, bootSource, enableBootRepack, mode) {
        val needBoot = (mode == BuildMode.CUSTOM) ||
                (mode == BuildMode.WILDKERNELS && enableBootRepack)
        if (needBoot &&
            bootSource == FileSource.REUSE_EXISTING &&
            bootReuseUrl.isBlank() &&
            bootUploads.isNotEmpty()) {
            bootUploads.firstOrNull()?.assets?.firstOrNull()?.let {
                bootReuseUrl = it.browserDownloadUrl
            }
        }
    }

    // ============================================================
    //                LAUNCHERS
    // ============================================================

    val pickOsrcLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isCopyingOsrc = true; osrcError = null
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Não foi possível abrir o arquivo")
                val tempFile = File(context.cacheDir, "osrc_selected.zip")
                tempFile.outputStream().use { out -> inputStream.copyTo(out) }
                inputStream.close()
                osrcFilePath = tempFile.absolutePath
            } catch (e: Exception) {
                osrcError = "Erro: ${e.message}"
            } finally {
                isCopyingOsrc = false
            }
        }
    }

    val pickBootLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isConvertingBoot = true
            bootMessage = null
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Não foi possível abrir o arquivo")

                val tmpRaw = File(context.cacheDir, "boot_selected.raw")
                tmpRaw.outputStream().use { out -> inputStream.copyTo(out) }
                inputStream.close()

                val isLz4 = withContext(Dispatchers.IO) {
                    Lz4Utils.isLz4File(tmpRaw)
                }

                val outputImg = File(context.cacheDir, "boot_selected.img")

                val ok = withContext(Dispatchers.IO) {
                    if (isLz4) {
                        Lz4Utils.decompress(tmpRaw, outputImg)
                    } else {
                        Lz4Utils.copy(tmpRaw, outputImg)
                    }
                }

                tmpRaw.delete()

                if (ok && outputImg.exists() && outputImg.length() > 0) {
                    bootImgPath = outputImg.absolutePath
                    val mb = outputImg.length() / 1024 / 1024
                    val origem = if (isLz4) "LZ4 descomprimido" else "IMG direto"
                    bootMessage = "✅ $origem: ${outputImg.name} ($mb MB)"
                } else {
                    bootMessage = "❌ Falha ao processar o arquivo"
                }
            } catch (e: Exception) {
                bootMessage = "❌ Erro: ${e.message}"
            } finally {
                isConvertingBoot = false
            }
        }
    }

    val features = listOf(
        FeatureToggle("🔒", strings.featKsun, ksun) { ksun = it },
        FeatureToggle("🛡️", strings.featSusfs, susfs) { susfs = it },
        FeatureToggle("🚫", strings.featNomount, nomount) { nomount = it },
        FeatureToggle("🔤", strings.featUnicodefix, unicodefix) { unicodefix = it },
        FeatureToggle("📡", strings.featBbg, bbg) { bbg = it },
        FeatureToggle("💠", strings.featZeromount, zeromount) { zeromount = it },
        FeatureToggle("🎯", strings.featNtsync, ntsync) { ntsync = it },
        FeatureToggle("⚡", strings.featBbrv3, bbrv3) { bbrv3 = it },
        FeatureToggle("🚀", strings.featOptimization, optimization) { optimization = it },
        FeatureToggle("💾", strings.featCache, cache) { cache = it },
        FeatureToggle("🐳", strings.featDroidspace, droidspace) { droidspace = it },
        FeatureToggle("🌐", strings.featIpv6Nat, ipv6Nat) { ipv6Nat = it },
        FeatureToggle("⏱️", strings.featTtl, ttl) { ttl = it },
        FeatureToggle("🩹", strings.featFixKsun, fixKsun) { fixKsun = it }
    )
    val enabledCount = features.count { it.checked }

    // ============================================================
    //                       UI
    // ============================================================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        SessionHeaderCard(
            session = session,
            strings = strings,
            currentLanguage = currentLanguage,
            onLanguageChange = onLanguageChange,
            onLogout = onLogout
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onOpenBuilds,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 0.dp
            )
        ) {
            Text("📋", fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                strings.configViewBuild,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(24.dp))

        SectionHeader(
            icon = "⚙️",
            title = strings.configBuildModeTitle,
            subtitle = strings.configBuildModeSubtitle
        )

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeCard(
                modifier = Modifier.weight(1f),
                emoji = "⚡",
                title = strings.configWildKernelsTitle,
                description = strings.configWildKernelsDesc,
                selected = mode == BuildMode.WILDKERNELS,
                accent = AccentBlue,
                onClick = { mode = BuildMode.WILDKERNELS }
            )
            /***ModeCard(
                modifier = Modifier.weight(1f),
                emoji = "🔧",
                title = strings.configCustomTitle,
                description = strings.configCustomDesc,
                selected = mode == BuildMode.CUSTOM,
                accent = AccentGreen,
                onClick = { mode = BuildMode.CUSTOM }
            )***/
        }

        Spacer(Modifier.height(24.dp))

        when (mode) {
            BuildMode.WILDKERNELS -> {
                SectionHeader(
                    icon = "📱",
                    title = strings.configDeviceSectionTitle,
                    subtitle = strings.configDeviceSectionSubtitle
                )
                Spacer(Modifier.height(12.dp))

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        DeviceSelector(
                            devices = DeviceList.devices,
                            selected = deviceBranch,
                            refreshing = false,
                            strings = strings,
                            onSelected = { device ->
                                deviceBranch = device.branch
                                deviceBuildType = device.buildType
                                deviceWasAutoDetected = false
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        if (deviceWasAutoDetected && autoDetected != null) {
                            AutoDetectedBadge(strings = strings)
                        } else {
                            TextButton(
                                onClick = {
                                    autoDetected?.let {
                                        deviceBranch = it.branch
                                        deviceBuildType = it.buildType
                                        deviceWasAutoDetected = true
                                    }
                                }
                            ) {
                                Text(
                                    "🔄  ${strings.configUseDetectedDevice}",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // ============ Repack do boot (Odin) ============
                Spacer(Modifier.height(24.dp))

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { enableBootRepack = !enableBootRepack }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (enableBootRepack) AccentOrange.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📦", fontSize = 18.sp)
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            strings.wildRepackEnable,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Switch(
                            checked = enableBootRepack,
                            onCheckedChange = { enableBootRepack = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentOrange,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                AnimatedVisibility(
                    visible = enableBootRepack,
                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(tween(200))
                ) {
                    Column {
                        Spacer(Modifier.height(16.dp))

                        FileSourceCard(
                            uploads = bootUploads,
                            source = bootSource,
                            onSourceChange = { newSource ->
                                bootSource = newSource
                                if (newSource == FileSource.REUSE_EXISTING && bootReuseUrl.isBlank()) {
                                    bootUploads.firstOrNull()?.assets?.firstOrNull()?.let {
                                        bootReuseUrl = it.browserDownloadUrl
                                    }
                                }
                            },
                            selectedUrl = bootReuseUrl,
                            onUrlSelect = { bootReuseUrl = it },
                            loadingUploads = loadingUploads,
                            isNewFile = bootImgPath != null,
                            isProcessing = isConvertingBoot,
                            fileName = bootImgPath?.let { File(it).name },
                            errorMessage = bootMessage,
                            processingMessage = strings.configConvertingLz4,
                            onPickFile = { pickBootLauncher.launch("*/*") },
                            onReloadUploads = { buildViewModel.loadUploads(session) },
                            accent = AccentOrange,
                            strings = strings
                        )
                    }
                }
            }

            BuildMode.CUSTOM -> {
                SectionHeader(
                    icon = "📝",
                    title = strings.configConfigSectionTitle,
                    subtitle = strings.configConfigSectionSubtitle
                )
                Spacer(Modifier.height(12.dp))

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = deviceModel,
                            onValueChange = { deviceModel = it },
                            label = { Text(strings.configDeviceModelLabel) },
                            leadingIcon = { Text("📱") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                SectionHeader(
                    icon = "📦",
                    title = strings.configOsrcSectionTitle,
                    subtitle = strings.configOsrcSectionSubtitle
                )
                Spacer(Modifier.height(12.dp))

                FileSourceCard(
                    uploads = osrcUploads,
                    source = osrcSource,
                    onSourceChange = { newSource ->
                        osrcSource = newSource
                        if (newSource == FileSource.REUSE_EXISTING && osrcReuseUrl.isBlank()) {
                            osrcUploads.firstOrNull()?.assets?.firstOrNull()?.let {
                                osrcReuseUrl = it.browserDownloadUrl
                            }
                        }
                    },
                    selectedUrl = osrcReuseUrl,
                    onUrlSelect = { osrcReuseUrl = it },
                    loadingUploads = loadingUploads,
                    isNewFile = osrcFilePath != null,
                    isProcessing = isCopyingOsrc,
                    fileName = osrcFilePath?.let { File(it).name },
                    errorMessage = osrcError,
                    processingMessage = strings.configCopyingOsrc,
                    onPickFile = { pickOsrcLauncher.launch("*/*") },
                    onReloadUploads = { buildViewModel.loadUploads(session) },
                    accent = AccentGreen,
                    strings = strings
                )

                Spacer(Modifier.height(20.dp))

                SectionHeader(
                    icon = "💾",
                    title = strings.configBootSectionTitle,
                    subtitle = strings.configBootSectionSubtitle
                )
                Spacer(Modifier.height(12.dp))

                FileSourceCard(
                    uploads = bootUploads,
                    source = bootSource,
                    onSourceChange = { newSource ->
                        bootSource = newSource
                        if (newSource == FileSource.REUSE_EXISTING && bootReuseUrl.isBlank()) {
                            bootUploads.firstOrNull()?.assets?.firstOrNull()?.let {
                                bootReuseUrl = it.browserDownloadUrl
                            }
                        }
                    },
                    selectedUrl = bootReuseUrl,
                    onUrlSelect = { bootReuseUrl = it },
                    loadingUploads = loadingUploads,
                    isNewFile = bootImgPath != null,
                    isProcessing = isConvertingBoot,
                    fileName = bootImgPath?.let { File(it).name },
                    errorMessage = bootMessage,
                    processingMessage = strings.configConvertingLz4,
                    onPickFile = { pickBootLauncher.launch("*/*") },
                    onReloadUploads = { buildViewModel.loadUploads(session) },
                    accent = AccentBlue,
                    strings = strings
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        FeaturesCollapsibleCard(
            expanded = featuresExpanded,
            onToggle = { featuresExpanded = !featuresExpanded },
            enabledCount = enabledCount,
            totalCount = features.size,
            features = features,
            strings = strings
        )

        Spacer(Modifier.height(32.dp))

        // ========================================================
        //   Validação + Botão final
        // ========================================================
        val osrcOk = when (osrcSource) {
            FileSource.NEW_UPLOAD -> osrcFilePath != null
            FileSource.REUSE_EXISTING -> osrcReuseUrl.isNotBlank()
        }
        val bootOk = when (bootSource) {
            FileSource.NEW_UPLOAD -> bootImgPath != null
            FileSource.REUSE_EXISTING -> bootReuseUrl.isNotBlank()
        }
        val repackOk = !enableBootRepack || bootOk

        val ready = when (mode) {
            BuildMode.WILDKERNELS -> repackOk
            BuildMode.CUSTOM -> osrcOk && bootOk
        }
        val busy = isCopyingOsrc || isConvertingBoot

        val motivo = when {
            mode == BuildMode.WILDKERNELS -> when {
                busy -> strings.configWaitingProcessing
                enableBootRepack && !bootOk && bootSource == FileSource.NEW_UPLOAD ->
                    strings.configSelectBootImage
                enableBootRepack && !bootOk && bootSource == FileSource.REUSE_EXISTING ->
                    strings.configSelectExistingBoot
                else -> ""
            }
            else -> when {
                busy -> strings.configWaitingProcessing
                !osrcOk && osrcSource == FileSource.NEW_UPLOAD -> strings.configSelectOsrc
                !osrcOk && osrcSource == FileSource.REUSE_EXISTING -> strings.configSelectExistingOsrc
                !bootOk && bootSource == FileSource.NEW_UPLOAD -> strings.configSelectBootImage
                !bootOk && bootSource == FileSource.REUSE_EXISTING -> strings.configSelectExistingBoot
                else -> ""
            }
        }

        if (motivo.isNotBlank()) {
            InfoBanner(motivo)
            Spacer(Modifier.height(12.dp))
        }

        BuildButton(
            enabled = ready && !busy,
            label = strings.configStartBuild,
            onClick = {
                val config = BuildConfig(
                    mode = mode,
                    releaseType = releaseType,
                    lto = lto,
                    deviceBranch = deviceBranch,
                    deviceBuildType = deviceBuildType,
                    deviceModel = deviceModel,
                    enableBootRepack = enableBootRepack,
                    osrcSource = osrcSource,
                    osrcFilePath = osrcFilePath ?: "",
                    osrcReuseUrl = osrcReuseUrl,
                    bootSource = bootSource,
                    bootImagePath = bootImgPath ?: "",
                    bootReuseUrl = bootReuseUrl,
                    ksun = ksun, susfs = susfs,
                    zeromount = zeromount, nomount = nomount,
                    bbg = bbg, unicodefix = unicodefix,
                    droidspace = droidspace, ntsync = ntsync,
                    bbrv3 = bbrv3, cache = cache,
                    ipv6Nat = ipv6Nat, optimization = optimization, ttl = ttl,
                    fixKsun = fixKsun
                )
                onStartBuild(session, config, strings)
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}

// ============================================================
//              COMPONENTES DE UI
// ============================================================

@Composable
private fun SessionHeaderCard(
    session: UserSession,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit
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
                    .background(
                        Brush.horizontalGradient(
                            listOf(AccentBlue, AccentPurple)
                        )
                    )
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AccentBlue, AccentPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        strings.configConnected,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        session.login,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        session.repoName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LanguageSwitcher(
                        currentLanguage = currentLanguage,
                        onLanguageChange = onLanguageChange
                    )

                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(strings.logout, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: String,
    title: String,
    subtitle: String? = null
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        subtitle?.let {
            Spacer(Modifier.height(2.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModeCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    description: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(250),
        label = "modeCardBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(250),
        label = "modeCardBorder"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
            if (selected) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
    }
}

@Composable
private fun AutoDetectedBadge(strings: AppStrings) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AccentGreen.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("✅", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            strings.configAutoDetected,
            style = MaterialTheme.typography.bodySmall,
            color = AccentGreen,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoBanner(message: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(Modifier.width(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun BuildButton(enabled: Boolean, label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 1.dp
        )
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ============================================================
//      MENU DE RECURSOS — COLAPSÁVEL
// ============================================================

data class FeatureToggle(
    val emoji: String,
    val title: String,
    val checked: Boolean,
    val onChange: (Boolean) -> Unit
)

@Composable
private fun FeaturesCollapsibleCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    enabledCount: Int,
    totalCount: Int,
    features: List<FeatureToggle>,
    strings: AppStrings
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "arrowRotation"
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentPurple.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎛️", fontSize = 20.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        strings.configConfigureFeatures,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        strings.configFeaturesActive.format(enabledCount, totalCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentPurple.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$enabledCount/$totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentPurple,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(8.dp))

                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(arrowRotation)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(tween(200))
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(4.dp))

                    features.forEachIndexed { index, feature ->
                        FeatureToggleRow(feature)
                        if (index < features.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 60.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun FeatureToggleRow(feature: FeatureToggle) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { feature.onChange(!feature.checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (feature.checked) AccentPurple.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(feature.emoji, fontSize = 18.sp)
        }

        Spacer(Modifier.width(12.dp))

        Text(
            feature.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = feature.checked,
            onCheckedChange = feature.onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentPurple,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

// ============================================================
//      CARD DE FONTE DE ARQUIVO (OSRC / boot.img)
// ============================================================

@Composable
private fun FileSourceCard(
    uploads: List<ReleaseInfo>,
    source: FileSource,
    onSourceChange: (FileSource) -> Unit,
    selectedUrl: String,
    onUrlSelect: (String) -> Unit,
    loadingUploads: Boolean,
    isNewFile: Boolean,
    isProcessing: Boolean,
    fileName: String?,
    errorMessage: String?,
    processingMessage: String,
    onPickFile: () -> Unit,
    onReloadUploads: () -> Unit,
    accent: Color,
    strings: AppStrings
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = source == FileSource.NEW_UPLOAD,
                    onClick = { onSourceChange(FileSource.NEW_UPLOAD) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("📤  ${strings.configNewUpload}") }
                SegmentedButton(
                    selected = source == FileSource.REUSE_EXISTING,
                    onClick = { onSourceChange(FileSource.REUSE_EXISTING) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("📂  ${strings.configReuseExisting} (${uploads.size})") }
            }

            Spacer(Modifier.height(12.dp))

            if (source == FileSource.NEW_UPLOAD) {
                Button(
                    onClick = onPickFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(
                        if (fileName == null) strings.configSelectFile else strings.configChangeFile,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                if (isProcessing) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accent
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            processingMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                fileName?.let {
                    Spacer(Modifier.height(10.dp))
                    StatusChip("📦  $it", accent)
                }

                errorMessage?.let {
                    Spacer(Modifier.height(6.dp))
                    val isError = it.startsWith("❌") || it.startsWith("Erro")
                    val chipColor = if (isError) MaterialTheme.colorScheme.error else accent
                    StatusChip(it, chipColor)
                }
            } else {
                if (loadingUploads) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accent
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            strings.configLoadingUploads,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            if (uploads.isEmpty()) {
                                StatusChip(
                                    strings.configNoPreviousUpload,
                                    MaterialTheme.colorScheme.outline
                                )
                            } else {
                                UploadDropdown(uploads, selectedUrl, onUrlSelect, strings)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = onReloadUploads,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.12f))
                        ) {
                            Text("🔄", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

// ============================================================
//              COMPONENTES AUXILIARES
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSelector(
    devices: List<DeviceInfo>,
    selected: String,
    onSelected: (DeviceInfo) -> Unit,
    strings: AppStrings,
    refreshing: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery, devices) {
        if (searchQuery.isBlank()) devices
        else devices.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.branch.contains(searchQuery, ignoreCase = true)
        }
    }

    val selectedDevice = devices.firstOrNull { it.branch == selected }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedDevice?.displayName ?: "Escolher dispositivo...",
            onValueChange = {},
            readOnly = true,
            leadingIcon = { Text("📱") },
            trailingIcon = {
                if (refreshing) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = AccentPurple
                    )
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar...") },
                leadingIcon = { Text("🔍") },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Nenhum dispositivo encontrado") },
                    onClick = { }
                )
            } else {
                filtered.forEach { device ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(device.displayName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    device.branch,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onSelected(device)
                            expanded = false
                            searchQuery = ""
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadDropdown(
    uploads: List<ReleaseInfo>,
    selectedUrl: String,
    onUrlSelect: (String) -> Unit,
    strings: AppStrings
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAsset = uploads.flatMap { it.assets }
        .firstOrNull { it.browserDownloadUrl == selectedUrl }
    val selectedRelease = uploads.firstOrNull { release ->
        release.assets.any { it.browserDownloadUrl == selectedUrl }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (selectedRelease != null && selectedAsset != null)
                    "${formatReleaseLabel(selectedRelease)} (${selectedAsset.size / 1024 / 1024} MB)"
                else strings.configChooseUpload,
                maxLines = 1
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            uploads.forEach { release ->
                val asset = release.assets.firstOrNull() ?: return@forEach
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(formatReleaseLabel(release),
                                style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${asset.size / 1024 / 1024} MB • ${formatDate(release.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onUrlSelect(asset.browserDownloadUrl)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatReleaseLabel(release: ReleaseInfo): String {
    val prefix = if (release.tagName.startsWith("osrc-")) "OSRC" else "BOOT"
    return "$prefix • ${formatDate(release.createdAt)}"
}

private fun formatDate(iso: String): String {
    return try {
        val date = iso.substring(0, 10)
        val time = iso.substring(11, 16)
        val parts = date.split("-")
        "${parts[2]}/${parts[1]} $time"
    } catch (_: Exception) { iso }
}