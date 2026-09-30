package com.br.samsung_kernelsu.util

import android.os.Build
import android.util.Log
import com.br.samsung_kernelsu.data.DeviceInfo
import com.br.samsung_kernelsu.data.DeviceList

object DeviceDetector {

    private const val TAG = "DeviceDetector"

    fun getCurrentModel(): String {
        val raw = Build.MODEL?.trim().orEmpty()
        if (raw.isBlank()) {
            Log.w(TAG, "Build.MODEL vazio — retornando string vazia")
            return ""
        }

        var cleaned = raw
            .substringBefore("/")
            .substringBefore("_")
            .trim()

        val suffixes = listOf("DS", "5G", "4G", "LTE")
        for (suffix in suffixes) {
            if (cleaned.endsWith(suffix) && cleaned.length > suffix.length) {
                cleaned = cleaned.dropLast(suffix.length)
                break
            }
        }

        Log.d(TAG, "getCurrentModel: raw='$raw' → cleaned='$cleaned'")
        return cleaned
    }


    fun getCurrentModelOrFallback(fallback: String = ""): String {
        val model = getCurrentModel()
        return if (model.isBlank()) fallback else model
    }


    fun getModelFromDetectedBranch(): String? {
        return detect()?.branch?.substringBefore("-")
    }

    fun detect(): DeviceInfo? {
        val model = getCurrentModel()
        if (model.isBlank()) {
            Log.w(TAG, "❌ Build.MODEL vazio — impossível detectar dispositivo")
            return null
        }
        val oneUiVersion = detectOneUiVersion()

        Log.d(TAG, "Modelo: $model, One UI detectada: $oneUiVersion")

        val baseBranch = "$model-Oneui$oneUiVersion"

        DeviceList.findByBranch(baseBranch)?.let {
            Log.d(TAG, "✅ Branch exato: ${it.branch}")
            return it
        }

        val bitSuffixes = listOf("bit4", "bit5", "bit7", "bit8", "bit9", "bitA", "bitK")
        for (suffix in bitSuffixes) {
            DeviceList.findByBranch("$baseBranch-$suffix")?.let {
                Log.d(TAG, "✅ Branch com sufixo: ${it.branch}")
                return it
            }
        }

        DeviceList.devices.firstOrNull { it.branch.startsWith("$model-") }?.let {
            Log.d(TAG, "⚠️ Fallback (prefixo): ${it.branch}")
            return it
        }

        Log.w(TAG, "❌ Nenhum branch encontrado para $model")
        return null
    }

    fun describeDetection(): String {
        val modelRaw = Build.MODEL ?: "?"
        val model = getCurrentModel()
        val oneuiRaw = getSystemProperty("ro.build.version.oneui") ?: "?"
        val majorRaw = getSystemProperty("ro.build.version.oneui.major") ?: "?"
        val minorRaw = getSystemProperty("ro.build.version.oneui.minor") ?: "?"
        val normalized = detectOneUiVersion()
        val device = detect()
        return """
            Build.MODEL=$modelRaw
            Modelo limpo=$model
            ro.build.version.oneui=$oneuiRaw
            ro.build.version.oneui.major=$majorRaw
            ro.build.version.oneui.minor=$minorRaw
            One UI normalizada=$normalized
            Branch detectado=${device?.branch ?: "não encontrado"}
        """.trimIndent()
    }

    private fun detectOneUiVersion(): String {
        val major = getSystemProperty("ro.build.version.oneui.major")
        val minor = getSystemProperty("ro.build.version.oneui.minor")
        if (!major.isNullOrBlank()) {
            val raw = "$major.${minor ?: "0"}"
            Log.d(TAG, "One UI (major/minor): $raw")
            return normalizeOneUi(raw)
        }

        val oneUi = getSystemProperty("ro.build.version.oneui")
        if (!oneUi.isNullOrBlank()) {
            Log.d(TAG, "One UI (single): $oneUi")
            return normalizeOneUi(oneUi)
        }

        Log.w(TAG, "One UI não detectada — usando fallback '7'")
        return "7"
    }

    private fun normalizeOneUi(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return "7"

        if (trimmed.all { it.isDigit() }) {
            return when (trimmed.length) {
                1 -> trimmed
                2 -> trimmed
                else -> {
                    val major = trimmed.substring(0, 1)
                    val minor = trimmed.substring(1, 2)
                    when {
                        minor == "0" -> major
                        else -> "$major.$minor"
                    }
                }
            }
        }

        if (trimmed.contains(".")) {
            val parts = trimmed.split(".")
            if (parts.isEmpty()) return "7"
            if (parts.size == 1) return parts[0]

            val major = parts[0]
            val minor = parts[1].toIntOrNull() ?: 0

            return when {
                minor == 0 -> major
                major == "8" && minor == 5 -> "8.5"
                major == "6" && minor == 1 -> "6.1"
                major == "6" && minor == 0 -> "6.0"
                else -> "$major.$minor"
            }
        }

        return trimmed
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val get = clazz.getMethod("get", String::class.java)
            get.invoke(null, key) as? String
        } catch (_: Exception) {
            null
        }
    }
}