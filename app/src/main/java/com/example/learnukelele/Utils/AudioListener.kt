package com.example.learnukelele.Utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.delay
import kotlin.math.sqrt


const val REAL_SAMPLE_RATE = 8000
const val SAMPLE_SIZE = 4096
const val NUMBER_OF_SAMPLES_PER_SECOND = REAL_SAMPLE_RATE/ SAMPLE_SIZE
private var audioData = ShortArray(SAMPLE_SIZE)
private var newSampleRate: Int = 0
private var newSampleSize: Int = 0
private var audioRecord: AudioRecord? = null

class AudioListener (
    context: Context,
    sampleRate: Int,
    sampleSize: Int
        ){
    init {
        newSampleRate = sampleRate
        newSampleSize = sampleSize
        val audioSource = MediaRecorder.AudioSource.MIC
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(REAL_SAMPLE_RATE, channelConfig, audioFormat)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("No hi ha permisos de microfon")
        } else {
            audioRecord = AudioRecord( audioSource, REAL_SAMPLE_RATE, channelConfig, audioFormat, bufferSize)
            audioRecord!!.startRecording()
        }

    }
    suspend fun getOneSample(): Array<Complex> {
        audioRecord!!.read(audioData, 0, audioData.size)
        delay(200/ NUMBER_OF_SAMPLES_PER_SECOND.toLong())
        // We reduce the real sample rate to a lower sample rate to increase fft resolution without increasing the sample Size
        val newLength = (audioData.size * newSampleRate / REAL_SAMPLE_RATE)
        var newSamples = ShortArray(newLength)
        val step = REAL_SAMPLE_RATE / newSampleRate
        for((index, _) in newSamples.withIndex()){
            newSamples[index] = audioData[index*step]
        }
        // We need the information in array of Complex format for the fft funciton
        return newSamples.map { Complex(it.toDouble(),0.toDouble()) }.toTypedArray()
    }

    //Function that given the result of fft returns the
    fun getFrequencies(fftOutput: Array<Complex>, numberOfPeaks: Int): IntArray {
        val magnitudes = DoubleArray(fftOutput.size)
        //We calculate the magnitude of all components
        for (i in fftOutput.indices) {
            magnitudes[i] = sqrt(fftOutput[i].r * fftOutput[i].r + fftOutput[i].i * fftOutput[i].i)
        }
        //We take the highest magnitudes indices
        val highestMagnitudesIndices =
            magnitudes.indices.sortedByDescending { magnitudes[it] }.take(numberOfPeaks)

        //We calculate and return the frequency of the components with higher magnitude
        return highestMagnitudesIndices.map { it * newSampleRate / fftOutput.size }.toIntArray()
    }
}




