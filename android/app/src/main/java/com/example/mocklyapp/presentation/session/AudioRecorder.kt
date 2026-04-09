package com.example.mocklyapp.presentation.session

import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(sessionId: String): File? {
        stop() // на всякий случай останавливаем старый

        val file = File(
            context.cacheDir,
            "mockly_session_${sessionId}_${System.currentTimeMillis()}.m4a"
        )

        val r = MediaRecorder()
        recorder = r
        outputFile = file

        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(file.absolutePath)

            r.prepare()
            r.start()
        } catch (e: IOException) {
            e.printStackTrace()
            stop()
            return null
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            stop()
            return null
        }

        return file
    }

    fun stop(): File? {
        val file = outputFile
        try {
            recorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {
                }
                reset()
                release()
            }
        } catch (_: Exception) {
        } finally {
            recorder = null
            outputFile = null
        }
        return file
    }
}
