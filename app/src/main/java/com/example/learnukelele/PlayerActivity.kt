package com.example.learnukelele

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.learnukelele.views.PlayerNote
import com.example.learnukelele.views.PlayerNotesView
import com.example.learnukelele.audio.AudioListener
import com.example.learnukelele.audio.Complex
import com.example.learnukelele.audio.Note
import com.example.learnukelele.audio.notes
import com.example.learnukelele.audio.recursiveFFT
import com.example.learnukelele.audio.standardTuning
import com.example.learnukelele.database.TrackDatabaseHelper
import com.example.learnukelele.views.NoteScoreMark
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.roundToInt

class PlayerActivity : AppCompatActivity() {

    private val SAMPLE_RATE = 4000
    private val SAMPLE_SIZE = 600
    private val FFT_F_RESOLUTION = SAMPLE_RATE / SAMPLE_SIZE
    private val NOTE_FRAME_TIME = 3

    //Variables to track game status
    private var currentTrackTime: Double = -5.0
    private var currentTrackNotes = 0
    private var totalNoteCount = 0
    private var currentCorrectNotes = 0
    private var timeToEndGame = 0.0
    private var hasTrackEnded = false
    private var isTrackPaused = false
    private var areAllNotesDrawn = false
    private var currentTrackSpeed = 1.0
    private var trackSpeed: Array<Double> = arrayOf(0.25,0.5,0.75,1.0,1.25,1.5,1.75,2.0)

    private lateinit var audioListener: AudioListener

    private lateinit var trackBody: JSONArray
    private lateinit var trackDatabaseHelper: TrackDatabaseHelper
    private lateinit var type: String
    private var trackId: Int = 0
    private var stringOrder: Int = 0

    private lateinit var correctPaint: Paint
    private lateinit var errorPaint : Paint
    private lateinit var stringsPaint: Array<Paint>

    private var notesScoreMark: MutableList<NoteScoreMark> = mutableListOf()
    private var playerNotes: MutableList<Array<PlayerNote>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_player)

        trackId = intent.getIntExtra("trackId",0)
        trackDatabaseHelper = TrackDatabaseHelper(this)
        val track = trackDatabaseHelper.getTrack(trackId)

        trackBody = track!!.trackData
        type = track!!.type

        //We initialize the audio Listener
        audioListener = AudioListener(this)

        //We initialize paints
        correctPaint = Paint().apply {color = ContextCompat.getColor(this@PlayerActivity, R.color.green)}
        errorPaint = Paint().apply {color = ContextCompat.getColor(this@PlayerActivity, R.color.red)}
        stringsPaint = arrayOf(
            Paint().apply {color = ContextCompat.getColor(this@PlayerActivity, R.color.string1)},
            Paint().apply {color = ContextCompat.getColor(this@PlayerActivity, R.color.string2)},
            Paint().apply {color = ContextCompat.getColor(this@PlayerActivity, R.color.string3)},
            Paint().apply {color = ContextCompat.getColor(this@PlayerActivity, R.color.string4)}
        )
        lifecycleScope.launch {
            stringOrder = getSavedOption("stringOrder")
        }
        val pauseButton = findViewById<ImageButton>(R.id.pauseButton)
        pauseButton.setOnClickListener {
            if(isTrackPaused){
                runGame()
                pauseButton.setImageResource(R.drawable.baseline_pause_40)
            } else {
                pauseGame()
            }
        }
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                pauseGame()
            }
        })
        runGame()
    }

    //A function that starts the Game or restarts it after a pause
    private fun runGame() {
        isTrackPaused = false
        lifecycleScope.launch {
            if(!hasTrackEnded){
                runAddNotes()
            }
        }
        lifecycleScope.launch {
            runCountdown()
            runMoveNotes()
        }
    }

    private fun pauseGame(){
        if(!isTrackPaused&&!hasTrackEnded){
            val pauseButton = findViewById<ImageButton>(R.id.pauseButton)
            isTrackPaused = true
            pauseButton.setImageResource(R.drawable.baseline_play_arrow_40)
            showPauseDialog()
        }
    }
    private fun restartGame(){
        clearNotes()
        currentTrackTime = -5.0
        currentTrackNotes = 0
        totalNoteCount = 0
        currentCorrectNotes = 0
        areAllNotesDrawn = false
        hasTrackEnded = false
        runGame()
    }
    private fun exitGame(){
        Intent(this@PlayerActivity,PlayerMenu::class.java).also{
            it.putExtra("type",type)
            startActivity(it)
        }
    }
    //We decide here what to do when the function is minimized via Home button or block phone
    override fun onPause(){
        super.onPause()
        pauseGame()
    }

    //A function that counts time in seconds, when it fins Notes it adds them into the View
    private suspend fun runAddNotes() {
        val timerText = findViewById<TextView>(R.id.timer)

        val startTime = System.nanoTime()
        val startTrackTime = currentTrackTime

        var currentTrackNotesJSON = JSONObject("{}")
        var currentTrackNotesTime = 0.0
        if(!areAllNotesDrawn){
            currentTrackNotesJSON = trackBody[currentTrackNotes] as JSONObject
            currentTrackNotesTime = currentTrackNotesJSON.getDouble("timestamp")
        }
        while(!isTrackPaused){
            val currentTime = System.nanoTime()
            val elapsedTime: Double = (currentTime - startTime) / 1000000000.0
            currentTrackTime = startTrackTime + elapsedTime * currentTrackSpeed

            timerText.text = ((currentTrackTime * 10.0).roundToInt() / 10.0).toString()
            if((currentTrackTime > currentTrackNotesTime) && !areAllNotesDrawn){
                val notesArray = currentTrackNotesJSON.getJSONArray("notes")
                addNotes(notesArray)
                currentTrackNotes++
                if(currentTrackNotes >= trackBody.length()){
                    areAllNotesDrawn = true
                    timeToEndGame = currentTrackTime + NOTE_FRAME_TIME + 1
                } else{
                    //We search for the next note group
                    currentTrackNotesJSON = trackBody[currentTrackNotes] as JSONObject
                    currentTrackNotesTime = currentTrackNotesJSON.getDouble("timestamp")
                }
            }
            if(areAllNotesDrawn&&currentTrackTime > timeToEndGame){
                hasTrackEnded = true
                showFinishDialog()
                break
            }
            //We let the thread rest
            delay(10)
        }
    }

    //A function that moves the notes and pritns them through the canvas every frame
    private suspend fun runMoveNotes(){
        val notesView = findViewById<PlayerNotesView>(R.id.playerNotes)
        val fps = 60
        val frameTime = 1000 / fps // time between frames in milliseconds
        while (!isTrackPaused&&!hasTrackEnded) {
            val startTime = System.currentTimeMillis()

            //List to store notes that we are going to delete later
            val notesToDelete : MutableList<Array<PlayerNote>> = mutableListOf()

            //We move the notes and
            for (playerNotesArray in playerNotes) {
                val currentTime = currentTrackTime.toFloat()
                for (playerNote in playerNotesArray) {
                    playerNote.y = (currentTime - playerNote.timestamp)/NOTE_FRAME_TIME * notesView.getCanvasHeight()
                }
                //If the note has reached the end add to delete list and trigger onNoteArrive
                if(playerNotesArray[0].y > notesView.getCanvasHeight()){
                    notesToDelete.add(playerNotesArray)
                    lifecycleScope.launch {
                        listenForNotes(playerNotesArray, 2)
                    }
                }
            }

            //Delete notes indicated in notesToDelete
            for (playerNoteArray in notesToDelete){
                playerNotes.remove(playerNoteArray)
            }

            notesView.updateFrame(playerNotes, notesScoreMark)

            val endTime = System.currentTimeMillis()

            //We stored the time passed while running our code
            val elapsedTime = endTime - startTime

            if (elapsedTime < frameTime) {
                //println("We delay this ${frameTime - elapsedTime} and framTime is $frameTime")
                delay(frameTime - elapsedTime)
            }
        }
    }

    //This function draws the Text of the countdown in case we are still in negative Time
    private suspend fun runCountdown(){
        val notesView = findViewById<PlayerNotesView>(R.id.playerNotes)
        val brush = Paint().apply{
            textSize = 250f
            typeface = Typeface.DEFAULT_BOLD
            style = Paint.Style.FILL
            strokeWidth = 20f
        }
        while(currentTrackTime < -0.5 && !isTrackPaused){
            when(currentTrackTime) {
                in -4.0..-3.5 -> {
                    notesView.drawCenteredText("3", brush, stringsPaint[0].color) }
                in -3.5..-3.0 -> { notesView.clearCanvas() }
                in -3.0..-2.5 -> {
                    notesView.drawCenteredText("2", brush, stringsPaint[1].color) }
                in -2.5..-2.0 -> { notesView.clearCanvas() }
                in -2.0..-1.5 -> {
                    notesView.drawCenteredText("1", brush, stringsPaint[2].color) }
                in -1.5..-1.0 -> { notesView.clearCanvas() }
                in -1.0..-0.5 -> {
                    notesView.drawCenteredText(resources.getString(R.string.messageCountdown), brush, stringsPaint[3].color) }
            }
            //We let the thread rest
            delay(10)
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showPauseDialog() {
        val pauseButton = findViewById<ImageButton>(R.id.pauseButton)
        // Create an alert builder
        val builder = AlertDialog.Builder(this)

        // set the custom layout
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_pause_player, null)
        builder.setView(customLayout)

        val decreaseSpeedButton = customLayout.findViewById<Button>(R.id.decreaseButton)
        val increaseSpeedButton = customLayout.findViewById<Button>(R.id.increaseButton)
        val selectedSpeed = customLayout.findViewById<TextView>(R.id.selectedSpeed)
        val pauseButton1 = customLayout.findViewById<Button>(R.id.pauseButton1)
        val pauseButton2 = customLayout.findViewById<Button>(R.id.pauseButton2)
        val pauseButton3 = customLayout.findViewById<Button>(R.id.pauseButton3)

        builder.setOnCancelListener {
            runGame()
        }

        // we set the current speed to the text
        selectedSpeed.text = "x${currentTrackSpeed}"

        // create and show the alert dialog
        val dialog = builder.create()
        dialog.show()

        decreaseSpeedButton.setOnClickListener {
            val currentSpeedIndex = (currentTrackSpeed/0.25 - 1).toInt()
            if(currentSpeedIndex > 0){
                currentTrackSpeed = trackSpeed[currentSpeedIndex-1]
                selectedSpeed.text = "x${currentTrackSpeed.toString()}"
            }
        }
        increaseSpeedButton.setOnClickListener {
            val currentSpeedIndex = (currentTrackSpeed/0.25 - 1).toInt()
            if(currentSpeedIndex < trackSpeed.size-1){
                currentTrackSpeed = trackSpeed[currentSpeedIndex+1]
                selectedSpeed.text = "x${currentTrackSpeed.toString()}"
            }
        }
        pauseButton1.setOnClickListener {
            runGame()
            pauseButton.setImageResource(R.drawable.baseline_pause_40)
            dialog.dismiss()
        }
        pauseButton2.setOnClickListener {
            restartGame()
            pauseButton.setImageResource(R.drawable.baseline_pause_40)
            dialog.dismiss()
        }
        pauseButton3.setOnClickListener {
            exitGame()
            dialog.dismiss()
        }
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    private fun showFinishDialog() {
        // Create an alert builder
        val builder = AlertDialog.Builder(this)

        // set the custom layout
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_finish_player, null)
        builder.setView(customLayout)

        val scoreText = customLayout.findViewById<TextView>(R.id.scoreValue)
        val restartButton = customLayout.findViewById<Button>(R.id.finishRestart)
        val exitButton = customLayout.findViewById<Button>(R.id.finishExit)
        val highScoreText = customLayout.findViewById<TextView>(R.id.highScore)

        val score = (currentCorrectNotes.toDouble() / totalNoteCount.toDouble()) * 100
        val roundedScore = ((score * 10.0).roundToInt() / 10.0)
        scoreText.text = "$roundedScore %"
        if(trackDatabaseHelper.getTrackScore(trackId) < roundedScore) {
            trackDatabaseHelper.modifyTrackScore(trackId, roundedScore)
            lifecycleScope.launch {
                while(hasTrackEnded){
                    highScoreText.visibility = View.VISIBLE
                    delay(500)
                    highScoreText.visibility = View.INVISIBLE
                    delay(500)
                }
            }
        }
        builder.setCancelable(false)

        // create and show the alert dialog
        val dialog = builder.create()
        dialog.show()

        restartButton.setOnClickListener {
            restartGame()
            dialog.dismiss()
        }
        exitButton.setOnClickListener {
            exitGame()
            dialog.dismiss()
        }
    }

    private fun addNotes(notesArray: JSONArray) {
        val notesView = findViewById<PlayerNotesView>(R.id.playerNotes)
        val width = notesView.getCanvasWidth()
        val list = mutableListOf<PlayerNote>()
        val timestamp = currentTrackTime.toFloat()
        for (i in 0 until notesArray.length()){
            if(!notesArray.isNull(i)){
                totalNoteCount++
                //We insert the strings in the list depending on the string order chooosed in the options
                if(stringOrder==0){
                    list.add(PlayerNote((width*(i+1)*0.2).toFloat(),0f,(i+1), notesArray.optInt(i), stringsPaint[i], timestamp))
                }else{
                    val numberOfStrings = notesArray.length()
                    list.add(PlayerNote((width*(abs(i-numberOfStrings)*0.2)).toFloat(),0f,abs(i-numberOfStrings), notesArray.optInt(i), stringsPaint[i], timestamp))
                }
            }
        }
        playerNotes.add(list.toTypedArray())
    }

    private fun clearNotes(){
        val notesView = findViewById<PlayerNotesView>(R.id.playerNotes)
        playerNotes = mutableListOf()
        notesView.clearCanvas()
    }

    //We listen if the note or notes are played during the next 0.25 seconds, if yes mark as correct if not we mark it as error
    private suspend fun listenForNotes(playerNotesArray: Array<PlayerNote>, executionTimes: Int) {
        val audioSample = audioListener.getOneSample(SAMPLE_SIZE)
        val audioSampleAwait = audioSample.await()
        val fftResult = recursiveFFT(audioSampleAwait)
        val predominantFrequencies = audioListener.getFrequencies(fftResult,playerNotesArray.size)
        var searchAgainArray : ArrayList<PlayerNote> = arrayListOf()
        val matchingJobs = playerNotesArray.map { playerNote ->
            lifecycleScope.launch {
                val note: Note = if(stringOrder==0){
                    notes[standardTuning[playerNote.string - 1] + playerNote.fret]
                } else {
                    notes[standardTuning[abs(playerNote.string - 4)] + playerNote.fret]
                }
                var wasNoteFound = false
                for(predominantFrequency in predominantFrequencies){
                    val difference = abs(note.frequency-predominantFrequency)
                    if(difference< FFT_F_RESOLUTION){
                        wasNoteFound = true
                    }
                }
                if(wasNoteFound){
                    notesScoreMark.add(NoteScoreMark(playerNote.string,10f , correctPaint))
                    currentCorrectNotes++
                } else {
                    if(executionTimes>1){
                        searchAgainArray.add(playerNote)
                    }else{
                        notesScoreMark.add(NoteScoreMark(playerNote.string,10f , errorPaint))
                    }
                }
            }
        }
        matchingJobs.joinAll()
        if(executionTimes>1&&searchAgainArray.size>0){
            listenForNotes(searchAgainArray.toTypedArray(),executionTimes-1)
        }
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
