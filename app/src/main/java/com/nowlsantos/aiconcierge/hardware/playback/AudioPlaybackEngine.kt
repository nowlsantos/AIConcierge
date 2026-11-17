package com.nowlsantos.aiconcierge.hardware.playback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.nowlsantos.aiconcierge.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

sealed interface PlaybackState {
    object Idle : PlaybackState
    object Playing : PlaybackState
    object Draining : PlaybackState
}

/**
 * AUDIO PLAYBACK ENGINE
 * Pure software class encapsulating Android AudioTrack (24kHz PCM 16-bit Mono).
 * Uses USAGE_MEDIA for crystal-clear tablet speaker output.
 */
class AudioPlaybackEngine(
    private val scope: CoroutineScope,
    private val sampleRate: Int = 24000
) {
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    val audioQueue = Channel<ByteArray?>(capacity = 256)
    @Volatile
    private var audioTrack: AudioTrack? = null
    private val trackLock = Any()
    private var playbackJob: Job? = null
    private val totalBytesWritten = AtomicLong(0L)
    private val baseHeadPosition = AtomicLong(0L)

    fun initAudioTrack() {
        synchronized(trackLock) {
            if (audioTrack != null) return

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val bufferSize = (minBufferSize * 4).coerceAtLeast(8192)

            audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            ).apply {
                play()
            }

            totalBytesWritten.set(0L)
            baseHeadPosition.set(0L)
            Logger.d("AudioPlaybackEngine initialized at $sampleRate Hz", "AUDIO_PLAYBACK")
        }
    }

    fun clearAudioQueue() {
        var count = 0
        while (audioQueue.tryReceive().isSuccess) {
            count++
        }
        if (count > 0) {
            Logger.d("Purged $count stale audio chunks from playback queue", "AUDIO_PLAYBACK")
        }
    }

    fun startConsumer() {
        initAudioTrack()
        clearAudioQueue()
        if (playbackJob?.isActive == true) return

        playbackJob = scope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val chunk = withTimeoutOrNull(3000.milliseconds) {
                        audioQueue.receiveCatching().getOrNull()
                    }

                    if (chunk != null && chunk.isNotEmpty()) {
                        val track = audioTrack
                        if (track != null) {
                            if (_playbackState.value != PlaybackState.Playing) {
                                synchronized(trackLock) {
                                    if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                                        track.play()
                                    }
                                    baseHeadPosition.set(track.playbackHeadPosition.toLong() and 0xFFFFFFFFL)
                                    totalBytesWritten.set(0L)
                                }
                                _playbackState.value = PlaybackState.Playing
                            }

                            val written = try {
                                track.write(chunk, 0, chunk.size)
                            } catch (e: Exception) {
                                Logger.w("AudioTrack write failed: ${e.localizedMessage}", "AUDIO_PLAYBACK")
                                0
                            }
                            if (written > 0) {
                                totalBytesWritten.addAndGet(written.toLong())
                            }
                        }
                    } else {
                        // Either null/empty sentinel received OR 3.0s timeout with no incoming audio chunks
                        if (_playbackState.value == PlaybackState.Playing || _playbackState.value == PlaybackState.Draining) {
                            _playbackState.value = PlaybackState.Draining
                            drainHardwareBuffer()
                            totalBytesWritten.set(0L)
                            _playbackState.value = PlaybackState.Idle
                            Logger.d("AudioPlaybackEngine idle (turn complete or silence timeout)", "AUDIO_PLAYBACK")
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("Error in audio playback consumer: ${e.localizedMessage}", e, "AUDIO_PLAYBACK")
                _playbackState.value = PlaybackState.Idle
            }
        }
    }

    private suspend fun drainHardwareBuffer() {
        try {
            val track = audioTrack ?: return
            val totalFramesWritten: Long = totalBytesWritten.get() / 2L // 16-bit mono = 2 bytes per frame
            val startHead = baseHeadPosition.get()
            var currentHead: Long = synchronized(trackLock) {
                if (audioTrack != null) track.playbackHeadPosition.toLong() and 0xFFFFFFFFL else totalFramesWritten + startHead
            }
            var playedFramesThisTurn: Long = (currentHead - startHead) and 0xFFFFFFFFL

            var remainingFrames: Long = totalFramesWritten - playedFramesThisTurn
            if (remainingFrames <= 0L) return

            val calculatedWaitMs = ((remainingFrames * 1000L) / sampleRate.toLong()) + 150L
            val maxWaitMs: Long = calculatedWaitMs.coerceAtMost(5000L)
            val startTime = System.currentTimeMillis()
            var lastHead = currentHead
            var stalledSince = startTime

            while (remainingFrames > 0L && (System.currentTimeMillis() - startTime) < maxWaitMs) {
                kotlinx.coroutines.delay(15.milliseconds)
                val now = System.currentTimeMillis()
                currentHead = synchronized(trackLock) {
                    val activeTrack = audioTrack
                    if (activeTrack == null || activeTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        return
                    }
                    activeTrack.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                }
                if (currentHead != lastHead) {
                    lastHead = currentHead
                    stalledSince = now
                } else if (now - stalledSince > 250L) {
                    // Hardware playback head has finished advancing
                    break
                }
                playedFramesThisTurn = (currentHead - startHead) and 0xFFFFFFFFL
                remainingFrames = totalFramesWritten - playedFramesThisTurn
            }
            Logger.d("Hardware buffer drained successfully. Remaining frames: $remainingFrames", "AUDIO_PLAYBACK")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Error draining hardware buffer: ${e.localizedMessage}", e, "AUDIO_PLAYBACK")
        }
    }

    fun interruptPlayback() {
        clearAudioQueue()
        synchronized(trackLock) {
            try {
                audioTrack?.apply {
                    if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                        pause()
                        flush()
                        play()
                    }
                }
                totalBytesWritten.set(0L)
                baseHeadPosition.set(audioTrack?.playbackHeadPosition?.toLong()?.and(0xFFFFFFFFL) ?: 0L)
                _playbackState.value = PlaybackState.Idle
                Logger.d("AudioPlaybackEngine interrupted and flushed", "AUDIO_PLAYBACK")
            } catch (e: Exception) {
                Logger.e("Error flushing AudioTrack: ${e.localizedMessage}", e, "AUDIO_PLAYBACK")
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        clearAudioQueue()
        synchronized(trackLock) {
            try {
                audioTrack?.apply {
                    if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                        stop()
                    }
                    release()
                }
                audioTrack = null
                totalBytesWritten.set(0L)
                baseHeadPosition.set(0L)
                _playbackState.value = PlaybackState.Idle
                Logger.d("AudioPlaybackEngine stopped", "AUDIO_PLAYBACK")
            } catch (e: Exception) {
                Logger.e("Error releasing AudioTrack: ${e.localizedMessage}", e, "AUDIO_PLAYBACK")
            }
        }
    }
}
