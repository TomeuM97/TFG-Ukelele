package com.example.learnukelele.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.learnukelele.R
import com.example.learnukelele.db.Track
import org.json.JSONArray


class TrackCreatorAdapter(private var tracksList: ArrayList<Track>): RecyclerView.Adapter<TrackCreatorAdapter.MyViewHolder>() {

    private lateinit var trackClickListener: TrackClickListener

    interface TrackClickListener{
        fun onTrackClick(position : Int, trackBody: JSONArray)
    }

    fun setTrackClickListener(listener: TrackClickListener){
        trackClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.creator_menu_item, parent, false)
        return MyViewHolder(itemView, trackClickListener )
    }

    override fun getItemCount(): Int {
        return tracksList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = tracksList[position]
        holder.trackImage.setImageBitmap(currentItem.image)
        holder.trackName.text = currentItem.title
        holder.trackAuthor.text = currentItem.author
        holder.trackBody = currentItem.trackData
    }

    class MyViewHolder(itemView: View, listener: TrackClickListener) : RecyclerView.ViewHolder(itemView){
        val trackImage: ImageView = itemView.findViewById(R.id.trackImage)
        val trackName: TextView = itemView.findViewById(R.id.trackTitle)
        val trackAuthor: TextView = itemView.findViewById(R.id.trackAuthor)
        lateinit var trackBody: JSONArray

        init {
            itemView.setOnClickListener{
                listener.onTrackClick(adapterPosition, trackBody)
            }
        }
    }

    fun setFilteredArrayList(filteredArrayList: ArrayList<Track>) {
        tracksList = filteredArrayList
        notifyDataSetChanged()
    }
}