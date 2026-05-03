package com.example.mocklyapp.data.artifact

import android.util.Log
import com.example.mocklyapp.data.artifact.remote.ArtifactApi
import com.example.mocklyapp.data.artifact.remote.ArtifactResponseDto
import com.example.mocklyapp.data.artifact.remote.CompleteUploadRequestDto
import com.example.mocklyapp.data.artifact.remote.RequestUploadRequestDto
import com.example.mocklyapp.domain.artifact.ArtifactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.net.URL

class ArtifactRepositoryImpl(
    private val api: ArtifactApi
) : ArtifactRepository {

    private val uploadClient = OkHttpClient()

    override suspend fun uploadSessionAudio(
        sessionId: String,
        file: File,
        durationSec: Int
    ): Int = withContext(Dispatchers.IO) {
        Log.d("ArtifactRepo", "uploadSessionAudio START")
        Log.d(
            "ArtifactRepo",
            "sessionId=$sessionId, file=${file.absolutePath}, size=${file.length()}, durationSec=$durationSec"
        )

        if (!file.exists()) {
            throw UploadException.LocalFileError("Audio file does not exist: ${file.absolutePath}")
        }

        if (file.length() <= 0L) {
            throw UploadException.LocalFileError("Audio file is empty: ${file.absolutePath}")
        }

        val contentType = "audio/mp4"

        val uploadInfo = try {
            api.requestUpload(
                sessionId = sessionId,
                body = RequestUploadRequestDto(
                    type = "AUDIO_MIXED",
                    fileName = file.name,
                    fileSizeBytes = file.length(),
                    contentType = contentType
                )
            )
        } catch (e: HttpException) {
            Log.e("ArtifactRepo", "requestUpload failed: HTTP ${e.code()}", e)
            throw UploadException.BackendError(e.code(), getBackendMessage(e))
        } catch (e: Exception) {
            Log.e("ArtifactRepo", "requestUpload failed", e)
            throw UploadException.NetworkError(e)
        }

        Log.d(
            "ArtifactRepo",
            "requestUpload OK: artifactId=${uploadInfo.artifactId}, objectName=${uploadInfo.objectName}"
        )

        uploadFileToMinio(
            uploadUrl = uploadInfo.uploadUrl,
            file = file,
            contentType = contentType
        )

        val safeDuration = durationSec.coerceAtLeast(1)

        val completeRequest = CompleteUploadRequestDto(
            fileSizeBytes = file.length(),
            durationSec = safeDuration
        )

        val completed = completeUploadWithFallback(
            sessionId = sessionId,
            returnedArtifactId = uploadInfo.artifactId,
            objectName = uploadInfo.objectName,
            request = completeRequest
        )

        Log.d("ArtifactRepo", "uploadSessionAudio FINISHED: artifact=${completed.id}")

        safeDuration
    }

    private suspend fun completeUploadWithFallback(
        sessionId: String,
        returnedArtifactId: String,
        objectName: String,
        request: CompleteUploadRequestDto
    ): ArtifactResponseDto {
        try {
            val response = api.completeUpload(
                sessionId = sessionId,
                artifactId = returnedArtifactId,
                body = request
            )

            Log.d("ArtifactRepo", "completeUpload OK with returned artifactId=$returnedArtifactId")

            return response
        } catch (e: HttpException) {
            val message = getBackendMessage(e)

            Log.e(
                "ArtifactRepo",
                "completeUpload failed with returned artifactId=$returnedArtifactId: HTTP ${e.code()}, message=$message",
                e
            )

            val isArtifactNotFound = e.code() == 404 && message.contains("Artifact not found", ignoreCase = true)

            if (!isArtifactNotFound) {
                throw UploadException.BackendError(e.code(), message)
            }
        }

        Log.w(
            "ArtifactRepo",
            "Trying completeUpload fallback. Backend returned artifactId=$returnedArtifactId but cannot find it."
        )

        delay(300)

        val actualArtifact = findUploadedArtifactByObjectName(
            sessionId = sessionId,
            objectName = objectName
        )

        Log.w(
            "ArtifactRepo",
            "Fallback found real artifactId=${actualArtifact.id} for objectName=$objectName"
        )

        return try {
            val response = api.completeUpload(
                sessionId = sessionId,
                artifactId = actualArtifact.id,
                body = request
            )

            Log.d("ArtifactRepo", "completeUpload OK with fallback artifactId=${actualArtifact.id}")

            response
        } catch (e: HttpException) {
            Log.e(
                "ArtifactRepo",
                "completeUpload fallback failed: HTTP ${e.code()}, message=${getBackendMessage(e)}",
                e
            )
            throw UploadException.BackendError(e.code(), getBackendMessage(e))
        } catch (e: Exception) {
            Log.e("ArtifactRepo", "completeUpload fallback failed", e)
            throw UploadException.NetworkError(e)
        }
    }

    private suspend fun findUploadedArtifactByObjectName(
        sessionId: String,
        objectName: String
    ): ArtifactResponseDto {
        val artifacts = try {
            api.listArtifacts(sessionId)
        } catch (e: HttpException) {
            Log.e("ArtifactRepo", "listArtifacts failed: HTTP ${e.code()}", e)
            throw UploadException.BackendError(e.code(), getBackendMessage(e))
        } catch (e: Exception) {
            Log.e("ArtifactRepo", "listArtifacts failed", e)
            throw UploadException.NetworkError(e)
        }

        Log.d("ArtifactRepo", "listArtifacts returned ${artifacts.size} items")

        return artifacts.firstOrNull { artifact ->
            val storageUrl = artifact.storageUrl.orEmpty()
            storageUrl == objectName ||
                    storageUrl.endsWith(objectName) ||
                    objectName.endsWith(storageUrl)
        } ?: throw UploadException.BackendError(
            code = 404,
            message = "Uploaded artifact exists in MinIO but was not found in backend artifact list. objectName=$objectName"
        )
    }

    private fun uploadFileToMinio(
        uploadUrl: String,
        file: File,
        contentType: String
    ) {
        val emulatorUrl = buildEmulatorUploadUrl(uploadUrl)
        val hostHeader = buildOriginalHostHeader(uploadUrl)

        val putRequest = Request.Builder()
            .url(emulatorUrl)
            .header("Host", hostHeader)
            .header("Content-Type", contentType)
            .put(file.asRequestBody(contentType.toMediaType()))
            .build()

        val putResponse = try {
            uploadClient.newCall(putRequest).execute()
        } catch (e: Exception) {
            Log.e("ArtifactRepo", "PUT upload failed", e)
            throw UploadException.NetworkError(e)
        }

        putResponse.use { response ->
            val responseBody = response.body?.string()

            Log.d(
                "ArtifactRepo",
                "PUT response: code=${response.code}, message=${response.message}, body=$responseBody"
            )

            if (!response.isSuccessful) {
                throw UploadException.MinioError(
                    code = response.code,
                    message = "${response.message} - $responseBody"
                )
            }
        }
    }

    private fun buildEmulatorUploadUrl(rawUrl: String): String {
        val original = URL(rawUrl)

        if (original.host == "minio") {
            return rawUrl.replace(
                "${original.protocol}://${original.host}:${original.port}",
                "http://minio.iness.app"
            )
        }

        return rawUrl
    }

    private fun buildOriginalHostHeader(rawUrl: String): String {
        val original = URL(rawUrl)

        val port = if (original.port != -1) {
            original.port
        } else {
            original.defaultPort
        }

        return if (port != 80 && port != 443 && port != -1) {
            "${original.host}:$port"
        } else {
            original.host
        }
    }

    private fun getBackendMessage(e: HttpException): String {
        return try {
            val body = e.response()?.errorBody()?.string()
            JSONObject(body ?: "").optString("message").takeIf { it.isNotBlank() }
                ?: e.message()
                ?: "Unknown backend error"
        } catch (_: Exception) {
            e.message() ?: "Unknown backend error"
        }
    }
}

sealed class UploadException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class LocalFileError(message: String) : UploadException(message)

    class NetworkError(cause: Throwable) :
        UploadException("Network error: ${cause.message}", cause)

    class MinioError(val code: Int, message: String) :
        UploadException("MinIO error $code: $message")

    class BackendError(val code: Int, message: String) :
        UploadException("Backend error $code: $message")
}