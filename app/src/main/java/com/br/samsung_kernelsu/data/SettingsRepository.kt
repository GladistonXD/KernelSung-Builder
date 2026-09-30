package com.br.samsung_kernelsu.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "kernelsu_next_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_CLIENT_ID = "github_client_id"
        private const val KEY_REPO_NAME = "repo_name"

        private const val KEY_TOKEN = "session_token"
        private const val KEY_LOGIN = "session_login"
        private const val KEY_NAME = "session_name"
        private const val KEY_AVATAR = "session_avatar"
        private const val KEY_SESSION_REPO = "session_repo"
        private const val KEY_SESSION_BRANCH = "session_branch"

        const val DEFAULT_REPO_NAME = "kernelsung-builder"
    }

    fun saveClientId(clientId: String) {
        prefs.edit().putString(KEY_CLIENT_ID, clientId.trim()).apply()
    }

    fun saveLanguage(code: String) {
        prefs.edit().putString("app_language", code).apply()
    }

    fun getLanguage(): String {
        return prefs.getString("app_language", "en") ?: "en"
    }

    fun getClientId(): String = prefs.getString(KEY_CLIENT_ID, "") ?: ""

    fun hasClientId(): Boolean = getClientId().isNotBlank()

    fun saveRepoName(repoName: String) {
        prefs.edit().putString(KEY_REPO_NAME, repoName.trim()).apply()
    }

    fun getRepoName(): String =
        prefs.getString(KEY_REPO_NAME, DEFAULT_REPO_NAME) ?: DEFAULT_REPO_NAME

    fun saveSession(session: UserSession) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_LOGIN, session.login)
            .putString(KEY_NAME, session.name ?: "")
            .putString(KEY_AVATAR, session.avatarUrl ?: "")
            .putString(KEY_SESSION_REPO, session.repoName)
            .putString(KEY_SESSION_BRANCH, session.defaultBranch)
            .apply()
    }

    fun loadSession(): UserSession? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val login = prefs.getString(KEY_LOGIN, null) ?: return null
        val repo = prefs.getString(KEY_SESSION_REPO, null) ?: return null
        val branch = prefs.getString(KEY_SESSION_BRANCH, null) ?: return null
        val name = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
        val avatar = prefs.getString(KEY_AVATAR, null)?.takeIf { it.isNotBlank() }

        return UserSession(
            token = token,
            login = login,
            name = name,
            avatarUrl = avatar,
            repoName = repo,
            defaultBranch = branch
        )
    }

    fun hasSession(): Boolean = prefs.getString(KEY_TOKEN, null) != null

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_LOGIN)
            .remove(KEY_NAME)
            .remove(KEY_AVATAR)
            .remove(KEY_SESSION_REPO)
            .remove(KEY_SESSION_BRANCH)
            .apply()
    }

    private val KEY_LAST_RUN_ID = "last_run_id"
    private val KEY_LAST_RUN_STARTED_AT = "last_run_started_at"

    fun saveLastRunId(runId: Long) {
        prefs.edit()
            .putLong(KEY_LAST_RUN_ID, runId)
            .putLong(KEY_LAST_RUN_STARTED_AT, System.currentTimeMillis())
            .apply()
    }

    fun getLastRunId(): Long = prefs.getLong(KEY_LAST_RUN_ID, -1L)

    fun getLastRunStartedAt(): Long = prefs.getLong(KEY_LAST_RUN_STARTED_AT, 0L)

    fun clearLastRun() {
        prefs.edit()
            .remove(KEY_LAST_RUN_ID)
            .remove(KEY_LAST_RUN_STARTED_AT)
            .apply()
    }
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}