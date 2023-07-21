package com.example.learnukelele

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainMenu : AppCompatActivity() {

    private var isRecordPermissionGranted = false
    private val RECORD_PERMISION_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestAudioRecordPermission()

        val btn1 = findViewById<Button>(R.id.mybutton)
        val btn2 = findViewById<Button>(R.id.mybutton2)
        val btn3 = findViewById<Button>(R.id.mybutton3)
        val btn4 = findViewById<Button>(R.id.mybutton4)

        btn1.setOnClickListener {
            startPlayerMenuActivity("song")
        }
        btn2.setOnClickListener {
            startPlayerMenuActivity("lesson")
        }
        btn3.setOnClickListener {
            startActivity(Intent(this, TunerActivity::class.java))
        }
        btn4.setOnClickListener {
            startActivity(Intent(this,OptionsMenu::class.java))
        }
    }

    private fun requestAudioRecordPermission() {
        isRecordPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!isRecordPermissionGranted){
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), RECORD_PERMISION_CODE)
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        isRecordPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if(!isRecordPermissionGranted){
            startActivity(Intent(this,PermissionActivity::class.java))
        }
    }

    private fun startPlayerMenuActivity ( type: String ){
        Intent(this,PlayerMenu::class.java).also{
            it.putExtra("type",type)
            startActivity(it)
        }
    }
}