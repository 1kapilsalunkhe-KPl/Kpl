package com.pixel6a.callrecorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles audio recording on Google Pixel 6a.
 * On Pixel 6a (Android 12/13/14/15), VOICE_COMMUNICATION or MIC with speakerphone
 * provides the cleanest two-way audio without requiring kernel modifications.
 */
class AudioRecordManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    fun startRecording(): Boolean {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
                ?: context.filesDir
            
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            
            currentOutputFile = File(storageDir, "Pixel6a_Call_$timeStamp.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                // Pixel 6a audio source hierarchy:
                // 1. VOICE_COMMUNICATION: Best for speech clarity and echo cancellation
                // 2. MIC fallback: Works seamlessly in speakerphone mode
                try {
                    setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                } catch (e: Exception) {
                    Log.w("AudioRecordManager", "VOICE_COMMUNICATION unavailable, falling back to MIC")
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }

                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentOutputFile?.absolutePath)

                prepare()
                start()
            }
            Log.d("AudioRecordManager", "Recording started: ${currentOutputFile?.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error starting recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            return false
        }
    }

    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            Log.d("AudioRecordManager", "Recording saved: ${currentOutputFile?.absolutePath}")
            currentOutputFile
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error stopping recording", e)
            mediaRecorder = null
            null
        }
    }
}
