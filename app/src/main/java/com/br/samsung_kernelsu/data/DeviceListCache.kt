package com.br.samsung_kernelsu.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Cache dinâmico da lista de dispositivos do WildKernels.
 *
 * Fonte: .github/workflows/kernel-samsung.yml (seção matrix.include)
 *
 * - Carrega do SharedPreferences na primeira chamada de init()
 * - refresh() baixa do upstream, faz parse e salva
 * - Se falhar, mantém o cache anterior (ou a lista estática como fallback)
 */
object DeviceListCache {

    private const val TAG = "DeviceListCache"
    private const val PREFS_NAME = "device_list_cache"
    private const val KEY_DEVICES = "devices_serialized"
    private const val KEY_TIMESTAMP = "last_refresh"

    private const val UPSTREAM_PATH = ".github/workflows/kernel-samsung.yml"

    private val _devices = MutableStateFlow<List<DeviceInfo>>(DeviceList.devices)
    val devices: StateFlow<List<DeviceInfo>> = _devices

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _lastRefresh = MutableStateFlow(0L)
    val lastRefresh: StateFlow<Long> = _lastRefresh

    private var initialized = false

    /**
     * Carrega o cache salvo (SharedPreferences). Idempotente.
     * Chame isso cedo no MainActivity.
     */
    fun init(context: Context) {
        if (initialized) return
        initialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_DEVICES, null)
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)

        if (!cached.isNullOrBlank()) {
            try {
                val list = deserialize(cached)
                if (list.isNotEmpty()) {
                    _devices.value = list
                    _lastRefresh.value = timestamp
                    Log.d(TAG, "Carregados ${list.size} devices do cache (ts=$timestamp)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao ler cache", e)
            }
        }
    }

    /**
     * Atualiza a lista do upstream. Não bloqueia a UI, mas é suspend
     * (rode em coroutine).
     */
    suspend fun refresh(context: Context, token: String) {
        if (_refreshing.value) return
        _refreshing.value = true

        try {
            val repository = GitHubRepository()
            val yaml = withContext(Dispatchers.IO) {
                repository.fetchUpstreamFile(
                    token = token,
                    owner = WorkflowTemplate.SOURCE_OWNER,
                    repo = WorkflowTemplate.SOURCE_REPO,
                    branch = WorkflowTemplate.SOURCE_BRANCH,
                    path = UPSTREAM_PATH
                )
            }

            if (yaml.isNullOrBlank()) {
                Log.w(TAG, "Upstream retornou vazio para $UPSTREAM_PATH")
                return
            }

            val parsed = parseMatrixDevices(yaml)
            if (parsed.isEmpty()) {
                Log.w(TAG, "Nenhum device encontrado no parse")
                return
            }

            Log.d(TAG, "Atualizado: ${parsed.size} devices")
            _devices.value = parsed

            val now = System.currentTimeMillis()
            _lastRefresh.value = now

            // Persiste
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_DEVICES, serialize(parsed))
                .putLong(KEY_TIMESTAMP, now)
                .apply()

        } catch (e: Exception) {
            Log.e(TAG, "Falha no refresh", e)
        } finally {
            _refreshing.value = false
        }
    }

    /** Busca um device pelo branch na lista ATUAL (dinâmica ou fallback). */
    fun findByBranch(branch: String): DeviceInfo? =
        _devices.value.firstOrNull { it.branch == branch }

    // ============================================================
    //              PARSER DO kernel-samsung.yml
    // ============================================================

    /**
     * Extrai os pares (branch, build_type) da seção `matrix.include`.
     * Formato esperado:
     *
     *   matrix:
     *     include:
     *       - branch: "SM-S938B-Oneui8.5"
     *         build_type: "Bazel"
     *       - branch: "SM-A546B-Oneui7"
     *         build_type: "make"
     */
    private fun parseMatrixDevices(yaml: String): List<DeviceInfo> {
        val result = mutableListOf<DeviceInfo>()
        val lines = yaml.lines()

        // Regex para "  - branch: X" ou "  - branch: "X""
        val branchRegex = Regex("""^\s*-\s*branch:\s*["']?([^"'\s]+)["']?\s*$""")
        // Regex para "    build_type: X" ou "    build_type: "X""
        val buildTypeRegex = Regex("""^\s*build_type:\s*["']?([^"'\s]+)["']?\s*$""")

        var currentBranch: String? = null
        var currentBuildType: String? = null

        fun flush() {
            val b = currentBranch ?: return
            val t = currentBuildType ?: "Bazel"
            val displayName = resolveDisplayName(b)
            result.add(DeviceInfo(b, displayName, t))
            currentBranch = null
            currentBuildType = null
        }

        for (raw in lines) {
            val line = raw.trimEnd()
            if (line.isBlank()) continue
            val trimmed = line.trimStart()
            if (trimmed.startsWith("#")) continue

            branchRegex.find(line)?.let { match ->
                flush()
                currentBranch = match.groupValues[1]
                return@let
            }

            if (currentBranch != null) {
                buildTypeRegex.find(line)?.let { match ->
                    currentBuildType = match.groupValues[1]
                }
            }
        }
        flush()

        // Deduplica por branch (caso o arquivo tenha duplicatas)
        return result.distinctBy { it.branch }
    }

    // ============================================================
    //         DISPLAY NAME (nome amigável) — fallback
    // ============================================================

    /**
     * Tenta pegar o displayName da lista estática primeiro (que tem nomes
     * amigáveis como "Galaxy S24 Ultra • One UI 7").
     * Se não existir lá, gera um nome a partir do branch.
     */
    private fun resolveDisplayName(branch: String): String {
        DeviceList.findByBranch(branch)?.let { return it.displayName }
        return buildDisplayNameFromBranch(branch)
    }

    /**
     * Gera nome amigável a partir do branch.
     *
     * Exemplos:
     *   "SM-S928B-Oneui7"       → "SM-S928B • One UI 7"
     *   "SM-S928B-Oneui8-bit4"  → "SM-S928B • One UI 8 (bit4)"
     *   "SM-M146B-Oneui7-ADYJ2" → "SM-M146B • One UI 7 (ADYJ2)"
     *   "SM-X916B"              → "SM-X916B"
     */
    private fun buildDisplayNameFromBranch(branch: String): String {
        val parts = branch.split("-")
        if (parts.size < 2) return branch

        val oneUiIndex = parts.indexOfFirst {
            it.startsWith("Oneui", ignoreCase = true)
        }
        if (oneUiIndex < 0) return branch

        val model = parts.subList(0, oneUiIndex).joinToString("-")
        val oneUiRaw = parts[oneUiIndex]
            .removePrefix("Oneui")
            .removePrefix("oneui")

        val suffix = if (oneUiIndex + 1 < parts.size) {
            parts.subList(oneUiIndex + 1, parts.size).joinToString("-")
        } else ""

        return if (suffix.isBlank()) {
            "$model • One UI $oneUiRaw"
        } else {
            "$model • One UI $oneUiRaw ($suffix)"
        }
    }

    // ============================================================
    //         SERIALIZAÇÃO (SharedPreferences)
    // ============================================================

    private fun serialize(devices: List<DeviceInfo>): String =
        devices.joinToString("\n") { "${it.branch}|${it.displayName}|${it.buildType}" }

    private fun deserialize(data: String): List<DeviceInfo> =
        data.lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size != 3) return@mapNotNull null
            DeviceInfo(parts[0], parts[1], parts[2])
        }
}