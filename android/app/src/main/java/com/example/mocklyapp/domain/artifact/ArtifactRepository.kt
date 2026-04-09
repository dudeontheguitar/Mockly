package com.example.mocklyapp.domain.artifact

import java.io.File

interface ArtifactRepository {

    suspend fun uploadSessionAudio(
        sessionId: String,
        file: File,
        durationSec: Int
    ): Int
}
