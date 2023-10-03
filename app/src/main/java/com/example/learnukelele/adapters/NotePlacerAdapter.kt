package com.example.learnukelele.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.recyclerview.widget.RecyclerView
import com.example.learnukelele.R
import com.example.learnukelele.dataStore
import com.example.learnukelele.views.NotePlacerView
import kotlinx.coroutines.flow.first


class NotePlacerAdapter(private var notesTimestamps: ArrayList<NoteTimestamp>, private var stringOrder: Int): RecyclerView.Adapter<NotePlacerAdapter.NotePlacerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotePlacerViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.creator_note_placer_item, parent, false)
        return NotePlacerViewHolder(itemView, notesTimestamps)
    }

    override fun getItemCount(): Int {
        return notesTimestamps.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: NotePlacerViewHolder, position: Int) {
        if(position%4 == 0){
            holder.notePlacerSecondsTimestamp.text = "${((notesTimestamps[position].timestamp%60).toInt())}s"
            holder.notePlacerMinutesTimestamp.text = "${((notesTimestamps[position].timestamp/60).toInt())} min"
        } else {
            holder.notePlacerSecondsTimestamp.text = ""
            holder.notePlacerMinutesTimestamp.text = ""
        }
        holder.notePlacerView.setNotesArray(notesTimestamps[position].notes)
        holder.notePlacerView.setStringOrder(stringOrder)
        holder.notePlacerPosition = position
    }

    class NotePlacerViewHolder(itemView: View, notesTimestamps: ArrayList<NoteTimestamp>) : RecyclerView.ViewHolder(itemView){
        var notePlacerSecondsTimestamp: TextView = itemView.findViewById(R.id.notePlacerSecondsTimestamp)
        var notePlacerMinutesTimestamp: TextView = itemView.findViewById(R.id.notePlacerMinutesTimestamp)
        var notePlacerView: NotePlacerView = itemView.findViewById(R.id.notePlacer)
        var notePlacerPosition = 0

        init {
            notePlacerView.setNotePlacerListener(object: NotePlacerView.NotePlacerListener{
                override fun onNotePlacerClick(string: Int, value: Int?) {
                    notesTimestamps[notePlacerPosition].notes[string] = value
                }
            })
        }
    }
}

data class NoteTimestamp(
    val timestamp: Double,
    var notes: Array<Int?>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoteTimestamp

        if (!notes.contentEquals(other.notes)) return false

        return true
    }

    override fun hashCode(): Int {
        return notes.contentHashCode()
    }
}

