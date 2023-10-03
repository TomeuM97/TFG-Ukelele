import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class AudioPlayer() {

    private val sampleRate = 44100
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channels = AudioFormat.CHANNEL_OUT_MONO
    private val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channels, audioFormat)

    private val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(audioFormat)
                .setSampleRate(sampleRate)
                .setChannelMask(channels)
                .build()
        )
        .setBufferSizeInBytes(bufferSize)
        .build()

    fun playFrequencies(frequencies: Array<Double>, durationMillis: Long) {
        //We calculate how many samples do we need for the given the sampleRate of audioTrack to get the desired duration
        val buffer = ShortArray((sampleRate * durationMillis / 1000).toInt())

        //We fill the buffer with the corresponding sin wave
        for (i in buffer.indices) {
            val time = i.toDouble() / sampleRate
            var sample: Short = 0
            for(frequency in frequencies){
                val frequencySample = (12000 * sin(2 * PI * frequency * time)).toInt().toShort()
                sample = addShorts(sample, frequencySample)
            }
            buffer[i] = sample
        }
        GlobalScope.launch {
            audioTrack.play()
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.stop()
        }
    }

    private fun addShorts(a: Short, b: Short): Short {
        val result = a.toInt() + b.toInt()
        return when {
            result > Short.MAX_VALUE -> Short.MAX_VALUE
            result < Short.MIN_VALUE -> Short.MIN_VALUE
            else -> result.toShort()
        }
    }

    fun release() {
        audioTrack.release()
    }
}
