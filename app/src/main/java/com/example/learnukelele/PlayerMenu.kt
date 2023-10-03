package com.example.learnukelele

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnukelele.adapters.TrackAdapter
import com.example.learnukelele.database.Track
import com.example.learnukelele.database.TrackDatabaseHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class PlayerMenu : AppCompatActivity() {

    private var tracksList: ArrayList<Track> = arrayListOf()
    private var filterApplied = 0

    private lateinit var trackAdapter: TrackAdapter
    private lateinit var typeOfTrack: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_menu)

        typeOfTrack = intent.getStringExtra("type").toString()
        val listTitle = findViewById<TextView>(R.id.listTitle)
        if(typeOfTrack == "song"){
            listTitle.text = getString(R.string.button1_title)
        }else{
            listTitle.text = getString(R.string.button2_title)
        }

        val filterButton = findViewById<ImageButton>(R.id.filterButton)
        filterButton.setOnClickListener {
            showFilterDialog()
        }

        var newRecyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        newRecyclerView.layoutManager = LinearLayoutManager(this)
        newRecyclerView.setHasFixedSize(true)

        val trackDatabaseHelper = TrackDatabaseHelper(this)
        tracksList = trackDatabaseHelper.getAllTracks() as ArrayList<Track>
        tracksList = ArrayList(tracksList.filter { track -> track.type == typeOfTrack })

        //We apply the ordering Title A-Z as default
        trackAdapter = TrackAdapter(filterTrackListTitleAZ(tracksList))
        newRecyclerView.adapter = trackAdapter

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                startActivity(Intent(this@PlayerMenu, MainMenu::class.java))
            }
        })

        trackAdapter.setTrackClickListener(object: TrackAdapter.TrackClickListener{
            override fun onTrackClick(trackId: Int) {
                Intent(this@PlayerMenu,PlayerActivity::class.java).also{
                    it.putExtra("trackId", trackId)
                    startActivity(it)
                }
            }
        })
        //If the tutorial was not skiped previously we show it
        val dataStoreKey = booleanPreferencesKey("player_tutorial")
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

        val path = "android.resource://" + packageName + "/" + R.raw.player_tutorial

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
            val dataStoreKey = booleanPreferencesKey("player_tutorial")
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
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_player_menu_filter, null)
        builder.setView(customLayout)

        val orderSpinner = customLayout.findViewById<Spinner>(R.id.orderSpinner)
        val acceptButton = customLayout.findViewById<Button>(R.id.acceptButton)

        ArrayAdapter.createFromResource(
            this,
            R.array.orderSpinner,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            orderSpinner.adapter = adapter
            orderSpinner.setSelection(filterApplied)
        }

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
    private fun applySelectedFilter(selectedOrder: String) {
        val orderStringArray = resources.getStringArray(R.array.orderSpinner)
        when(selectedOrder){
            orderStringArray[0] -> {trackAdapter.setFilteredArrayList(filterTrackListTitleAZ(tracksList))
                filterApplied = 0}
            orderStringArray[1] -> {trackAdapter.setFilteredArrayList(filterTrackListTitleZA(tracksList))
                filterApplied = 1}
            orderStringArray[2] -> {trackAdapter.setFilteredArrayList(filterTrackListAuthorAZ(tracksList))
                filterApplied = 2}
            orderStringArray[3] -> {trackAdapter.setFilteredArrayList(filterTrackListAuthorZA(tracksList))
                filterApplied = 3}
            orderStringArray[4] -> {trackAdapter.setFilteredArrayList(filterTrackListRatingUp(tracksList))
                filterApplied = 4}
            orderStringArray[5] -> {trackAdapter.setFilteredArrayList(filterTrackListRatingDown(tracksList))
                filterApplied = 5}
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

    private fun filterTrackListRatingUp(tracksList: ArrayList<Track>): ArrayList<Track> {
        return ArrayList(tracksList.sortedWith(compareBy { it.score }))
    }

    private fun filterTrackListRatingDown(tracksList: ArrayList<Track>): ArrayList<Track> {
        return ArrayList(tracksList.sortedWith(compareByDescending { it.score }))
    }

}