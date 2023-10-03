package com.example.learnukelele.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.learnukelele.R
import com.example.learnukelele.database.Track


class TrackAdapter(private var tracksList: ArrayList<Track>): RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    private lateinit var trackClickListener: TrackClickListener

    interface TrackClickListener{
        fun onTrackClick(trackId : Int)
    }

    fun setTrackClickListener(listener: TrackClickListener){
        trackClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        //LayoutInflater.from(parent.context).inflate(R.layout.creator_menu_item, parent, false)
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.player_menu_item, parent, false)
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
        if(currentItem.score % 1 == 0.0){
            holder.trackRating.text = "${currentItem.score.toInt()} %"
        } else {
            holder.trackRating.text = "${currentItem.score} %"
        }
        holder.trackId = currentItem.id
    }

    class TrackViewHolder(itemView: View, listener: TrackClickListener) : RecyclerView.ViewHolder(itemView){
        val trackImage: ImageView = itemView.findViewById(R.id.trackImage)
        val trackName: TextView = itemView.findViewById(R.id.trackTitle)
        val trackAuthor: TextView = itemView.findViewById(R.id.trackAuthor)
        val trackRating: TextView = itemView.findViewById(R.id.trackRating)
        var trackId: Int = 0

        init {
            itemView.setOnClickListener{
                itemView.startAnimation(AnimationUtils.loadAnimation(itemView.context,R.anim.scale_up))
                itemView.startAnimation(AnimationUtils.loadAnimation(itemView.context,R.anim.scale_down))
                listener.onTrackClick(trackId)
            }
        }
    }

    fun setFilteredArrayList(filteredArrayList: ArrayList<Track>) {
        tracksList = filteredArrayList
        notifyDataSetChanged()
    }

}