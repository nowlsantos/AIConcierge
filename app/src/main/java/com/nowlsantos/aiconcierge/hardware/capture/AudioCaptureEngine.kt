package com.nowlsantos.aiconcierge.hardware.capture

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import androidx.core.content.ContextCompat
import com.nowlsantos.aiconcierge.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.milliseconds

/**
 * AUDIO CAPTURE ENGINE
 * High-performance 16kHz 16-bit Mono PCM audio capture targeting VOICE_RECOGNITION.
 * Optimized with 100ms PCM chunking (3200 bytes), AcousticEchoCanceler (AEC), and NoiseSuppressor (NS).
 */
class AudioCaptureEngine(
    private val context: Context
) {
    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        // 100ms of 16kHz 16-bit Mono PCM audio = 16,000 samples/sec * 2 bytes/sample * 0.10 sec = 3200 bytes
        const val CHUNK_SIZE = 3200
    }

    private val captureLock = ReentrantLock()
    private val isCapturing = AtomicBoolean(false)
    private val isMuted = AtomicBoolean(false)

    private var audioRecord: AudioRecord? = null
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startCaptureStream(): Flow<ByteArray> = channelFlow {
        if (!hasRecordPermission()) {
            Logger.e("RECORD_AUDIO permission missing for capture stream", null, "AUDIO_CAPTURE")
            return@channelFlow
        }

        captureLock.withLock {
            if (isCapturing.get()) {
                Logger.w("Capture stream already running. Forcing cleanup before re-initialization...", "AUDIO_CAPTURE")
                stopCaptureInternal()
            }

            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            val bufferSize = (minBufferSize * 4).coerceAtLeast(CHUNK_SIZE * 2)

            val rec = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                Logger.e("AudioRecord failed to initialize", null, "AUDIO_CAPTURE")
                rec.release()
                return@withLock null
            }

            audioRecord = rec

            // Attach Hardware/Software Acoustic Echo Canceler (AEC)
            if (AcousticEchoCanceler.isAvailable()) {
                try {
                    aec = AcousticEchoCanceler.create(rec.audioSessionId)?.apply {
                        enabled = true
                        Logger.d("AcousticEchoCanceler enabled on session ${rec.audioSessionId}", "AUDIO_CAPTURE")
                    }
                } catch (e: Exception) {
                    Logger.w("Failed to attach AcousticEchoCanceler: ${e.localizedMessage}", "AUDIO_CAPTURE")
                }
            }

            // Attach Hardware/Software Noise Suppressor (NS)
            if (NoiseSuppressor.isAvailable()) {
                try {
                    ns = NoiseSuppressor.create(rec.audioSessionId)?.apply {
                        enabled = true
                        Logger.d("NoiseSuppressor enabled on session ${rec.audioSessionId}", "AUDIO_CAPTURE")
                    }
                } catch (e: Exception) {
                    Logger.w("Failed to attach NoiseSuppressor: ${e.localizedMessage}", "AUDIO_CAPTURE")
                }
            }

            rec.startRecording()
            isCapturing.set(true)
            Logger.d("AudioCaptureEngine started recording at $SAMPLE_RATE Hz (100ms / 3200-byte chunks)", "AUDIO_CAPTURE")
            rec
        } ?: return@channelFlow

        try {
            val buffer = ByteArray(CHUNK_SIZE)
            while (isActive && isCapturing.get()) {
                val currentRecord = audioRecord ?: break
                if (currentRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readBytes = currentRecord.read(buffer, 0, buffer.size)
                    if (readBytes > 0) {
                        if (!isMuted.get()) {
                            send(buffer.copyOf(readBytes))
                        }
                    } else {
                        if (readBytes < 0) {
                            Logger.w("AudioRecord read error ($readBytes)", "AUDIO_CAPTURE")
                        }
                        kotlinx.coroutines.yield()
                    }
                } else {
                    // Yield execution to prevent busy-wait CPU spin when recording is paused
                    kotlinx.coroutines.delay(50.milliseconds)
                }
            }
        } catch (e: Exception) {
            Logger.e("Capture stream exception: ${e.localizedMessage}", e, "AUDIO_CAPTURE")
        } finally {
            stopCapture()
        }
    }.flowOn(Dispatchers.IO)

    fun pauseRecording() {
        isMuted.set(true)
        Logger.d("AudioCaptureEngine software squelch active (mic muted for clear speaker output)", "AUDIO_CAPTURE")
    }

    fun resumeRecording() {
        isMuted.set(false)
        Logger.d("AudioCaptureEngine software squelch released (mic active for guest listening)", "AUDIO_CAPTURE")
    }

    private fun stopCaptureInternal() {
        isMuted.set(false)
        if (!isCapturing.getAndSet(false) && audioRecord == null) {
            return
        }

        try {
            ns?.apply {
                enabled = false
                release()
            }
            ns = null

            aec?.apply {
                enabled = false
                release()
            }
            aec = null

            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
            audioRecord = null

            Logger.d("AudioCaptureEngine stopped cleanly", "AUDIO_CAPTURE")
        } catch (e: Exception) {
            Logger.e("Error stopping AudioRecord: ${e.localizedMessage}", e, "AUDIO_CAPTURE")
        }
    }

    fun stopCapture() {
        captureLock.withLock {
            stopCaptureInternal()
        }
    }
}
