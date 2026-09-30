package com.br.samsung_kernelsu.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface GitHubApi {

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("https://github.com/login/device/code")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String = "repo workflow"
    ): Response<ResponseBody>

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("https://github.com/login/oauth/access_token")
    suspend fun pollAccessToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
    ): Response<ResponseBody>


    @Headers("Accept: application/vnd.github+json")
    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") auth: String
    ): Response<GitHubUser>


    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRepo>

    @Headers("Accept: application/vnd.github+json")
    @POST("repos/{owner}/{repo}/forks")
    suspend fun createFork(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateForkRequest = CreateForkRequest()
    ): Response<GitHubRepo>

    @Headers("Accept: application/vnd.github+json")
    @POST("repos/{owner}/{repo}/merge-upstream")
    suspend fun syncFork(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/actions/permissions")
    suspend fun getActionsPermissions(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Map<String, Any>>

    @Headers("Accept: application/vnd.github+json")
    @PUT("repos/{owner}/{repo}/actions/permissions")
    suspend fun setActionsPermissions(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: Map<String, Any>
    ): Response<Unit>

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun listWorkflows(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Map<String, Any>>

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String
    ): Response<FileContent>

    @Headers("Accept: application/vnd.github+json")
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body body: CreateOrUpdateFileRequest
    ): Response<ResponseBody>

    @Headers("Accept: application/vnd.github+json")
    @POST("repos/{owner}/{repo}/actions/workflows/{workflowId}/dispatches")
    suspend fun dispatchWorkflow(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflowId") workflowId: String,
        @Body body: WorkflowDispatchRequest
    ): Response<Unit>


    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getWorkflowRuns(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 20
    ): Response<WorkflowRunsResponse>

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/actions/runs/{runId}")
    suspend fun getWorkflowRun(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long
    ): Response<WorkflowRun>

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/actions/runs/{runId}/jobs")
    suspend fun getWorkflowJobs(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long,
        @Query("per_page") perPage: Int = 50
    ): Response<WorkflowJobsResponse>

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/actions/runs/{runId}/artifacts")
    suspend fun getArtifacts(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long
    ): Response<ArtifactsResponse>


    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/releases")
    suspend fun listReleases(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30
    ): Response<List<ReleaseInfo>>
}