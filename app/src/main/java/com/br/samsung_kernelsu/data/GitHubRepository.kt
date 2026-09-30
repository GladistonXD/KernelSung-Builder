package com.br.samsung_kernelsu.data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class GitHubRepository {

    private val api: GitHubApi
    private val client: OkHttpClient
    private val gson: Gson = Gson()

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(600, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(NullOnEmptyConverterFactory())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        api = retrofit.create(GitHubApi::class.java)
    }

    suspend fun requestDeviceCode(clientId: String): DeviceCodeResponse {
        val response = api.requestDeviceCode(clientId)
        val rawBody: String = response.body()?.string().orEmpty()

        if (!response.isSuccessful) {
            val hint = when (response.code()) {
                404 -> "Client ID inválido ou Device Flow não habilitado no OAuth App."
                403 -> "Acesso negado. Verifique se o Device Flow está habilitado."
                else -> "HTTP ${response.code()}"
            }
            throw Exception("Erro ao solicitar device code: $hint\nResposta bruta: ${rawBody.take(300)}")
        }

        if (rawBody.isBlank()) {
            throw Exception("GitHub retornou resposta vazia. Verifique o Client ID e o Device Flow.")
        }

        return parseDeviceCodeResponse(rawBody)
    }

    suspend fun pollAccessToken(clientId: String, deviceCode: String): AccessTokenResponse {
        val response = api.pollAccessToken(clientId, deviceCode)
        val rawBody: String = response.body()?.string().orEmpty()

        if (!response.isSuccessful) {
            throw Exception("Erro ao poll token: HTTP ${response.code()} - ${rawBody.take(300)}")
        }

        if (rawBody.isBlank()) {
            throw Exception("Resposta vazia no poll token.")
        }

        return parseAccessTokenResponse(rawBody)
    }

    private fun parseDeviceCodeResponse(rawBody: String): DeviceCodeResponse {
        val trimmed = rawBody.trim()
        if (trimmed.startsWith("{")) {
            try {
                return gson.fromJson(trimmed, DeviceCodeResponse::class.java)
            } catch (_: Exception) { }
        }

        val map = parseFormUrlEncoded(trimmed)
        val deviceCode = map["device_code"]
            ?: throw Exception("Resposta não contém 'device_code': ${rawBody.take(300)}")
        val userCode = map["user_code"]
            ?: throw Exception("Resposta não contém 'user_code': ${rawBody.take(300)}")
        val verificationUri = map["verification_uri"]
            ?: throw Exception("Resposta não contém 'verification_uri': ${rawBody.take(300)}")
        val expiresIn = map["expires_in"]?.toIntOrNull() ?: 900
        val interval = map["interval"]?.toIntOrNull() ?: 5

        return DeviceCodeResponse(deviceCode, userCode, verificationUri, expiresIn, interval)
    }

    private fun parseAccessTokenResponse(rawBody: String): AccessTokenResponse {
        val trimmed = rawBody.trim()
        if (trimmed.startsWith("{")) {
            try {
                return gson.fromJson(trimmed, AccessTokenResponse::class.java)
            } catch (_: Exception) { }
        }

        val map = parseFormUrlEncoded(trimmed)
        return AccessTokenResponse(
            accessToken = map["access_token"],
            tokenType = map["token_type"],
            scope = map["scope"],
            error = map["error"],
            errorDescription = map["error_description"]
        )
    }

    private fun parseFormUrlEncoded(body: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        body.split("&").forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = pair.substring(0, idx)
                val value = pair.substring(idx + 1)
                result[key] = URLDecoder.decode(value, "UTF-8")
            }
        }
        return result
    }

    suspend fun getAuthenticatedUser(token: String): GitHubUser {
        val response = api.getAuthenticatedUser(auth = "Bearer $token")
        if (!response.isSuccessful) throw Exception("Erro ao obter usuário: ${response.code()}")
        return response.body() ?: throw Exception("Usuário vazio")
    }

    suspend fun ensureForkExists(token: String, userLogin: String): GitHubRepo {
        val auth = "Bearer $token"
        val repoName = WorkflowTemplate.SOURCE_REPO

        val existing = api.getRepository(auth = auth, owner = userLogin, repo = repoName)
        if (existing.isSuccessful) {
            val repo = existing.body() ?: throw Exception("Fork vazio")
            try {
                api.syncFork(
                    auth = auth,
                    owner = userLogin,
                    repo = repoName,
                    body = mapOf("branch" to WorkflowTemplate.SOURCE_BRANCH)
                )
            } catch (_: Exception) { }
            return repo
        }

        if (existing.code() != 404) {
            throw Exception("Erro ao verificar fork: ${existing.code()}")
        }

        val created = api.createFork(
            auth = auth,
            owner = WorkflowTemplate.SOURCE_OWNER,
            repo = WorkflowTemplate.SOURCE_REPO
        )
        if (!created.isSuccessful) {
            throw Exception("Falha ao criar fork: ${created.code()} - ${created.errorBody()?.string()}")
        }

        var forkRepo: GitHubRepo? = null
        var attempts = 0
        while (attempts < 15 && forkRepo == null) {
            Thread.sleep(2000)
            val reread = api.getRepository(auth = auth, owner = userLogin, repo = repoName)
            if (reread.isSuccessful) forkRepo = reread.body()
            attempts++
        }

        return forkRepo ?: throw Exception("Fork criado mas não ficou disponível a tempo")
    }

    suspend fun enableActionsInFork(token: String, owner: String, repo: String) {
        val auth = "Bearer $token"
        val body = mapOf<String, Any>(
            "enabled" to true,
            "allowed_actions" to "all"
        )
        val response = api.setActionsPermissions(auth, owner, repo, body)
        if (!response.isSuccessful) {
            val err = response.errorBody()?.string() ?: "sem detalhes"
            throw Exception("Falha ao habilitar Actions: ${response.code()} - $err")
        }
    }

    suspend fun getWorkflowId(token: String, owner: String, repo: String, fileName: String): String? {
        val auth = "Bearer $token"
        val response = api.listWorkflows(auth, owner, repo)
        if (!response.isSuccessful) return null
        val body = response.body() ?: return null
        @Suppress("UNCHECKED_CAST")
        val workflows = body["workflows"] as? List<Map<String, Any>> ?: return null
        for (wf in workflows) {
            val path = wf["path"] as? String ?: continue
            if (path.endsWith(fileName)) {
                return (wf["id"] as? Double)?.toLong()?.toString()
                    ?: (wf["id"] as? Int)?.toString()
            }
        }
        return null
    }

    suspend fun createOrUpdateCustomWorkflow(
        token: String,
        owner: String,
        repo: String,
        branch: String
    ): Boolean {
        return createOrUpdateWorkflowFile(
            token = token,
            owner = owner,
            repo = repo,
            branch = branch,
            path = WorkflowTemplate.CUSTOM_WORKFLOW_PATH,
            content = WorkflowTemplate.CUSTOM_CONTENT,
            commitMessage = "Adiciona workflow customizado"
        )
    }

    suspend fun createOrUpdateSingleDeviceWorkflow(
        token: String,
        owner: String,
        repo: String,
        branch: String
    ): Boolean {
        return createOrUpdateWorkflowFile(
            token = token,
            owner = owner,
            repo = repo,
            branch = branch,
            path = WorkflowTemplate.SINGLE_DEVICE_WORKFLOW_PATH,
            content = WorkflowTemplate.SINGLE_DEVICE_CONTENT,
            commitMessage = "Adiciona workflow single-device"
        )
    }

    private suspend fun createOrUpdateWorkflowFile(
        token: String,
        owner: String,
        repo: String,
        branch: String,
        path: String,
        content: String,
        commitMessage: String
    ): Boolean {
        return createOrUpdateFile(
            token = token,
            owner = owner,
            repo = repo,
            branch = branch,
            path = path,
            content = content,
            commitMessage = commitMessage
        )
    }

    private suspend fun createOrUpdateFile(
        token: String,
        owner: String,
        repo: String,
        branch: String,
        path: String,
        content: String,
        commitMessage: String
    ): Boolean {
        val auth = "Bearer $token"

        val existing = api.getFileContent(auth, owner, repo, path)
        val sha: String? = if (existing.isSuccessful) existing.body()?.sha else null
        Log.d("KSUN_SYNC", "GET $path → HTTP ${existing.code()}, sha=${sha ?: "null"}")

        val encodedContent = Base64.encodeToString(
            content.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )

        val body = CreateOrUpdateFileRequest(
            message = commitMessage,
            content = encodedContent,
            branch = branch,
            sha = sha
        )

        val response = api.createOrUpdateFile(auth, owner, repo, path, body)
        if (!response.isSuccessful) {
            val err = response.errorBody()?.string() ?: "sem detalhes"
            Log.e("KSUN_SYNC", "PUT $path → HTTP ${response.code()}: $err")
            throw Exception("Falha ao criar/atualizar $path: ${response.code()} - $err")
        }
        Log.d("KSUN_SYNC", "PUT $path → HTTP ${response.code()} (sucesso)")
        return true
    }

    suspend fun syncKsunActionYml(
        token: String,
        forkOwner: String,
        forkRepo: String,
        forkBranch: String,
        keepFixStep: Boolean
    ): Boolean {
        val path = ".github/actions/ksun/action.yml"
        val backupPath = ".github/actions/ksun/action.yml.orig"

        Log.d("KSUN_SYNC", "▶ Iniciando sync (keepFixStep=$keepFixStep)")

        val current = fetchRawFile(token, forkOwner, forkRepo, forkBranch, path)
        if (current == null) {
            Log.e("KSUN_SYNC", "✗ Não consegui ler $path do fork $forkOwner/$forkRepo")
            return false
        }
        Log.d("KSUN_SYNC", "Atual: ${current.length} bytes, tem 'Fix KernelSU-Next'? ${current.contains("Fix KernelSU-Next")}")

        var backup = fetchRawFile(token, forkOwner, forkRepo, forkBranch, backupPath)
        if (backup == null) {
            if (current.contains("Fix KernelSU-Next")) {
                Log.d("KSUN_SYNC", "Criando backup $backupPath (com a etapa)")
                try {
                    createOrUpdateFile(
                        token = token,
                        owner = forkOwner,
                        repo = forkRepo,
                        branch = forkBranch,
                        path = backupPath,
                        content = current,
                        commitMessage = "Backup original ksun/action.yml"
                    )
                    backup = current
                } catch (e: Exception) {
                    Log.w("KSUN_SYNC", "Falha ao criar backup: ${e.message} — prosseguindo com current como base")
                    backup = current
                }
            } else {
                Log.w("KSUN_SYNC", "Sem backup e sem etapa no atual — nada a fazer")
                return true
            }
        } else {
            Log.d("KSUN_SYNC", "Backup já existe: ${backup.length} bytes")
        }

        val baseContent = backup ?: current
        val targetContent = if (keepFixStep) {
            baseContent
        } else {
            removeKsunFixStep(baseContent)
        }
        Log.d("KSUN_SYNC", "Alvo: ${targetContent.length} bytes, tem 'Fix KernelSU-Next'? ${targetContent.contains("Fix KernelSU-Next")}")

        if (current.trim() == targetContent.trim()) {
            Log.d("KSUN_SYNC", "✓ Já está no estado desejado")
            return true
        }

        Log.d("KSUN_SYNC", "▶ Escrevendo $path...")
        return try {
            createOrUpdateFile(
                token = token,
                owner = forkOwner,
                repo = forkRepo,
                branch = forkBranch,
                path = path,
                content = targetContent,
                commitMessage = if (keepFixStep)
                    "Restore ksun/action.yml with Fix KernelSU-Next"
                else
                    "Remove obsolete 'Fix KernelSU-Next' step"
            )
            Log.d("KSUN_SYNC", "✓ Sync concluído")
            true
        } catch (e: Exception) {
            Log.e("KSUN_SYNC", "✗ Falha ao escrever: ${e.message}", e)
            throw e
        }
    }

    private fun removeKsunFixStep(yaml: String): String {
        val lines = yaml.lines()
        var startIndex = -1
        var stepIndent = -1

        for ((i, line) in lines.withIndex()) {
            val trimmed = line.trimStart()
            if (trimmed.startsWith("- ") && trimmed.contains("Fix KernelSU-Next")) {
                startIndex = i
                stepIndent = line.length - trimmed.length
                Log.d("KSUN_SYNC", "  Step encontrado na linha $i (indent=$stepIndent)")
                break
            }
        }
        if (startIndex < 0) {
            Log.d("KSUN_SYNC", "  Nenhum step para remover")
            return yaml
        }

        var endIndex = lines.size
        for (i in (startIndex + 1) until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val trimmed = line.trimStart()
            val indent = line.length - trimmed.length

            if (indent < stepIndent) { endIndex = i; break }
            if (indent == stepIndent && trimmed.startsWith("- ")) { endIndex = i; break }
        }
        Log.d("KSUN_SYNC", "  Removendo linhas $startIndex..$endIndex")

        val result = lines.toMutableList()
        var removeStart = startIndex
        while (removeStart > 0 && result[removeStart - 1].isBlank()) {
            removeStart--
        }
        result.subList(removeStart, endIndex).clear()
        return result.joinToString("\n")
    }

    private suspend fun fetchRawFile(
        token: String,
        owner: String,
        repo: String,
        branch: String,
        path: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.raw+json")
                .header("User-Agent", "KernelSung-Builder")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code in 200..299) {
                    response.body?.string()
                } else {
                    Log.d("KSUN_SYNC", "  fetchRawFile $owner/$repo@$branch/$path → HTTP ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("KSUN_SYNC", "  fetchRawFile exception: ${e.message}", e)
            null
        }
    }

    suspend fun fetchUpstreamFile(
        token: String,
        owner: String,
        repo: String,
        branch: String,
        path: String
    ): String? = fetchRawFile(token, owner, repo, branch, path)

    suspend fun dispatchBuild(
        token: String,
        owner: String,
        repo: String,
        workflowId: String,
        branch: String,
        inputs: Map<String, String>
    ) {
        val auth = "Bearer $token"
        val body = WorkflowDispatchRequest(ref = branch, inputs = inputs)
        val response = api.dispatchWorkflow(auth, owner, repo, workflowId, body)
        if (!response.isSuccessful) {
            val err = response.errorBody()?.string() ?: "sem detalhes"
            throw Exception("Falha ao disparar build: ${response.code()} - $err")
        }
    }

    suspend fun listRuns(token: String, owner: String, repo: String, perPage: Int = 20): List<WorkflowRun> {
        val response = api.getWorkflowRuns("Bearer $token", owner, repo, perPage)
        if (!response.isSuccessful) return emptyList()
        return response.body()?.workflowRuns ?: emptyList()
    }

    suspend fun getLatestRun(token: String, owner: String, repo: String): WorkflowRun? {
        val response = api.getWorkflowRuns("Bearer $token", owner, repo)
        if (!response.isSuccessful) throw Exception("Erro ao obter runs: ${response.code()}")
        return response.body()?.workflowRuns?.firstOrNull()
    }

    suspend fun getRunById(token: String, owner: String, repo: String, runId: Long): WorkflowRun? {
        val response = api.getWorkflowRuns("Bearer $token", owner, repo, perPage = 50)
        if (!response.isSuccessful) throw Exception("Erro ao obter runs: ${response.code()}")
        return response.body()?.workflowRuns?.firstOrNull { it.id == runId }
    }

    suspend fun getWorkflowRunById(token: String, owner: String, repo: String, runId: Long): WorkflowRun? {
        val response = api.getWorkflowRun("Bearer $token", owner, repo, runId)
        if (!response.isSuccessful) return null
        return response.body()
    }

    suspend fun getWorkflowJobs(token: String, owner: String, repo: String, runId: Long): List<WorkflowJob> {
        val response = api.getWorkflowJobs("Bearer $token", owner, repo, runId)
        if (!response.isSuccessful) return emptyList()
        return response.body()?.jobs ?: emptyList()
    }

    suspend fun getArtifacts(token: String, owner: String, repo: String, runId: Long): List<Artifact> {
        val response = api.getArtifacts("Bearer $token", owner, repo, runId)
        if (!response.isSuccessful) throw Exception("Erro ao obter artefatos: ${response.code()}")
        return response.body()?.artifacts ?: emptyList()
    }

    fun downloadArtifact(
        token: String,
        owner: String,
        repo: String,
        artifactId: Long,
        outputFile: File
    ): Boolean {
        val url = "https://api.github.com/repos/$owner/$repo/actions/artifacts/$artifactId/zip"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.code !in 200..299) throw Exception("Download falhou: HTTP ${response.code}")
                val body = response.body ?: throw Exception("Corpo vazio")
                body.byteStream().use { input ->
                    outputFile.outputStream().use { output -> input.copyTo(output) }
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace(); false
        }
    }

    fun downloadRunLogs(
        token: String,
        owner: String,
        repo: String,
        runId: Long,
        outputFile: File
    ): Boolean {
        val url = "https://api.github.com/repos/$owner/$repo/actions/runs/$runId/logs"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.code !in 200..299) throw Exception("HTTP ${response.code}")
                val body = response.body ?: throw Exception("Corpo vazio")
                body.byteStream().use { input ->
                    outputFile.outputStream().use { output -> input.copyTo(output) }
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace(); false
        }
    }

    suspend fun listUploads(
        token: String,
        owner: String,
        repo: String,
        prefixes: List<String>
    ): List<ReleaseInfo> {
        val response = api.listReleases("Bearer $token", owner, repo, perPage = 50)
        if (!response.isSuccessful) return emptyList()

        return response.body()
            ?.filter { release -> prefixes.any { release.tagName.startsWith(it) } }
            ?.filter { it.assets.isNotEmpty() }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    suspend fun listReleaseAssets(
        token: String,
        owner: String,
        repo: String,
        tag: String
    ): List<String> {
        val url = "https://api.github.com/repos/$owner/$repo/releases/tags/$tag"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                @Suppress("UNCHECKED_CAST")
                val map = gson.fromJson(body, Map::class.java) as Map<String, Any>
                @Suppress("UNCHECKED_CAST")
                val assets = map["assets"] as? List<Map<String, Any>> ?: return emptyList()
                assets.mapNotNull { it["name"] as? String }
            }
        } catch (e: Exception) {
            e.printStackTrace(); emptyList()
        }
    }

    fun uploadBootImageAsReleaseAsset(
        token: String,
        owner: String,
        repo: String,
        file: File,
        tag: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): String {
        val releaseId = createRelease(token, owner, repo, tag)
        return uploadReleaseAsset(token, owner, repo, releaseId, file, onProgress)
    }

    private fun createRelease(token: String, owner: String, repo: String, tag: String): Long {
        val url = "https://api.github.com/repos/$owner/$repo/releases"
        val json = """
            {
              "tag_name": "$tag",
              "name": "Upload $tag",
              "body": "Upload automático",
              "draft": false,
              "prerelease": false
            }
        """.trimIndent()

        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code !in 200..299) {
                val err = response.body?.string() ?: "sem detalhes"
                throw Exception("Falha ao criar release: HTTP ${response.code} - $err")
            }
            val responseBody = response.body?.string() ?: throw Exception("Resposta vazia")
            @Suppress("UNCHECKED_CAST")
            val map = gson.fromJson(responseBody, Map::class.java) as Map<String, Any>
            return (map["id"] as? Double)?.toLong() ?: throw Exception("ID da release ausente")
        }
    }

    private fun uploadReleaseAsset(
        token: String,
        owner: String,
        repo: String,
        releaseId: Long,
        file: File,
        onProgress: (Long, Long) -> Unit
    ): String {
        val url = "https://uploads.github.com/repos/$owner/$repo/releases/$releaseId/assets" +
                "?name=${file.name}"

        val requestBody = ProgressRequestBody(file, onProgress)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("Content-Type", "application/octet-stream")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code !in 200..299) {
                val err = response.body?.string() ?: "sem detalhes"
                throw Exception("Falha no upload: HTTP ${response.code} - $err")
            }
            val responseBody = response.body?.string() ?: throw Exception("Resposta vazia")
            @Suppress("UNCHECKED_CAST")
            val map = gson.fromJson(responseBody, Map::class.java) as Map<String, Any>
            return map["browser_download_url"] as? String
                ?: throw Exception("URL de download ausente")
        }
    }
}

private class ProgressRequestBody(
    private val file: File,
    private val onProgress: (Long, Long) -> Unit
) : RequestBody() {

    private val totalBytes = file.length()
    private val chunkSize = 64 * 1024L

    override fun contentType(): MediaType? = "application/octet-stream".toMediaType()

    override fun contentLength(): Long = totalBytes

    override fun writeTo(sink: BufferedSink) {
        var uploaded = 0L
        val buffer = ByteArray(chunkSize.toInt())

        file.inputStream().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                sink.write(buffer, 0, read)
                sink.flush()
                uploaded += read
                onProgress(uploaded, totalBytes)
            }
        }
    }
}