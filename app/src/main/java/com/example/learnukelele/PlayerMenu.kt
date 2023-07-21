package com.example.learnukelele

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnukelele.Adapter.TrackAdapter
import com.example.learnukelele.Model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ratings")

class PlayerMenu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_menu)

        val typeOfTrack = intent.getStringExtra("type")
        val listTitle = findViewById<TextView>(R.id.listTitle)
        if(typeOfTrack == "song"){
            listTitle.text = getString(R.string.button1_title)
        }else{
            listTitle.text = getString(R.string.button2_title)
        }

        lifecycleScope.launch {
            saveRating("trackdata2.json",99)
            saveRating("trackdata1.json",34)
            saveRating("trackdata3.json",58)
        }

        var newRecyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        newRecyclerView.layoutManager = LinearLayoutManager(this)
        newRecyclerView.setHasFixedSize(true)

        var tracksList: ArrayList<Track> = arrayListOf<Track>()

        applicationContext.assets.list("trackdata")?.forEach { filename ->
            val trackInString = applicationContext.assets.open("trackdata/$filename").bufferedReader().use { it.readText() }
            val trackInJson = JSONObject(trackInString)
            val trackHeaderInJSOn = trackInJson.getJSONObject("track-header")

            if(trackHeaderInJSOn.getString("type") == typeOfTrack ){
                val trackTitle = trackHeaderInJSOn.getString("name")
                val trackAuthor = trackHeaderInJSOn.getString("artist")
                val trackImageDrawable: Drawable = if(trackHeaderInJSOn.has("image-name")){
                    val imageFilename = trackHeaderInJSOn.getString("image-name")
                    Drawable.createFromStream(assets.open("thumbnail/$imageFilename"), null)!!
                } else {
                    AppCompatResources.getDrawable(this, R.drawable.track_icon_default)!!
                }

                var trackRating: Int
                runBlocking {
                    trackRating = getRating(filename)
                }
                val track = Track(trackImageDrawable,trackTitle,trackAuthor,trackRating, filename)
                tracksList.add(track)
            }
        }
        var trackAdapter = TrackAdapter(tracksList)
        newRecyclerView.adapter = trackAdapter

        trackAdapter.setTrackClickListener(object: TrackAdapter.TrackClickListener{
            override fun onTrackClick(position: Int, filename: String) {
                Intent(this@PlayerMenu,PlayerActivity::class.java).also{
                    it.putExtra("filename",filename)
                    startActivity(it)
                }
            }
        })
    }

    private suspend fun saveRating(filename: String, rating: Int){
        val dataStoreKey = intPreferencesKey(filename)
        dataStore.edit{ ratings ->
            ratings[dataStoreKey] = rating
        }
    }

    private suspend fun getRating (filename: String): Int {
        var trackRating = 0
        val dataStoreKey = intPreferencesKey(filename)
        val preferences = dataStore.data.first()
        val ratingStored = preferences[dataStoreKey]
        if(ratingStored is Int){
            trackRating = preferences[dataStoreKey]!!
        }
        return trackRating
    }
}