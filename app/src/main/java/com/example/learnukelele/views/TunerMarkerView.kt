package com.example.learnukelele.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.learnukelele.NoteScoreMark
import com.example.learnukelele.R
import org.json.JSONArray


class PlayerNotesView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var playerNoteBitmap: Bitmap? = null
    private var playerNotes: MutableList<Array<PlayerNote>> = mutableListOf()
    private val textPaint = Paint().apply{
        color = Color.BLACK
        textSize = 70f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private var brush = Paint()

    private lateinit var noteArriveListener: NoteArriveListener

    interface NoteArriveListener{
        fun onNoteArrive(playerNoteArray: Array<PlayerNote>)
    }

    fun setNoteArriveListener(listener: NoteArriveListener){
        noteArriveListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (playerNoteBitmap == null){
            playerNoteBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        canvas.drawBitmap(playerNoteBitmap!!, 0f, 0f, brush)
    }

    fun addNotes(notesArray: JSONArray) {
        val list = mutableListOf<PlayerNote>()
        for (i in 0 until notesArray.length()){
            if(!notesArray.isNull(i)){
                val brush = Paint().apply {
                    color = when(i){
                        1 -> ContextCompat.getColor(context, R.color.string1)
                        2 -> ContextCompat.getColor(context, R.color.string2)
                        3 -> ContextCompat.getColor(context, R.color.string3)
                        else -> ContextCompat.getColor(context, R.color.string4)
                    }
                }
                list.add(PlayerNote((playerNoteBitmap!!.width*(i+1)*0.2).toFloat(),0f,(i+1), notesArray.optInt(i), brush))
            }
        }
        playerNotes.add(list.toTypedArray())
    }

    fun clearAllNotes() {
        playerNotes = mutableListOf()
        val canvas = Canvas(playerNoteBitmap!!)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }

    fun updateFrame(notesScoreMark: MutableList<NoteScoreMark>) {
        val canvas = Canvas(playerNoteBitmap!!)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val moveAmount = 0.005f * playerNoteBitmap!!.height

        //List to store notes that we are going to delete later
        val notesToDelete : MutableList<Array<PlayerNote>> = mutableListOf()
        for (playerNotesArray in playerNotes){
            for (playerNote in playerNotesArray){
                //Draw the Note
                val fretTextCoordinateY = playerNote.y - (textPaint.descent() + textPaint.ascent()) / 2
                brush.color = Color.BLACK
                canvas.drawCircle(playerNote.x,playerNote.y,60F,brush)
                canvas.drawCircle(playerNote.x,playerNote.y,50F,playerNote.brush)
                canvas.drawText(playerNote.fret.toString(), playerNote.x, fretTextCoordinateY, textPaint)

                //Move it for next frame
                playerNote.y = playerNote.y + moveAmount
            }
            //If the note has reached the end add to delete list and trigger onNoteArrive
            if(playerNotesArray[0].y > playerNoteBitmap!!.height){
                notesToDelete.add(playerNotesArray)
                noteArriveListener.onNoteArrive(playerNotesArray)
            }
        }
        //List to store marks that we are going to delete later
        val marksToDelete : MutableList<NoteScoreMark> = mutableListOf()
        for (noteScoreMark in notesScoreMark){
            val rectCenter = (playerNoteBitmap!!.width*(noteScoreMark.string)*0.2).toFloat()
            val rectBottom = playerNoteBitmap!!.height.toFloat()
            canvas.drawRect(rectCenter-noteScoreMark.length,rectBottom - 30f,rectCenter+noteScoreMark.length,rectBottom,noteScoreMark.paint)
            noteScoreMark.length = noteScoreMark.length * 1.25f

            //If the mark gets to 50 of length we remove it
            if(noteScoreMark.length>60f){
                marksToDelete.add(noteScoreMark)
            }
        }
        //Delete notes indicated in notesToDelete
        for (playerNoteArray in notesToDelete){
            playerNotes.remove(playerNoteArray)
        }
        //Delete marks indicated in marksToDelete
        for (markToDelete in marksToDelete){
            notesScoreMark.remove(markToDelete)
        }
        //Redraw View
        invalidate()
    }

    fun drawCenteredText(text: String, paint: Paint, color: Int) {
        val canvas = Canvas(playerNoteBitmap!!)

        // Measure the width of the text
        val textWidth = paint.measureText(text)
        // Calculate the coordinates where the text should be drawn
        val x = (canvas.width - textWidth) / 2
        val y = (canvas.height - paint.textSize) / 2 + paint.textSize

        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawText(text, x, y, paint)

        // Draw the text on the Canvas
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawText(text, x, y, paint)



        invalidate()
    }

    fun eraseCanvas(){
        val canvas = Canvas(playerNoteBitmap!!)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }

}

data class PlayerNote(val x: Float, var y: Float, val string: Int, val fret: Int, val brush: Paint)