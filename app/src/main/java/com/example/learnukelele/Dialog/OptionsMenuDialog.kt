package com.example.learnukelele.Dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.fragment.app.DialogFragment
import com.example.learnukelele.Adapter.TrackAdapter
import com.example.learnukelele.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select

class OptionsMenuDialog(private val title: String, private val options: Array<String>, private val checkedItem: Int) : DialogFragment() {

    private lateinit var saveButtonListener: OptionsMenuDialog.SaveButtonClickListener

    interface SaveButtonClickListener{
        fun onSaveButtonClick(checkedItem: Int)
    }

    fun setSaveButtonClickListener(listener: OptionsMenuDialog.SaveButtonClickListener){
        saveButtonListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            var selectedItem: Int = checkedItem
            // Set the dialog title
            builder.setTitle(title)
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setSingleChoiceItems(options, checkedItem,
                    DialogInterface.OnClickListener { dialog, which ->
                        selectedItem = which
                    })
                // Set the action buttons
                .setPositiveButton("Guardar",
                    DialogInterface.OnClickListener { dialog, id ->
                        saveButtonListener.onSaveButtonClick(selectedItem)
                    })
                .setNegativeButton("Cancelar",
                    DialogInterface.OnClickListener { dialog, id ->
                        // We do nothing here
                    })

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}