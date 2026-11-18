package com.example.xo.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Original synthesized ambient loop (no external/copyrighted recording).
 * Soft chord tones are faded and blended into a continuous stream.
 */
class AmbientMusic {
    private val sampleRate = 22050
    @Volatile private var running = false
    private var audioTrack: AudioTrack? = null
    private var worker: Thread? = null

    fun start() {
        if (running) return
        running = true
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build())
            .setBufferSizeInBytes(minBuffer * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack = track
        track.setVolume(0.42f)
        track.play()
        worker = Thread {
            val buffer = ShortArray(1024)
            var sampleIndex = 0L
            // A mellow original four-chord cycle in a major key.
            val chords = arrayOf(
                doubleArrayOf(261.63, 329.63, 392.00),
                doubleArrayOf(220.00, 261.63, 329.63),
                doubleArrayOf(174.61, 220.00, 261.63),
                doubleArrayOf(196.00, 246.94, 293.66)
            )
            while (running) {
                for (i in buffer.indices) {
                    val time = sampleIndex.toDouble() / sampleRate
                    val chordIndex = ((time / 5.0).toInt()) % chords.size
                    val chord = chords[chordIndex]
                    val fade = minOf(1.0, time % 5.0 / 0.7, (5.0 - time % 5.0) / 0.7).coerceAtLeast(0.0)
                    var value = 0.0
                    for (j in chord.indices) {
                        val freq = chord[j]
                        value += sin(2.0 * PI * freq * time + j * 0.22) * 0.10
                        value += sin(2.0 * PI * freq * 0.5 * time + j * 0.31) * 0.025
                    }
                    // Soft, subtle high note for a gentle sense of motion.
                    val upper = chord[1] * 2.0
                    value += sin(2.0 * PI * upper * time) * 0.025
                    buffer[i] = (value * fade * 7000.0).toInt().coerceIn(-32768, 32767).toShort()
                    sampleIndex++
                }
                track.write(buffer, 0, buffer.size)
            }
        }.apply { name = "XO3D-Ambient"; isDaemon = true; start() }
    }

    fun stop() {
        running = false
        worker?.join(250)
        worker = null
        runCatching { audioTrack?.pause(); audioTrack?.flush(); audioTrack?.stop(); audioTrack?.release() }
        audioTrack = null
    }
}

@Composable
fun dynamicWaveBackground(): Color {
    val transition = rememberInfiniteTransition(label = "appBackgroundWave")
    val hue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 26000, easing = LinearEasing)),
        label = "backgroundHue"
    )
    return Color.hsv(hue, 0.42f, 0.19f)
}


/**
 * Records touch origins without consuming gestures, then paints expanding,
 * hue-shifting rings behind the screen content.
 */
@Composable
fun Modifier.touchWaveBackground(): Modifier {
    val touchOrigins = remember { mutableStateListOf<Offset>() }
    val transition = rememberInfiniteTransition(label = "touchWaves")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2200, easing = LinearEasing)),
        label = "touchWavePhase"
    )
    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchOrigins.add(down.position)
                    if (touchOrigins.size > 7) touchOrigins.removeAt(0)
                }
            }
        }
        .drawBehind {
            val maxRadius = size.maxDimension * 0.72f
            touchOrigins.forEachIndexed { index, origin ->
                for (ring in 0..3) {
                    val ringPhase = (phase + ring * 0.25f + index * 0.06f) % 1f
                    val radius = maxRadius * (0.08f + ringPhase * 0.92f)
                    val alpha = (1f - ringPhase) * 0.26f
                    val hue = (phase * 360f + index * 48f + ring * 70f) % 360f
                    drawCircle(
                        color = Color.hsv(hue, 0.72f, 1f).copy(alpha = alpha),
                        radius = radius,
                        center = origin,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
}
