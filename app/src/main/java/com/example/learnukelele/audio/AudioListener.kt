package com.example.learnukelele.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlin.math.sqrt

class AudioListener (
    context: Context,
        ){
    private val sampleRate = 4000
    private val magnitudeThreshold = 20000
    private var audioRecord: AudioRecord? = null
    init {
        val audioSource = MediaRecorder.AudioSource.MIC
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("No hi ha permisos de microfon")
        } else {
            audioRecord = AudioRecord( audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
            audioRecord!!.startRecording()
        }

    }
    fun getOneSample(sampleSize: Int): Deferred<Array<Complex>> = GlobalScope.async {
        var audioData = ShortArray(sampleSize)
        audioRecord!!.read(audioData, 0, audioData.size)
        // We need the information in array of Complex format for the fft function
        return@async audioData.map { Complex(it.toDouble(),0.toDouble()) }.toTypedArray()

    }

    //Function that given the result of fft returns the
    fun getFrequencies(fftOutput: Array<Complex>, numberOfPeaks: Int): IntArray {
        val magnitudes = DoubleArray(fftOutput.size / 2)
        //We calculate the magnitude of the first half of componenets (the second half is redundant information)
        for (i in magnitudes.indices) {
            magnitudes[i] = sqrt(fftOutput[i].r * fftOutput[i].r + fftOutput[i].i * fftOutput[i].i)
        }
        //We take the highest magnitudes indices
        val highestMagnitudesIndices =
            magnitudes.indices.sortedByDescending { magnitudes[it] }.take(numberOfPeaks)
        //We calculate and return the frequency of the components with higher magnitude than THRESHOLD
        return highestMagnitudesIndices.map {
            if (magnitudes[it] > magnitudeThreshold) {
                it * sampleRate / fftOutput.size
            } else {
                0
            }
        }.toIntArray()
    }
}




