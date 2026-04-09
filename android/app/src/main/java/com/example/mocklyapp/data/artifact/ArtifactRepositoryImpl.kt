package com.example.mocklyapp.data.artifact

import android.util.Log
import com.example.mocklyapp.data.artifact.remote.ArtifactApi
import com.example.mocklyapp.data.artifact.remote.CompleteUploadRequestDto
import com.example.mocklyapp.data.artifact.remote.RequestUploadRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.net.URL

class ArtifactRepositoryImpl(
    private val api: ArtifactApi
) : com.example.mocklyapp.domain.artifact.ArtifactRepository {

    private val uploadClient = OkHttpClient()

    override suspend fun uploadSessionAudio(
        sessionId: String,
        file: File,
        durationSec: Int
    ): Int = withContext(Dispatchers.IO) {

        Log.d("ArtifactRepo", "=== uploadSessionAudio START ===")
        Log.d(
            "ArtifactRepo",
            "sessionId=$sessionId, file=${file.absolutePath}, size=${file.length()}, durationSec=$durationSec"
        )

        val requestUpload = RequestUploadRequestDto(
            type = "AUDIO_MIXED",
            fileName = file.name,
            fileSizeBytes = file.length(),
            contentType = "audio/mpeg"
        )

        // 1) presigned URL от бэка
        val resp = try {
            api.requestUpload(
                sessionId = sessionId,
                body = requestUpload
            )
        } catch (e: HttpException) {
            Log.e("ArtifactRepo", "requestUpload failed: HTTP ${e.code()}: ${e.message()}", e)
            throw UploadException.BackendError(e.code(), e.message() ?: "Unknown error")
        } catch (e: Exception) {
            Log.e("ArtifactRepo", "requestUpload failed", e)
            throw UploadException.NetworkError(e)
        }

        Log.d(
            "ArtifactRepo",
            "requestUpload -> artifactId=${resp.artifactId}, objectName=${resp.objectName}, expiresIn=${resp.expiresInSeconds}s"
        )

        val rawUrl = resp.uploadUrl
        Log.d("ArtifactRepo", "Raw uploadUrl: $rawUrl")

        // 2) собираем Host для MinIO и URL для эмулятора
        val original = URL(rawUrl)
        val originalHostHeader = buildString {
            append(original.host)
            val port = if (original.port != -1) original.port else original.defaultPort
            if (port != 80 && port != 443 && port != -1) {
                append(":")
                append(port)
            }
        }

        val emulatorUrl = rawUrl.replace(
            "${original.protocol}://${original.host}",
            "${original.protocol}://10.0.2.2"
        )

        Log.d("ArtifactRepo", "Emulator uploadUrl: $emulatorUrl")
        Log.d("ArtifactRepo", "Host header: $originalHostHeader")

        val mediaType = "audio/mpeg".toMediaType()
        val body = file.asRequestBody(mediaType)

        // 3) PUT в MinIO по presigned URL
        Log.d("ArtifactRepo", "Sending PUT to MinIO for artifactId=${resp.artifactId}")
        val putRequest = Request.Builder()
            .url(emulatorUrl)
            .header("Host", originalHostHeader)
            .put(body)
            .build()

        val putResponse = try {
            uploadClient.newCall(putRequest).execute()
        } catch (e: Exception) {
            Log.e("ArtifactRepo", "PUT request failed for artifactId=${resp.artifactId}", e)
            throw UploadException.NetworkError(e)
        }

        putResponse.use { respPut ->
            val code = respPut.code
            val msg = respPut.message
            val bodyStr = respPut.body?.string()

            Log.d("ArtifactRepo", "PUT response: $code $msg, body=$bodyStr")

            if (!respPut.isSuccessful) {
                throw UploadException.MinioError(code, "$msg - $bodyStr")
            }
        }

        Log.d("ArtifactRepo", "PUT successful, file uploaded to MinIO")

        // 4) completeUpload на бэке
        val completeReq = CompleteUploadRequestDto(
            fileSizeBytes = file.length(),
            durationSec = durationSec
        )

        Log.d(
            "ArtifactRepo",
            "Calling completeUpload(sessionId=$sessionId, artifactId=${resp.artifactId})"
        )

        try {
            val response = api.completeUpload(
                sessionId = sessionId,
                artifactId = resp.artifactId,
                body = completeReq
            )
            Log.d(
                "ArtifactRepo",
                "completeUpload OK for artifactId=${resp.artifactId}, response=$response"
            )
        } catch (e: HttpException) {
            Log.e(
                "ArtifactRepo",
                "completeUpload failed: HTTP ${e.code()}: ${e.message()}",
                e
            )
            throw UploadException.BackendError(e.code(), e.message() ?: "Unknown error")
        } catch (e: Exception) {
            Log.e("ArtifactRepo", "completeUpload failed", e)
            throw UploadException.NetworkError(e)
        }

        Log.d(
            "ArtifactRepo",
            "=== uploadSessionAudio FINISHED successfully for artifactId=${resp.artifactId} ==="
        )

        durationSec
    }
}

sealed class UploadException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    class NetworkError(cause: Throwable) :
        UploadException("Network error: ${cause.message}", cause)

    class MinioError(val code: Int, message: String) :
        UploadException("MinIO error $code: $message")

    class BackendError(val code: Int, message: String) :
        UploadException("Backend error $code: $message")
}
