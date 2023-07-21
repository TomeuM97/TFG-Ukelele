package com.example.learnukelele

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.learnukelele.Adapter.TrackAdapter
import com.example.learnukelele.Dialog.OptionsMenuDialog
import com.example.learnukelele.Utils.ContextUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class OptionsMenu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options_menu)

        val opt1 = findViewById<Button>(R.id.option1)
        val opt2 = findViewById<Button>(R.id.option2)
        val opt3 = findViewById<Button>(R.id.option3)

        val optionsFragmentManager = supportFragmentManager

        val opt1MenuDialog = OptionsMenuDialog(resources.getString(R.string.option1_title), resources.getStringArray(R.array.option1_array), runBlocking { getSavedOption("language") })
        val opt2MenuDialog = OptionsMenuDialog(resources.getString(R.string.option2_title), resources.getStringArray(R.array.option2_array), runBlocking { getSavedOption("audio-input") })
        val opt3MenuDialog = OptionsMenuDialog(resources.getString(R.string.option3_title), resources.getStringArray(R.array.option3_array), runBlocking { getSavedOption("notation") })

        opt1MenuDialog.setSaveButtonClickListener(object: OptionsMenuDialog.SaveButtonClickListener{
            override fun onSaveButtonClick(checkedItem: Int) {
                println("you selected option $checkedItem")
                var locale: Locale
                runBlocking {
                    saveOption("language",checkedItem)
                    locale = when (checkedItem){
                        0 -> Locale("en")
                        1 -> Locale("es")
                        else -> Locale("ca")
                    }
                }
                println(locale.getDisplayName(locale))
                var contextUtils = ContextUtils.updateLocale(this@OptionsMenu, locale)
            }
        })
        opt2MenuDialog.setSaveButtonClickListener(object: OptionsMenuDialog.SaveButtonClickListener{
            override fun onSaveButtonClick(checkedItem: Int) {
                println("you selected option $checkedItem")
                runBlocking {
                    saveOption("audio-input",checkedItem)
                }
            }
        })
        opt3MenuDialog.setSaveButtonClickListener(object: OptionsMenuDialog.SaveButtonClickListener{
            override fun onSaveButtonClick(checkedItem: Int) {
                println("you selected option $checkedItem")
                runBlocking {
                    saveOption("notation",checkedItem)
                }
            }
        })

        opt1.setOnClickListener {
            opt1MenuDialog.show(optionsFragmentManager, "languageDialog")
        }
        opt2.setOnClickListener {
            opt2MenuDialog.show(optionsFragmentManager, "audioDialog")
        }
        opt3.setOnClickListener {
            opt3MenuDialog.show(optionsFragmentManager, "notationDialog")
        }
    }

    private suspend fun saveOption(optionName: String, optionValue: Int){
        val dataStoreKey = intPreferencesKey(optionName)
        dataStore.edit{ options ->
            options[dataStoreKey] = optionValue
        }
    }

    private suspend fun getSavedOption (option: String): Int {
        val dataStoreKey = intPreferencesKey(option)
        val preferences = dataStore.data.first()
        val optionSavedValue = preferences[dataStoreKey]
        return if(optionSavedValue is Int){
            optionSavedValue
        } else {
            0
        }
    }
}
