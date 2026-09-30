package com.br.samsung_kernelsu.data

data class DeviceInfo(
    val branch: String,
    val displayName: String,
    val buildType: String
)

object DeviceList {
    val devices: List<DeviceInfo> = listOf(
        // ========== Galaxy S25 ==========
        DeviceInfo("SM-S938B-Oneui8.5", "Galaxy S25 Ultra • One UI 8.5", "Bazel"),
        DeviceInfo("SM-S938B-Oneui7",   "Galaxy S25 Ultra • One UI 7",   "Bazel"),
        DeviceInfo("SM-S931B-Oneui8.5", "Galaxy S25 • One UI 8.5",       "Bazel"),
        DeviceInfo("SM-S931B-Oneui8",   "Galaxy S25 • One UI 8",         "Bazel"),

        // ========== Galaxy S24 ==========
        DeviceInfo("SM-S928B-Oneui8.5",    "Galaxy S24 Ultra • One UI 8.5",     "Bazel"),
        DeviceInfo("SM-S928B-Oneui8-bit5", "Galaxy S24 Ultra • One UI 8 (bit5)", "Bazel"),
        DeviceInfo("SM-S928B-Oneui8-bit4", "Galaxy S24 Ultra • One UI 8 (bit4)", "Bazel"),
        DeviceInfo("SM-S928B-Oneui7",      "Galaxy S24 Ultra • One UI 7",       "Bazel"),
        DeviceInfo("SM-S928B-Oneui6.1",    "Galaxy S24 Ultra • One UI 6.1",     "Bazel"),
        DeviceInfo("SM-S926B-Oneui8.5",    "Galaxy S24+ • One UI 8.5",          "Bazel"),
        DeviceInfo("SM-S926B-Oneui8",      "Galaxy S24+ • One UI 8",            "Bazel"),
        DeviceInfo("SM-S926B-Oneui7-bit9", "Galaxy S24+ • One UI 7 (bit9)",     "Bazel"),
        DeviceInfo("SM-S926B-Oneui7-bit8", "Galaxy S24+ • One UI 7 (bit8)",     "Bazel"),
        DeviceInfo("SM-S926B-Oneui7-bit7", "Galaxy S24+ • One UI 7 (bit7)",     "Bazel"),

        // ========== Galaxy S23 ==========
        DeviceInfo("SM-S918B-Oneui8.5", "Galaxy S23 Ultra • One UI 8.5", "Bazel"),
        DeviceInfo("SM-S918B-Oneui8",   "Galaxy S23 Ultra • One UI 8",   "Bazel"),
        DeviceInfo("SM-S918B-Oneui7",   "Galaxy S23 Ultra • One UI 7",   "Bazel"),
        DeviceInfo("SM-S9180-Oneui6.1", "Galaxy S23 Ultra (CN) • One UI 6.1", "Bazel"),
        DeviceInfo("SM-S916B-Oneui8",   "Galaxy S23+ • One UI 8",        "Bazel"),
        DeviceInfo("SM-S911B-Oneui8.5",       "Galaxy S23 • One UI 8.5",          "Bazel"),
        DeviceInfo("SM-S911B-Oneui8",         "Galaxy S23 • One UI 8",            "Bazel"),
        DeviceInfo("SM-S911B-Oneui8-bit8",    "Galaxy S23 • One UI 8 (bit8)",     "Bazel"),
        DeviceInfo("SM-S911B-Oneui7",         "Galaxy S23 • One UI 7",            "Bazel"),
        DeviceInfo("SM-S911B-Oneui6.1-bit8",  "Galaxy S23 • One UI 6.1 (bit8)",   "Bazel"),
        DeviceInfo("SM-S911B-Oneui6.1-bit7",  "Galaxy S23 • One UI 6.1 (bit7)",   "Bazel"),

        // ========== Galaxy S22 ==========
        DeviceInfo("SM-S908E-Oneui8",        "Galaxy S22 Ultra • One UI 8",       "make"),
        DeviceInfo("SM-S908B-Oneui8-bitK",   "Galaxy S22 Ultra • One UI 8 (bitK)","make"),
        DeviceInfo("SM-S906E-Oneui8",        "Galaxy S22+ • One UI 8",            "make"),
        DeviceInfo("SM-S901E-Oneui8",        "Galaxy S22 • One UI 8",             "make"),

        // ========== Galaxy Z Fold ==========
        DeviceInfo("SM-F946B-Oneui8", "Galaxy Z Fold5 • One UI 8",      "Bazel"),
        DeviceInfo("SM-F946N-Oneui7", "Galaxy Z Fold5 (KR) • One UI 7", "Bazel"),
        DeviceInfo("SM-F741B-Oneui8", "Galaxy Z Flip6 • One UI 8",      "Bazel"),

        // ========== Galaxy Z Flip ==========
        DeviceInfo("SM-F731B-Oneui8.5", "Galaxy Z Flip5 • One UI 8.5", "Bazel"),
        DeviceInfo("SM-F731B-Oneui8",   "Galaxy Z Flip5 • One UI 8",   "Bazel"),
        DeviceInfo("SM-F731B-Oneui7",   "Galaxy Z Flip5 • One UI 7",   "Bazel"),
        DeviceInfo("SM-F731B-Oneui6",   "Galaxy Z Flip5 • One UI 6",   "Bazel"),

        // ========== Galaxy A ==========
        DeviceInfo("SM-A556B-Oneui7-bitA",  "Galaxy A55 • One UI 7 (bitA)", "Bazel"),
        DeviceInfo("SM-A556E-Oneui7-bitA",  "Galaxy A55 • One UI 7 (bitA)", "Bazel"),
        DeviceInfo("SM-A546B-Oneui8",       "Galaxy A54 • One UI 8",        "make"),
        DeviceInfo("SM-A546B-Oneui7",       "Galaxy A54 • One UI 7",        "make"),
        DeviceInfo("SM-A546B-Oneui6.0",     "Galaxy A54 • One UI 6.0",      "make"),
        DeviceInfo("SM-A546E-Oneui8",       "Galaxy A54 • One UI 8",        "make"),
        DeviceInfo("SM-A546E-Oneui7",       "Galaxy A54 • One UI 7",        "make"),
        DeviceInfo("SM-A356E-Oneui8.5",     "Galaxy A35 • One UI 8.5",      "make"),
        DeviceInfo("SM-A356E-Oneui8",       "Galaxy A35 • One UI 8",        "make"),
        DeviceInfo("SM-A166B-Oneui7",       "Galaxy A16 • One UI 7",        "make"),
        DeviceInfo("SM-A075M-Oneui7",       "Galaxy A07 • One UI 7",        "make"),
        DeviceInfo("SM-A057M-Oneui7",       "Galaxy A05s • One UI 7",       "Bazel"),
        DeviceInfo("SM-A055M-Oneui7",       "Galaxy A05 • One UI 7",        "Bazel"),
        DeviceInfo("SM-A055F-Oneui7",       "Galaxy A05 • One UI 7",        "Bazel"),

        // ========== Galaxy M ==========
        DeviceInfo("SM-M556B-Oneui8",       "Galaxy M55 • One UI 8", "make"),
        DeviceInfo("SM-M146B-Oneui7-ADYJ2", "Galaxy M14 • One UI 7", "make"),

        // ========== Galaxy S FE ==========
        DeviceInfo("SM-S711B-Oneui8.5", "Galaxy S23 FE • One UI 8.5", "make"),

        // ========== Galaxy Tab ==========
        DeviceInfo("SM-X926B-Oneui6.1",      "Galaxy Tab S10 Ultra • One UI 6.1",     "Bazel"),
        DeviceInfo("SM-X916B",               "Galaxy Tab S9 Ultra",                   "Bazel"),
        DeviceInfo("SM-X800-Oneui8",         "Galaxy Tab S9+ • One UI 8",             "make"),
        DeviceInfo("SM-X710-Oneui6.1.1",     "Galaxy Tab S9 • One UI 6.1.1",          "Bazel"),
        DeviceInfo("SM-X610-Oneui7",         "Galaxy Tab S6 Lite • One UI 7",         "make"),
        DeviceInfo("SM-X610-Oneui6.1.1-bit7","Galaxy Tab S6 Lite • One UI 6.1.1",     "make")
    )

    val default: DeviceInfo = devices.firstOrNull { it.branch == "SM-S928B-Oneui7" }
        ?: devices.first()

    fun findByBranch(branch: String): DeviceInfo? = devices.firstOrNull { it.branch == branch }
}