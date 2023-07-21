package com.example.learnukelele

import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.learnukelele.Utils.AudioListener
import com.example.learnukelele.Utils.Complex
import com.example.learnukelele.Utils.Note
import com.example.learnukelele.Utils.notes
import com.example.learnukelele.Utils.recursiveFFT
import kotlinx.coroutines.launch
import kotlin.math.abs

//We define the tunning that we want to get
var desiredTuning: Array<Note> = arrayOf(notes[55],notes[48],notes[52],notes[57])

var barPreviousPostion = 0

class TunerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tuner)

        val desiredNote = findViewById<TextView>(R.id.textViewNote)
        val stringNotes: Array<TextView> = arrayOf(
            findViewById(R.id.textViewString1),
            findViewById(R.id.textViewString2),
            findViewById(R.id.textViewString3),
            findViewById(R.id.textViewString4)
        )

        //We set the displayed note of each string
        for ((index, textview) in stringNotes.withIndex()) {
            textview.text = desiredTuning[index].name
            println("Note $index has frecuency: ${desiredTuning[index].frequency}")
        }

        val audioListener = AudioListener(this,4000, 2048)
        lifecycleScope.launch{
            while(true){
                val audioSample = audioListener.getOneSample()
                val fftResult = recursiveFFT(audioSample)
                val predominantFrecuency = audioListener.getFrequencies(fftResult,1)[0]
                println(predominantFrecuency)
                val predominantNote = getDesiredNote(predominantFrecuency)
                println(predominantNote.name)
                desiredNote.text = predominantNote.name
            }
        }
    }

    //Function that given a frecuency, detects the chord we are trying to tune and returns it as a Note
    private fun getDesiredNote(inputFrecuency: Int): Note {
        var minimumNoteDiference: Note = desiredTuning[0]
        for (note in desiredTuning) {
            if (abs(note.frequency - inputFrecuency) < abs(minimumNoteDiference.frequency - inputFrecuency)) {
                minimumNoteDiference = note
            }
        }
        moveBar((minimumNoteDiference.frequency - inputFrecuency).toInt())
        return minimumNoteDiference
    }

    private fun moveBar (endPosition: Int){
        val movingBar = findViewById<View>(R.id.movingBar)
        val distanceText = findViewById<TextView>(R.id.textViewDistance)

        val animation = ValueAnimator.ofInt(barPreviousPostion, endPosition*10)
        animation.addUpdateListener { valueAnimator ->
            val layoutParams = movingBar.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.marginStart = valueAnimator.animatedValue as Int
            movingBar.layoutParams = layoutParams
        }
        animation.duration = 100 // duration of the animation in milliseconds
        animation.interpolator = LinearInterpolator() // optional: set the animation interpolator to LinearInterpolator
        distanceText.text = endPosition.toString()
        barPreviousPostion = endPosition*10
        animation.start()
    }
}

