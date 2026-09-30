package com.br.samsung_kernelsu.data

import com.google.gson.annotations.SerializedName


data class DeviceCodeResponse(
    @SerializedName("device_code") val deviceCode: String,
    @SerializedName("user_code") val userCode: String,
    @SerializedName("verification_uri") val verificationUri: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("interval") val interval: Int
)

data class AccessTokenResponse(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("scope") val scope: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_description") val errorDescription: String?
)

data class WorkflowDispatchRequest(
    val ref: String,
    val inputs: Map<String, String>
)

data class WorkflowRun(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("run_number") val runNumber: Long = 0,
    @SerializedName("head_branch") val headBranch: String? = null,
    @SerializedName("event") val event: String? = null
)

data class WorkflowRunsResponse(
    @SerializedName("workflow_runs") val workflowRuns: List<WorkflowRun>
)

data class WorkflowStep(
    val name: String,
    val status: String,
    val conclusion: String?,
    val number: Int
)

data class WorkflowJob(
    val id: Long,
    @SerializedName("run_id") val runId: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("completed_at") val completedAt: String?,
    val steps: List<WorkflowStep> = emptyList()
)

data class WorkflowJobsResponse(
    @SerializedName("total_count") val totalCount: Int,
    val jobs: List<WorkflowJob>
)

data class Artifact(
    val id: Long,
    val name: String,
    @SerializedName("archive_download_url") val archiveDownloadUrl: String,
    @SerializedName("expired") val expired: Boolean,
    @SerializedName("size_in_bytes") val sizeInBytes: Long = 0
)

data class ArtifactsResponse(
    val artifacts: List<Artifact>
)

data class GitHubUser(
    val login: String,
    val name: String?,
    @SerializedName("avatar_url") val avatarUrl: String?
)

data class GitHubRepo(
    val id: Long,
    val name: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("default_branch") val defaultBranch: String,
    val private: Boolean,
    val fork: Boolean = false
)

data class FileContent(
    val name: String,
    val path: String,
    val sha: String,
    val content: String?,
    val encoding: String?
)

data class CreateForkRequest(
    val name: String? = null,
    @SerializedName("default_branch_only") val defaultBranchOnly: Boolean = true
)

data class CreateOrUpdateFileRequest(
    val message: String,
    val content: String,
    val branch: String,
    val sha: String? = null
)

data class ReleaseAsset(
    val id: Long,
    val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String,
    val size: Long,
    @SerializedName("created_at") val createdAt: String
)

data class ReleaseInfo(
    val id: Long,
    @SerializedName("tag_name") val tagName: String,
    val name: String,
    @SerializedName("created_at") val createdAt: String,
    val draft: Boolean,
    val assets: List<ReleaseAsset>
)

enum class BuildMode { WILDKERNELS, CUSTOM }
enum class FileSource { NEW_UPLOAD, REUSE_EXISTING }

data class BuildConfig(

    val mode: BuildMode = BuildMode.WILDKERNELS,

    val releaseType: String = "Actions",
    val lto: String = "default",
    val deviceBranch: String = DeviceList.default.branch,
    val deviceBuildType: String = DeviceList.default.buildType,

    val enableBootRepack: Boolean = false,

    val deviceModel: String = "",
    val ksuVariant: String = "KernelSU-Next",

    val osrcSource: FileSource = FileSource.NEW_UPLOAD,
    val osrcFilePath: String = "",
    val osrcReuseUrl: String = "",

    val bootSource: FileSource = FileSource.NEW_UPLOAD,
    val bootImagePath: String = "",
    val bootReuseUrl: String = "",

    val ksun: Boolean = true,
    val susfs: Boolean = true,
    val nomount: Boolean = true,
    val bbg: Boolean = true,
    val unicodefix: Boolean = true,
    val zeromount: Boolean = false,
    val droidspace: Boolean = false,
    val ntsync: Boolean = false,
    val bbrv3: Boolean = false,
    val cache: Boolean = true,
    val ipv6Nat: Boolean = false,
    val optimization: Boolean = false,
    val ttl: Boolean = false,

    val fixKsun: Boolean = false
)

data class UserSession(
    val token: String,
    val login: String,
    val name: String?,
    val avatarUrl: String?,
    val repoName: String,
    val defaultBranch: String
)

sealed class AuthState {
    object Idle : AuthState()
    object RequestingCode : AuthState()
    data class DeviceCodeReady(val userCode: String, val verificationUri: String) : AuthState()
    data class Polling(val userCode: String, val verificationUri: String, val attempt: Int = 0) : AuthState()
    data class ProvisioningStep(val message: String) : AuthState()
    data class Success(val session: UserSession) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class BuildState {
    object Idle : BuildState()
    object Dispatching : BuildState()

    data class Uploading(
        val message: String,
        val progress: Float? = null,
        val bytesUploaded: Long = 0,
        val bytesTotal: Long = 0,
        val fileName: String = ""
    ) : BuildState()

    data class Running(val runId: Long, val status: String) : BuildState()
    data class Completed(
        val runId: Long,
        val conclusion: String?,
        val artifacts: List<Artifact>
    ) : BuildState()
    data class Error(val message: String) : BuildState()
}