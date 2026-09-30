package com.br.samsung_kernelsu.i18n
import androidx.compose.runtime.staticCompositionLocalOf

val LocalStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }
val LocalLanguage = staticCompositionLocalOf<AppLanguage> { AppLanguage.ENGLISH }