package com.br.samsung_kernelsu.i18n

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flag: String
) {
    ENGLISH("en", "English", "🇺🇸"),
    PORTUGUESE("pt-BR", "Português (Brasil)", "🇧🇷");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            values().firstOrNull { it.code == code } ?: ENGLISH
    }
}