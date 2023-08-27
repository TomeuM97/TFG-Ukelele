import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotePlacersScrollView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    private var playerNoteBitmap: Bitmap? = null
    private var brush = Paint()
    private var notePlacerMutableList: MutableList<NotePlacer> = mutableListOf()

    init {
        // Set a LinearLayoutManager to arrange items vertically
        layoutManager = LinearLayoutManager(context)
        // Set a custom adapter to the RecyclerView
        adapter = NotePlacerAdapter()
        // Add initial NotePlacers to the list
        addInitialNotePlacers(20)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (playerNoteBitmap == null) {
            playerNoteBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        canvas.drawBitmap(playerNoteBitmap!!, 0f, 0f, brush)
    }

    // Add NotePlacers to the mutableList
    private fun addInitialNotePlacers(count: Int) {
        for (i in 0 until count) {
            notePlacerMutableList.add(NotePlacer(0.0))
        }
        adapter?.notifyDataSetChanged()
    }

    // Custom adapter for the RecyclerView
    inner class NotePlacerAdapter : RecyclerView.Adapter<NotePlacerAdapter.NotePlacerViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotePlacerViewHolder {
            // Create a custom view for each item
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note_placer, parent, false)
            return NotePlacerViewHolder(view)
        }

        override fun onBindViewHolder(holder: NotePlacerViewHolder, position: Int) {
            // Bind data to the custom view
            val notePlacer = notePlacerMutableList[position]
            // Implement your logic to update the custom view based on NotePlacer data
            // For example, if you have TextViews for displaying the timestamp and fretsArray,
            // you can set their text accordingly here.
        }

        override fun getItemCount(): Int {
            // Return the number of items in the list
            return notePlacerMutableList.size
        }

        inner class NotePlacerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            // Initialize your custom view elements here
            // For example, if you have TextViews, you can find them using itemView.findViewById()
            // and store them as properties of this ViewHolder.
        }
    }

    data class NotePlacer(
        val timestamp: Double,
        var fretsArray: Array<Int?> = arrayOf(null, null, null, null),
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NotePlacer

            if (!fretsArray.contentEquals(other.fretsArray)) return false

            return true
        }

        override fun hashCode(): Int {
            return fretsArray.contentHashCode()
        }
    }
}
