package com.br.samsung_kernelsu.util

import android.util.Log
import com.br.samsung_kernelsu.data.WorkflowInput
import java.net.HttpURLConnection
import java.net.URL


object WorkflowInputParser {

    private const val TAG = "WorkflowInputParser"

    private var cachedSha: String? = null
    private var cachedInputs: List<WorkflowInput>? = null
    private var cachedRepo: String? = null

    fun fetchInputs(
        token: String,
        owner: String,
        repo: String,
        path: String,
        branch: String
    ): List<WorkflowInput>? {
        val repoKey = "$owner/$repo@$branch"

        val sha = fetchFileSha(token, owner, repo, path, branch)
        if (sha == null) {
            Log.w(TAG, "Falha ao obter SHA de $owner/$repo@$branch")
            return null
        }

        if (sha == cachedSha && cachedRepo == repoKey && cachedInputs != null) {
            Log.d(TAG, "Cache hit (SHA=$sha)")
            return cachedInputs
        }

        val yaml = downloadFile(token, owner, repo, path, branch)
        if (yaml == null) return null

        val inputs = parseInputs(yaml)
        if (inputs.isEmpty()) {
            Log.w(TAG, "Nenhum input encontrado em $path")
            return null
        }

        cachedSha = sha
        cachedRepo = repoKey
        cachedInputs = inputs
        Log.d(TAG, "Carregados ${inputs.size} inputs de $owner/$repo")
        return inputs
    }

    fun invalidateCache() {
        cachedSha = null
        cachedInputs = null
        cachedRepo = null
    }


    private fun fetchFileSha(
        token: String, owner: String, repo: String,
        path: String, branch: String
    ): String? = try {
        val url = URL("https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github.sha")
            setRequestProperty("User-Agent", "KernelSung-Builder")
            connectTimeout = 10_000
            readTimeout = 15_000
        }

        val code = conn.responseCode
        val result = if (code == 200) {
            conn.inputStream.bufferedReader().use { it.readText() }.trim()
        } else {
            Log.w(TAG, "SHA request HTTP $code para $path")
            null
        }
        conn.disconnect()
        result?.ifBlank { null }
    } catch (e: Exception) {
        Log.e(TAG, "Erro buscando SHA", e)
        null
    }

    private fun downloadFile(
        token: String, owner: String, repo: String,
        path: String, branch: String
    ): String? = try {
        val url = URL("https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github.raw+json")
            setRequestProperty("User-Agent", "KernelSung-Builder")
            connectTimeout = 10_000
            readTimeout = 30_000
        }

        val code = conn.responseCode
        val result = if (code == 200) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            Log.w(TAG, "Download HTTP $code para $path")
            null
        }
        conn.disconnect()
        result
    } catch (e: Exception) {
        Log.e(TAG, "Erro baixando YAML", e)
        null
    }

    fun parseInputs(yaml: String): List<WorkflowInput> {
        val inputs = mutableListOf<WorkflowInput>()
        val lines = yaml.lines()

        var inDispatch = false
        var inInputs = false
        var dispatchIndent = -1
        var inputsIndent = -1
        var inputNameIndent = -1

        var name: String? = null
        var type: String? = null
        var default: String? = null
        var description: String? = null
        var required = false
        var options = mutableListOf<String>()
        var inOptions = false
        var optionsIndent = -1

        fun flush() {
            val n = name ?: return
            inputs.add(
                WorkflowInput(
                    name = n,
                    type = type ?: "string",
                    description = description,
                    required = required,
                    default = default,
                    options = options.toList()
                )
            )
            name = null; type = null; default = null
            description = null; required = false
            options = mutableListOf(); inOptions = false
        }

        for (raw in lines) {
            val line = raw.trimEnd()
            if (line.isBlank()) continue

            val indent = line.indexOfFirst { it != ' ' }.coerceAtLeast(0)
            val content = line.trimStart()

            if (content.startsWith("#")) continue

            if (!inDispatch) {
                if (content == "workflow_dispatch:" || content.startsWith("workflow_dispatch:")) {
                    inDispatch = true
                    dispatchIndent = indent
                }
                continue
            }

            if (indent <= dispatchIndent && content.isNotBlank()) {
                inDispatch = false; inInputs = false
                flush()
                continue
            }

            if (!inInputs) {
                if (content.startsWith("inputs:")) {
                    inInputs = true
                    inputsIndent = indent
                }
                continue
            }

            if (indent <= inputsIndent && content.isNotBlank()) {
                inInputs = false
                flush()
                continue
            }

            val isField = content.startsWith("required:") ||
                    content.startsWith("type:") ||
                    content.startsWith("default:") ||
                    content.startsWith("description:") ||
                    content.startsWith("options:")

            if (!isField && content.contains(":")) {
                if (inputNameIndent < 0) {
                    inputNameIndent = indent
                }

                if (indent == inputNameIndent) {
                    flush()
                    name = content.substringBefore(":").trim()
                    continue
                }
            }

            when {
                content.startsWith("required:") -> {
                    required = content.substringAfter(":").trim() == "true"
                }
                content.startsWith("type:") -> {
                    type = content.substringAfter(":")
                        .trim().trim('"').trim('\'')
                }
                content.startsWith("default:") -> {
                    default = content.substringAfter(":")
                        .trim().trim('"').trim('\'')
                        .ifBlank { null }
                }
                content.startsWith("description:") -> {
                    description = content.substringAfter(":")
                        .trim().trim('"').trim('\'')
                        .ifBlank { null }
                }
                content.startsWith("options:") -> {
                    inOptions = true
                    optionsIndent = indent
                }
                inOptions && content.startsWith("-") && indent > optionsIndent -> {
                    val opt = content.removePrefix("-").trim()
                        .trim('"').trim('\'')
                    if (opt.isNotBlank()) options.add(opt)
                }
            }
        }
        flush()
        return inputs
    }
}