package com.example.learnukelele

import AudioPlayer
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnukelele.adapters.NotePlacerAdapter
import com.example.learnukelele.adapters.NoteTimestamp
import com.example.learnukelele.audio.Note
import com.example.learnukelele.audio.notes
import com.example.learnukelele.audio.standardTuning
import com.example.learnukelele.database.Track
import com.example.learnukelele.database.TrackDatabaseHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class TrackCreatorActivity : AppCompatActivity() {

    private val numberOfTimestamps = 1200
    private val timestampsTimeFactor = 0.25

    private var isTrackPlaying = false
    private var currentTrackTime = 0.0
    private var currentTrackTimestamp = 0

    private lateinit var mode: String
    private var trackId: Int = 0
    private var track: Track? = null
    private lateinit var imageBitmap : Bitmap
    private var trackImage: ImageView? = null

    private var timestamps: ArrayList<NoteTimestamp> = arrayListOf()
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var newRecyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_creator)

        mode = intent.getStringExtra("mode").toString()
        if (mode == "edit"){
            trackId = intent.getIntExtra("trackId", 0)
        }

        //We load the default image for track creation
        imageBitmap = BitmapFactory.decodeResource(this.resources,R.drawable.track_icon_default)
        //We initialize the audioPlayer
        audioPlayer = AudioPlayer()

        newRecyclerView = findViewById(R.id.notePlacersRecyclerView)
        linearLayoutManager = LinearLayoutManager(this)
        newRecyclerView.layoutManager = linearLayoutManager
        newRecyclerView.setHasFixedSize(true)

        for (i in  0..numberOfTimestamps){
            timestamps.add(NoteTimestamp(i*timestampsTimeFactor, arrayOf(null,null,null,null)))
        }

        if (mode == "edit"){
            val trackDatabaseHelper = TrackDatabaseHelper(this)
            track = trackDatabaseHelper.getTrack(trackId)

            val trackBody = track!!.trackData
            for (i in 0 until trackBody.length()){
                val jsonTimestamp = trackBody.getJSONObject(i)
                val notesArray = jsonTimestamp.getJSONArray("notes")
                val timestamp = jsonTimestamp.getDouble("timestamp")
                val notes: Array<Int?> = arrayOf(null,null,null,null)
                for (i in 0 until notesArray.length()){
                    if(!notesArray.isNull(i)){
                        notes[i] = notesArray.getInt(i)
                    }
                }
                timestamps[(timestamp*4).toInt()] = NoteTimestamp(timestamp, notes)
            }
        }

        lifecycleScope.launch {
            val stringOrder = getSavedOption("stringOrder")
            val notePlacerAdapter = NotePlacerAdapter(timestamps, stringOrder)
            newRecyclerView.adapter = notePlacerAdapter
        }

        //Now we set the functionalitly to the buttons
        val startButton = findViewById<ImageButton>(R.id.playButton)
        startButton.setOnClickListener {
            if(isTrackPlaying){
                isTrackPlaying = false
                startButton.setImageResource(R.drawable.baseline_play_arrow_40)
            } else {
                isTrackPlaying = true
                startButton.setImageResource(R.drawable.baseline_pause_40)
                lifecycleScope.launch{
                    runPlaying()
                }
            }
        }
        val stopButton = findViewById<ImageButton>(R.id.stopButton)
        stopButton.setOnClickListener {
            resetTrackPlaying()
        }
        val saveCreateButton = findViewById<ImageButton>(R.id.saveButton)
        saveCreateButton.setOnClickListener {
            showSaveCreateDialog()
        }
        val exitButton = findViewById<ImageButton>(R.id.quitButton)
        exitButton.setOnClickListener {
            val confirmationText = if(mode == "edit"){
                getString(R.string.confirmationTextEditQuit)
            }else{
                getString(R.string.confirmationTextCreateQuit)
            }
            showConfirmExitDialog(confirmationText)
        }
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                val confirmationText = if(mode == "edit"){
                    getString(R.string.confirmationTextEditQuit)
                }else{
                    getString(R.string.confirmationTextCreateQuit)
                }
                showConfirmExitDialog(confirmationText)
            }
        })
    }

    private suspend fun runPlaying() {
        val startTime = System.nanoTime()
        val startTrackTime = currentTrackTime
        if(!searchNextTimestampWithNotes()){
            resetTrackPlaying()
        }

        while(isTrackPlaying){
            val currentTime = System.nanoTime()
            val elapsedTime: Double = (currentTime - startTime) / 1000000000.0
            currentTrackTime = startTrackTime + elapsedTime
            scrollDownRecyclerView()
            if(timestamps[currentTrackTimestamp].timestamp < currentTrackTime){
                val noteArray = timestamps[currentTrackTimestamp].notes
                val frequenciesToPlay: MutableList<Double> = mutableListOf()
                for((string,fret) in noteArray.withIndex()){
                    if(fret!=null){
                        val note: Note = notes[standardTuning[string] + fret]
                        frequenciesToPlay.add(note.frequency)
                    }
                }
                if(frequenciesToPlay.isNotEmpty()){
                    lifecycleScope.launch {
                        audioPlayer.playFrequencies(frequenciesToPlay.toTypedArray(),250)
                    }
                }
                currentTrackTimestamp++
                if(!searchNextTimestampWithNotes()){
                    resetTrackPlaying()
                }
            }
            //We let the thread rest
            delay(5)
        }
    }

    private fun scrollDownRecyclerView() {
        val positionToScroll = (currentTrackTime/timestampsTimeFactor)
        linearLayoutManager.scrollToPositionWithOffset(positionToScroll.toInt(),0)
    }

    //Function that searches for the next timestamps with notes and returns true if it found one and false if not
    private fun searchNextTimestampWithNotes(): Boolean{
        for (index in currentTrackTimestamp until timestamps.size) {
            val noteTimestamp = timestamps[index]
            if (noteTimestamp.notes.any { it != null }) {
                currentTrackTimestamp = index
                return true
            }
        }
        return false
    }

    private fun resetTrackPlaying(){
        val startButton = findViewById<ImageButton>(R.id.playButton)
        isTrackPlaying = false
        startButton.setImageResource(R.drawable.baseline_play_arrow_40)
        currentTrackTime = 0.0
        currentTrackTimestamp = 0
        linearLayoutManager.scrollToPositionWithOffset(0,0)
        searchNextTimestampWithNotes()
    }

    private fun showSaveCreateDialog(){
        // Create an alert builder
        val builder = AlertDialog.Builder(this)

        // set the custom layout
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_track_creation, null)
        builder.setView(customLayout)

        val titleField = customLayout.findViewById<EditText>(R.id.editCreatorTitle)
        val authorField = customLayout.findViewById<EditText>(R.id.editCreatorAuthor)
        val typeSpinner = customLayout.findViewById<Spinner>(R.id.creatorTypeSpinner)
        trackImage = customLayout.findViewById(R.id.creatorThumbnailImage)
        val changeImageButton = customLayout.findViewById<TextView>(R.id.creatorImageChangeButton)
        val createButton = customLayout.findViewById<Button>(R.id.dialogCreateButton)
        val cancelButton = customLayout.findViewById<Button>(R.id.dialogCancelButton)

        // create and show the alert dialog
        val dialog = builder.create()
        dialog.show()

        val orderStringArray = resources.getStringArray(R.array.creatorTrackTypesArray)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, orderStringArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        trackImage!!.setImageBitmap(imageBitmap)
        typeSpinner.adapter = adapter
        //If we are in edit mode we set all the values of the field
        if (mode == "edit"){
            createButton.text = resources.getString(R.string.saveEditButton)
            titleField.setText(track!!.title)
            authorField.setText(track!!.author)
            when(track!!.type) {
                "song" -> typeSpinner.setSelection(0)
                "lesson" -> typeSpinner.setSelection(1)
            }
            imageBitmap = track!!.image
            trackImage!!.setImageBitmap(imageBitmap)
        }

        changeImageButton.setOnClickListener {
            launchImageSelection()
        }
        createButton.setOnClickListener {
            val trackDatabaseHelper = TrackDatabaseHelper(this)
            if (mode == "edit"){
                trackDatabaseHelper.removeTrack(trackId)
            }
            val trackData = convertTimestampArrayToJSONArray(timestamps)
            val typeOfTrack = if(typeSpinner.selectedItemPosition == 0){
                "song"
            } else {
                "lesson"
            }
            println("This is the trackData: $trackData")
            val track = Track(
                0,
                titleField.text.toString(),
                authorField.text.toString(),
                typeOfTrack,
                0.0,
                imageBitmap,
                trackData,
                true
            )
            trackDatabaseHelper.addTrack(track)
            dialog.dismiss()
            startActivity(Intent(this@TrackCreatorActivity, TrackCreatorMenu::class.java))
        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun showConfirmExitDialog(confirmationQuestion: String){
        // Create an alert builder
        val builder = AlertDialog.Builder(this)

        // set the custom layout
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_confirmation, null)
        builder.setView(customLayout)

        val confirmationMessage = customLayout.findViewById<TextView>(R.id.confirmationMessage)
        val noButton = customLayout.findViewById<Button>(R.id.confirmationNo)
        val yesButton = customLayout.findViewById<Button>(R.id.confirmationYes)

        confirmationMessage.text = confirmationQuestion

        // create and show the alert dialog
        val dialog = builder.create()
        dialog.show()

        noButton.setOnClickListener {
            dialog.dismiss()
        }
        yesButton.setOnClickListener {
            audioPlayer.release()
            startActivity(Intent(this@TrackCreatorActivity, TrackCreatorMenu::class.java))
            dialog.dismiss()
        }
    }
    private fun convertTimestampArrayToJSONArray(noteTimestamps: ArrayList<NoteTimestamp>): JSONArray {
        val jsonArray = JSONArray()

        for (noteTimestamp in noteTimestamps) {
            if(noteTimestamp.notes.any{it != null}){
                val jsonObject = JSONObject()
                val notesArray = JSONArray(noteTimestamp.notes)

                jsonObject.put("notes", notesArray)
                jsonObject.put("timestamp", noteTimestamp.timestamp)

                jsonArray.put(jsonObject)
            }
        }

        return jsonArray
    }

    private val imageSelectionLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = uriToBitmap(it)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap!!, 256, 256, false)
            imageBitmap = resizedBitmap
            trackImage!!.setImageBitmap(imageBitmap)
        }
    }

    private fun launchImageSelection() {
        imageSelectionLauncher.launch("image/*")
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
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

