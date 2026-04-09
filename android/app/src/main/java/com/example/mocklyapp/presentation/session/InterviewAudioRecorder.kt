package com.example.mocklyapp.presentation.session

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

class InterviewAudioRecorder(
    private val context: Context
) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    @RequiresApi(Build.VERSION_CODES.S)
    fun start(sessionId: String): File {
        stop() // на всякий случай

        // файл в кэше приложения
        val fileName = "mockly_session_${sessionId}_${System.currentTimeMillis()}.m4a"
        val file = File(context.cacheDir, fileName)

        val mediaRecorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        recorder = mediaRecorder
        outputFile = file
        return file
    }

    fun stop(): File? {
        val r = recorder ?: return outputFile
        return try {
            r.stop()
            r.reset()
            r.release()
            outputFile
        } catch (_: Exception) {
            outputFile
        } finally {
            recorder = null
        }
    }

    fun release() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        } finally {
            recorder = null
        }
    }
}
