package com.example.learnukelele

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnukelele.adapters.TrackCreatorAdapter
import com.example.learnukelele.database.Track
import com.example.learnukelele.database.TrackDatabaseHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrackCreatorMenu : AppCompatActivity() {

    private var tracksList: ArrayList<Track> = arrayListOf()
    private var filterApplied = 0
    private var isSongsChecked = true
    private var isPracticeChecked = true
    private lateinit var trackCreatorAdapter: TrackCreatorAdapter
    private lateinit var trackDatabaseHelper: TrackDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_creator_menu)

        val filterButton = findViewById<ImageButton>(R.id.filterButton)
        filterButton.setOnClickListener {
            showFilterDialog()
        }

        val createButton = findViewById<Button>(R.id.createButton)
        createButton.setOnClickListener {
            Intent(this,TrackCreatorActivity::class.java).also{
                it.putExtra("mode", "creation")
                startActivity(it)
            }
        }
        //We get the tracks from the database
        trackDatabaseHelper = TrackDatabaseHelper(this)
        val allTracks = trackDatabaseHelper.getAllTracks()
        tracksList = allTracks.filter { it.isEditable } as ArrayList<Track>

        //We initialize the RecyclerView
        val trackListRecyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        trackListRecyclerView.layoutManager = LinearLayoutManager(this)
        trackListRecyclerView.setHasFixedSize(true)

        //We apply the ordering Title A-Z as default and put the trackList into the recyclerView
        trackCreatorAdapter = TrackCreatorAdapter(filterTrackListTitleAZ(tracksList))
        trackListRecyclerView.adapter = trackCreatorAdapter
        checkIfRecyclerIsEmpty()

        trackCreatorAdapter.setTrackClickListener(object: TrackCreatorAdapter.TrackClickListener{
            override fun onTrackEditClick(trackId: Int) {
                Intent(this@TrackCreatorMenu,TrackCreatorActivity::class.java).also{
                    it.putExtra("mode", "edit")
                    it.putExtra("trackId", trackId)
                    startActivity(it)
                }
            }
            override fun onTrackDeleteClick(trackId: Int) {
                showConfirmDeleteDialog(getString(R.string.confirmationTextDeleteTrack), trackId)
            }
        })
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                startActivity(Intent(this@TrackCreatorMenu, MainMenu::class.java))
            }
        })

        //If the tutorial was not skiped previously we show it
        val dataStoreKey = booleanPreferencesKey("creator_tutorial")
        lifecycleScope.launch {
            val preferences = dataStore.data.first()
            val optionSavedValue = preferences[dataStoreKey]
            if (optionSavedValue != false){
                showVideoTutorialDialog()
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

        val path = "android.resource://" + packageName + "/" + R.raw.creator_tutorial

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
            val dataStoreKey = booleanPreferencesKey("creator_tutorial")
            lifecycleScope.launch {
                dataStore.edit{ options ->
                    options[dataStoreKey] = false
                }
            }
            dialog.dismiss()
        }
    }

    private fun showFilterDialog() {
        // Create an alert builder
        val builder = AlertDialog.Builder(this)

        // set the custom layout
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_creator_filter, null)
        builder.setView(customLayout)

        val songCheckBox = customLayout.findViewById<CheckBox>(R.id.songCheckBox)
        val practiceCheckBox = customLayout.findViewById<CheckBox>(R.id.practiceCheckBox)
        val orderSpinner = customLayout.findViewById<Spinner>(R.id.orderSpinner)
        val acceptButton = customLayout.findViewById<Button>(R.id.acceptButton)

        songCheckBox.isChecked = isSongsChecked
        practiceCheckBox.isChecked = isPracticeChecked

        songCheckBox.setOnCheckedChangeListener { _ , isChecked ->
            isSongsChecked = isChecked
        }
        practiceCheckBox.setOnCheckedChangeListener { _ , isChecked ->
            isPracticeChecked = isChecked
        }

        val orderStringArray = resources.getStringArray(R.array.orderSpinner)
        val cutOrderStringArray = orderStringArray.take(4)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cutOrderStringArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        orderSpinner.adapter = adapter
        orderSpinner.setSelection(filterApplied)

        builder.setOnCancelListener {
            applySelectedFilter(orderSpinner.selectedItem.toString())
        }

        // create and show the alert dialog
        val dialog = builder.create()
        dialog.show()

        acceptButton.setOnClickListener {
            applySelectedFilter(orderSpinner.selectedItem.toString())
            dialog.dismiss()
        }
    }

    private fun showConfirmDeleteDialog(confirmationQuestion: String, trackId: Int){
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
            trackDatabaseHelper.removeTrack(trackId)
            val allTracks = trackDatabaseHelper.getAllTracks()
            tracksList = allTracks.filter { it.isEditable } as ArrayList<Track>
            trackCreatorAdapter.setNewArrayList(tracksList)
            checkIfRecyclerIsEmpty()
            dialog.dismiss()
        }
    }
    private fun checkIfRecyclerIsEmpty(){
        val emptyRecyclerText = findViewById<TextView>(R.id.empty_recycler)
        if (trackCreatorAdapter.itemCount == 0) {
            emptyRecyclerText.visibility = View.VISIBLE;
        } else {
            emptyRecyclerText.visibility = View.GONE;
        }
    }

    private fun applySelectedFilter(selectedOrder: String) {
        val orderStringArray = resources.getStringArray(R.array.orderSpinner)
        when(selectedOrder){
            orderStringArray[0] -> {trackCreatorAdapter.setNewArrayList(filterTrackListTitleAZ(filterTrackListType()))
                                    filterApplied = 0}
            orderStringArray[1] -> {trackCreatorAdapter.setNewArrayList(filterTrackListTitleZA(filterTrackListType()))
                                    filterApplied = 1}
            orderStringArray[2] -> {trackCreatorAdapter.setNewArrayList(filterTrackListAuthorAZ(filterTrackListType()))
                                    filterApplied = 2}
            orderStringArray[3] -> {trackCreatorAdapter.setNewArrayList(filterTrackListAuthorZA(filterTrackListType()))
                                    filterApplied = 3}
        }
    }

    private fun filterTrackListType(): ArrayList<Track> {
        if(isPracticeChecked&&!isSongsChecked){
            return ArrayList(tracksList.filter { track -> track.type == "lesson" })
        } else if (!isPracticeChecked&&isSongsChecked){
            return ArrayList(tracksList.filter { track -> track.type == "song" })
        } else if (!isPracticeChecked&&!isSongsChecked){
            return ArrayList(tracksList.filter { track -> track.type == "" })
        } else{
            return tracksList
        }
    }

    private fun filterTrackListTitleAZ(tracksList: ArrayList<Track>): ArrayList<Track> {
        return ArrayList(tracksList.sortedWith(compareBy { it.title }))
    }

    private fun filterTrackListTitleZA(tracksList: ArrayList<Track>): ArrayList<Track> {
        return ArrayList(tracksList.sortedWith(compareByDescending { it.title }))
    }

    private fun filterTrackListAuthorAZ(tracksList: ArrayList<Track>): ArrayList<Track> {
        return ArrayList(tracksList.sortedWith(compareBy { it.author }))
    }

    private fun filterTrackListAuthorZA(tracksList: ArrayList<Track>): ArrayList<Track> {
        return ArrayList(tracksList.sortedWith(compareByDescending { it.author }))
    }
}