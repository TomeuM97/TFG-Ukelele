package com.example.learnukelele

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PermissionActivity : AppCompatActivity() {

    private var isRecordPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permision)

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                println("Can not use app without permissions")
            }
        })

        val btn = findViewById<Button>(R.id.permissionButton)
        btn.setOnClickListener {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 3)
        }

        GlobalScope.launch {
            checkPermissionsRoutine()
            startActivity(Intent(this@PermissionActivity,MainMenu::class.java))
        }
    }
    private suspend fun checkPermissionsRoutine(){
        while (!isRecordPermissionGranted){
            checkPermissions()
            delay(1000)
        }
    }

    private fun checkPermissions(){
        isRecordPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkPermissions()
        if(isRecordPermissionGranted){
            startActivity(Intent(this,MainMenu::class.java))
        }
    }
}