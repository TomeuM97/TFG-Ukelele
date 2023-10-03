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
import com.example.learnukelele.R
import org.json.JSONArray


class PlayerNotesView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var playerNoteBitmap: Bitmap? = null
    private val textPaint = Paint().apply{
        color = Color.BLACK
        textSize = 70f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private var brush = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (playerNoteBitmap == null){
            playerNoteBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        canvas.drawBitmap(playerNoteBitmap!!, 0f, 0f, brush)
    }
    fun clearCanvas() {
        val canvas = Canvas(playerNoteBitmap!!)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }
    fun getCanvasWidth(): Int{
        val canvas = Canvas(playerNoteBitmap!!)
        return canvas.width
    }
    fun getCanvasHeight(): Int{
        val canvas = Canvas(playerNoteBitmap!!)
        return canvas.height
    }

    fun updateFrame(playerNotes:MutableList<Array<PlayerNote>>, notesScoreMark: MutableList<NoteScoreMark>) {
        val canvas = Canvas(playerNoteBitmap!!)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        for (playerNotesArray in playerNotes){
            for (playerNote in playerNotesArray){
                //Draw the Note
                val fretTextCoordinateY = playerNote.y - (textPaint.descent() + textPaint.ascent()) / 2
                brush.color = Color.BLACK
                canvas.drawCircle(playerNote.x,playerNote.y,60F,brush)
                canvas.drawCircle(playerNote.x,playerNote.y,50F,playerNote.brush)
                canvas.drawText(playerNote.fret.toString(), playerNote.x, fretTextCoordinateY, textPaint)
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

}

data class PlayerNote(val x: Float, var y: Float, val string: Int, val fret: Int, val brush: Paint, val timestamp: Float)
data class NoteScoreMark(val string: Int, var length: Float, val paint: Paint)