package com.br.samsung_kernelsu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.br.samsung_kernelsu.data.*
import com.br.samsung_kernelsu.service.OAuthKeepAliveService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = GitHubRepository()
    private val settings = SettingsRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _savedClientId = MutableStateFlow(settings.getClientId())
    val savedClientId: StateFlow<String> = _savedClientId

    private val _currentSession = MutableStateFlow(settings.loadSession())
    val currentSession: StateFlow<UserSession?> = _currentSession


    fun startDeviceFlow(clientId: String, repoName: String) {
        val trimmedClientId = clientId.trim()

        if (trimmedClientId.isBlank()) {
            _authState.value = AuthState.Error("Informe um Client ID válido")
            return
        }

        settings.saveClientId(trimmedClientId)
        _savedClientId.value = trimmedClientId

        OAuthKeepAliveService.start(appContext)

        viewModelScope.launch {
            try {
                _authState.value = AuthState.RequestingCode
                val deviceCode = repository.requestDeviceCode(trimmedClientId)

                _authState.value = AuthState.DeviceCodeReady(
                    userCode = deviceCode.userCode,
                    verificationUri = deviceCode.verificationUri
                )

                pollForToken(
                    clientId = trimmedClientId,
                    deviceCode = deviceCode.deviceCode,
                    intervalSeconds = deviceCode.interval.coerceAtLeast(5),
                    userCode = deviceCode.userCode,
                    verificationUri = deviceCode.verificationUri
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Erro desconhecido")
            } finally {
                OAuthKeepAliveService.stop(appContext)
            }
        }
    }

    private suspend fun pollForToken(
        clientId: String,
        deviceCode: String,
        intervalSeconds: Int,
        userCode: String,
        verificationUri: String
    ) {
        var attempt = 0
        var currentInterval = intervalSeconds

        while (true) {
            _authState.value = AuthState.Polling(
                userCode = userCode,
                verificationUri = verificationUri,
                attempt = attempt
            )

            delay(currentInterval * 1000L)
            attempt++

            try {
                val response = repository.pollAccessToken(clientId, deviceCode)

                if (response.accessToken != null) {
                    provisionUserEnvironment(response.accessToken)
                    return
                }

                when (response.error) {
                    "authorization_pending" -> { /* continua */ }
                    "slow_down" -> currentInterval += 5
                    "expired_token" -> {
                        _authState.value = AuthState.Error("Código expirado. Tente novamente.")
                        return
                    }
                    "access_denied" -> {
                        _authState.value = AuthState.Error("Acesso negado pelo usuário.")
                        return
                    }
                    else -> {
                        if (response.error != null) {
                            _authState.value = AuthState.Error(
                                response.errorDescription ?: response.error
                            )
                            return
                        }
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Erro no polling")
                return
            }
        }
    }

    private suspend fun provisionUserEnvironment(token: String) {
        try {
            _authState.value = AuthState.ProvisioningStep("Obtendo usuário...")
            val user = repository.getAuthenticatedUser(token)

            _authState.value = AuthState.ProvisioningStep("Preparando fork do WildKernels...")
            val fork = repository.ensureForkExists(token, user.login)

            _authState.value = AuthState.ProvisioningStep("Habilitando Actions no fork...")
            try {
                repository.enableActionsInFork(token, user.login, fork.name)
            } catch (_: Exception) {
            }

            delay(5_000)

            _authState.value = AuthState.ProvisioningStep("Tudo pronto!")

            val session = UserSession(
                token = token,
                login = user.login,
                name = user.name,
                avatarUrl = user.avatarUrl,
                repoName = fork.name,
                defaultBranch = fork.defaultBranch
            )

            settings.saveSession(session)
            _currentSession.value = session

            _authState.value = AuthState.Success(session)
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Falha ao preparar ambiente: ${e.message}")
        }
    }

    fun logout() {
        settings.clearSession()
        _currentSession.value = null
        _authState.value = AuthState.Idle
    }

    fun reset() {
        _authState.value = AuthState.Idle
    }

    fun clearSavedClientId() {
        settings.saveClientId("")
        _savedClientId.value = ""
    }
}