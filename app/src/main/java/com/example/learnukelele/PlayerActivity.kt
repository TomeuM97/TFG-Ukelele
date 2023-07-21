package com.example.learnukelele

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import org.json.JSONObject

class PlayerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val filename = intent.getStringExtra("filename")
        Toast.makeText(this,"You clicked $filename", Toast.LENGTH_SHORT).show()

        //val trackInString = applicationContext.assets.open("trackdata/$filename").bufferedReader().use { it.readText() }
        //val trackInJson = JSONObject(trackInString)
        //val trackHeaderInJSON = trackInJson.getJSONObject("track-header")
        //val trackBodyInJSON = trackInJson.getJSONObject("track-data")
    }
}