package com.example.learnukelele.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.learnukelele.R
import com.example.learnukelele.database.Track


class TrackCreatorAdapter(private var tracksList: ArrayList<Track>): RecyclerView.Adapter<TrackCreatorAdapter.TrackViewHolder>() {

    private lateinit var trackClickListener: TrackClickListener

    interface TrackClickListener{
        fun onTrackEditClick(trackId : Int)
        fun onTrackDeleteClick(trackId: Int)
    }

    fun setTrackClickListener(listener: TrackClickListener){
        trackClickListener = listener
    }
    fun setNewArrayList(newArrayList: ArrayList<Track>) {
        tracksList = newArrayList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.creator_menu_item, parent, false)
        return TrackViewHolder(itemView, trackClickListener )
    }

    override fun getItemCount(): Int {
        return tracksList.size
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val currentItem = tracksList[position]
        holder.trackImage.setImageBitmap(currentItem.image)
        holder.trackName.text = currentItem.title
        holder.trackAuthor.text = currentItem.author
        holder.trackId = currentItem.id
    }

    class TrackViewHolder(itemView: View, listener: TrackClickListener) : RecyclerView.ViewHolder(itemView){
        val trackImage: ImageView = itemView.findViewById(R.id.trackImage)
        val trackName: TextView = itemView.findViewById(R.id.trackTitle)
        val trackAuthor: TextView = itemView.findViewById(R.id.trackAuthor)
        var trackId: Int = 0

        init {
            val editButton = itemView.findViewById<ImageButton>(R.id.editButton)
            val deleteButton = itemView.findViewById<ImageButton>(R.id.deleteButton)
            editButton.setOnClickListener {
                listener.onTrackEditClick(trackId)
            }
            deleteButton.setOnClickListener {
                listener.onTrackDeleteClick(trackId)
            }
        }
    }
}