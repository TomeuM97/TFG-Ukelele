package com.example.learnukelele

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.example.learnukelele.audio.notes
import com.example.learnukelele.audio.standardTuning
import com.example.learnukelele.database.TrackDatabaseHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "options")

class OptionsMenu : AppCompatActivity() {

    private lateinit var stringOrderOptions: Array<RadioButton>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options_menu)

        //Checkboxes
        val stringOrder = findViewById<RadioGroup>(R.id.stringOrderOptions)
        stringOrderOptions = arrayOf(
            findViewById(R.id.stringOrderOption1),
            findViewById(R.id.stringOrderOption2)
        )
        val notation = findViewById<RadioGroup>(R.id.notationOptions)
        val notationOptions: Array<RadioButton> = arrayOf(
            findViewById(R.id.notationOption1),
            findViewById(R.id.notationOption2)
        )

        lifecycleScope.launch {
            val checkedStringOrder = getSavedOption("stringOrder")
            stringOrderOptions[checkedStringOrder].isChecked = true

            val checkedNotation = getSavedOption("notation")
            setOrderOptionsStrings(checkedNotation)
            notationOptions[checkedNotation].isChecked = true
        }

        stringOrder.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.stringOrderOption1 -> {
                    lifecycleScope.launch {
                        saveOption("stringOrder", 0)
                    }
                }
                R.id.stringOrderOption2 -> {
                    lifecycleScope.launch {
                        saveOption("stringOrder", 1)
                    }
                }
            }
        }

        notation.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.notationOption1 -> {
                    lifecycleScope.launch {
                        saveOption("notation", 0)
                        setOrderOptionsStrings(0)
                    }
                }
                R.id.notationOption2 -> {
                    lifecycleScope.launch {
                        saveOption("notation", 1)
                        setOrderOptionsStrings(1)
                    }
                }
            }
        }


        //Buttons
        val repeatTutorialButton = findViewById<Button>(R.id.repeatTutorial)
        val scoreResetButton = findViewById<Button>(R.id.resetScores)
        repeatTutorialButton.setOnClickListener {
            lifecycleScope.launch {
                dataStore.edit{ options ->
                    options[booleanPreferencesKey("player_tutorial")] = true
                }
            }
            lifecycleScope.launch {
                dataStore.edit{ options ->
                    options[booleanPreferencesKey("tuner_tutorial")] = true
                }
            }
            lifecycleScope.launch {
                dataStore.edit{ options ->
                    options[booleanPreferencesKey("creator_tutorial")] = true
                }
            }
            val string = resources.getString(R.string.resetTutorial)
            val toast = Toast.makeText(this, string, Toast.LENGTH_SHORT) // in Activity
            toast.show()
        }
        scoreResetButton.setOnClickListener {
            showConfirmScoreResetDialog(getString(R.string.confirmationTextResetScores))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setOrderOptionsStrings(checkedNotation: Int) {
        if (checkedNotation==0){
            stringOrderOptions[0].text = "${notes[standardTuning[0]].name} - ${notes[standardTuning[1]].name} - ${notes[standardTuning[2]].name} - ${notes[standardTuning[3]].name}"
            stringOrderOptions[1].text = "${notes[standardTuning[3]].name} - ${notes[standardTuning[2]].name} - ${notes[standardTuning[1]].name} - ${notes[standardTuning[0]].name}"
        } else {
            stringOrderOptions[0].text = "${notes[standardTuning[0]].nameL} - ${notes[standardTuning[1]].nameL} - ${notes[standardTuning[2]].nameL} - ${notes[standardTuning[3]].nameL}"
            stringOrderOptions[1].text = "${notes[standardTuning[3]].nameL} - ${notes[standardTuning[2]].nameL} - ${notes[standardTuning[1]].nameL} - ${notes[standardTuning[0]].nameL}"
        }
    }

    private suspend fun saveOption(optionName: String, optionValue: Int){
        val dataStoreKey = intPreferencesKey(optionName)
        dataStore.edit{ options ->
            options[dataStoreKey] = optionValue
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

    private fun showConfirmScoreResetDialog(confirmationQuestion: String){
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
            val trackDatabaseHelper = TrackDatabaseHelper(this)
            trackDatabaseHelper.resetAllTrackScores()
            dialog.dismiss()
        }
    }
}


