package com.example.learnukelele

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.example.learnukelele.audio.AudioListener
import com.example.learnukelele.audio.Note
import com.example.learnukelele.audio.notes
import com.example.learnukelele.audio.recursiveFFT
import com.example.learnukelele.audio.standardTuning
import com.example.learnukelele.views.TunerMarkerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs

class TunerActivity : AppCompatActivity() {
    val sampleSize = 4000
    
    //We define the tunning that we want to get
    var desiredTuning: Array<Note> = arrayOf(notes[standardTuning[0]],notes[standardTuning[1]],notes[standardTuning[2]],notes[standardTuning[3]])
    var desiredNote: Note = desiredTuning[0]

    var messageTextString: String = ""
    var messageColor: Int = Color.BLACK

    var latinNotation = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //We add the Flag to keep the screen always on while the user is tuning the ukelele
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_tuner)

        val desiredNoteTextView = findViewById<TextView>(R.id.textViewNote)
        val stringNotes: Array<TextView> = arrayOf(
            findViewById(R.id.textViewString1),
            findViewById(R.id.textViewString2),
            findViewById(R.id.textViewString3),
            findViewById(R.id.textViewString4)
        )
        //We get the saved notation in options
        runBlocking {
            val checkedNotation = getSavedOption("notation")
            if(checkedNotation==1){
                latinNotation=true
            }
        }

        //We set the displayed note of each string
        for ((index, textview) in stringNotes.withIndex()) {
            textview.text = if(latinNotation){
                desiredTuning[index].nameL
            } else{
                desiredTuning[index].name
            }
        }

        val tunerView = findViewById<TunerMarkerView>(R.id.tunerView)
        val distanceText = findViewById<TextView>(R.id.textViewDistance)
        val messageText = findViewById<TextView>(R.id.textViewMessage)

        //Cream la instancia d'audio listener
        val audioListener = AudioListener(this)

        val backgroundColor = getThemeColor(this@TunerActivity, com.google.android.material.R.attr.colorOnPrimary)
        val stringColor = getThemeColor(this@TunerActivity, com.google.android.material.R.attr.colorTertiary)
        val primaryColor = getThemeColor(this@TunerActivity, com.google.android.material.R.attr.colorPrimary)

        //If the tutorial was not skiped previously we show it
        val dataStoreKey = booleanPreferencesKey("tuner_tutorial")
        lifecycleScope.launch {
            val preferences = dataStore.data.first()
            val optionSavedValue = preferences[dataStoreKey]
            if (optionSavedValue != false){
                showVideoTutorialDialog()
            }
        }

        lifecycleScope.launch{
            while(true){
                val audioSample = audioListener.getOneSample(sampleSize)
                val audioSampleAwait = audioSample.await()
                val fftResult = recursiveFFT(audioSampleAwait)
                val predominantFrequency = audioListener.getFrequencies(fftResult,1)[0]
                if(predominantFrequency != 0){
                    desiredNote = getDesiredNote(predominantFrequency)
                    desiredNoteTextView.text = if(latinNotation){
                        desiredNote.nameL
                    } else{
                        desiredNote.name
                    }
                    //We highlight the note we want to tune
                    for (textview in stringNotes) {
                        if(textview.text == desiredNote.name||textview.text == desiredNote.nameL){
                            textview.setBackgroundColor(primaryColor)
                            textview.setTextColor(backgroundColor)
                        } else{
                            textview.setBackgroundColor(backgroundColor)
                            textview.setTextColor(stringColor)
                        }
                    }
                    messageText.setTextColor(messageColor)
                    messageText.text = messageTextString
                    messageText.visibility = View.VISIBLE
                }else {
                    //THe sound is not strong enough
                    tunerView.eraseCanvas()
                    desiredNoteTextView.text = "-"
                    distanceText.text = "-"
                    messageTextString = ""
                    for (textview in stringNotes) {
                        textview.setBackgroundColor(backgroundColor)
                        textview.setTextColor(stringColor)
                    }
                    messageText.visibility = View.INVISIBLE
                }
            }
        }
    }
    private fun showVideoTutorialDialog() {
        // Create an alert builder
        val builder = AlertDialog.Builder(this)

        // set the custom layout
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_videotutorial, null)
        builder.setView(customLayout)

        val skipTutorialButton = customLayout.findViewById<Button>(R.id.skipTutorial)
        val videoView = customLayout.findViewById<VideoView>(R.id.videoView)

        val path = "android.resource://" + packageName + "/" + R.raw.tuner_tutorial

        videoView.setVideoPath(path)
        videoView.start()
        videoView.setOnCompletionListener { mediaPlayer ->
            mediaPlayer.seekTo(0)
            mediaPlayer.start()
        }

        // create and show the alert dialog
        val dialog = builder.create()
        dialog.show()

        skipTutorialButton.setOnClickListener {
            val dataStoreKey = booleanPreferencesKey("tuner_tutorial")
            lifecycleScope.launch {
                dataStore.edit{ options ->
                    options[dataStoreKey] = false
                }
            }
            dialog.dismiss()
        }
    }

    //Function that given a frecuency, detects the chord we are trying to tune and returns it as a Note
    private fun getDesiredNote(inputFrecuency: Int): Note {
        val distanceText = findViewById<TextView>(R.id.textViewDistance)
        var minimumNoteDiference: Note = desiredTuning[0]
        for (note in desiredTuning) {
            if (abs(note.frequency - inputFrecuency) < abs(minimumNoteDiference.frequency - inputFrecuency)) {
                minimumNoteDiference = note
            }
        }
        val tunerView = findViewById<TunerMarkerView>(R.id.tunerView)
        val distance = inputFrecuency - minimumNoteDiference.frequency
        if (distance < -1){
            messageTextString = getString(R.string.messageTooLoose)
            messageColor = ContextCompat.getColor(this@TunerActivity, R.color.red )
        } else if(distance > 1){
            messageTextString = getString(R.string.messageTooTight)
            messageColor = ContextCompat.getColor(this@TunerActivity, R.color.red )
        } else {
            messageTextString = getString(R.string.messageRightTone)
            messageColor = ContextCompat.getColor(this@TunerActivity, R.color.green )
        }
        // We calculate the % of the canvas.width that we need to move the bar with updateFrame
        val positionPercentFromMiddle = distance/60
        tunerView.updateFrame(positionPercentFromMiddle.toFloat(), messageColor)
        distanceText.text = String.format("%.2f", distance)
        return minimumNoteDiference
    }

    private fun getThemeColor(context: Context, attrRes: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    private suspend fun getSavedOption (optionName: String): Int {
        val dataStoreKey = intPreferencesKey(optionName)
        val preferences = dataStore.data.first()
        val optionSavedValue = preferences[dataStoreKey]
        return if(optionSavedValue is Int){
            optionSavedValue
        } else {
            0
        }
    }
}

