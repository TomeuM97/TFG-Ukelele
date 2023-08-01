package com.example.learnukelele.Adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.learnukelele.Model.Track
import com.example.learnukelele.R


class TrackCreatorAdapter(private val tracksList: ArrayList<Track>): RecyclerView.Adapter<TrackCreatorAdapter.MyViewHolder>() {

    private lateinit var trackClickListener: TrackClickListener

    interface TrackClickListener{
        fun onTrackClick(position : Int, filename: String)
    }

    fun setTrackClickListener(listener: TrackClickListener){
        trackClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        //LayoutInflater.from(parent.context).inflate(R.layout.creator_menu_item, parent, false)
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.player_menu_item, parent, false)
        return MyViewHolder(itemView, trackClickListener )
    }

    override fun getItemCount(): Int {
        return tracksList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = tracksList[position]
        holder.trackImage.setImageDrawable(currentItem.trackImage)
        holder.trackName.text = currentItem.trackTitle
        holder.trackAuthor.text = currentItem.trackAuthor
        holder.trackRating.text = ("${currentItem.trackRating.toString()} %")
        holder.filename = currentItem.filename
    }

    class MyViewHolder(itemView: View, listener: TrackClickListener) : RecyclerView.ViewHolder(itemView){
        val trackImage: ImageView = itemView.findViewById(R.id.trackImage)
        val trackName: TextView = itemView.findViewById(R.id.trackTitle)
        val trackAuthor: TextView = itemView.findViewById(R.id.trackAuthor)
        val trackRating: TextView = itemView.findViewById(R.id.trackRating)
        var filename: String = ""

        init {
            itemView.setOnClickListener{
                itemView.startAnimation(AnimationUtils.loadAnimation(itemView.context,R.anim.scale_up))
                itemView.startAnimation(AnimationUtils.loadAnimation(itemView.context,R.anim.scale_down))
                listener.onTrackClick(adapterPosition, filename)
            }
        }
    }
}