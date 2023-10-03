package com.example.learnukelele.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.learnukelele.R


class NotePlacerView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var notePlacerBitmap: Bitmap? = null
    private var brush = Paint()
    private var notes = arrayOf<Int?>(null, null, null, null)
    private var stringOrder: Int = 0

    val colorPrimary = getThemeColor(context, com.google.android.material.R.attr.colorPrimary)
    private var notesColor = arrayOf(
        ContextCompat.getColor(context, R.color.string1),
        ContextCompat.getColor(context, R.color.string2),
        ContextCompat.getColor(context, R.color.string3),
        ContextCompat.getColor(context, R.color.string4)
    )
    private val textPaint = Paint().apply{
        color = Color.BLACK
        textSize = 60f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    var pressX = 0
    var pressY = 0

    private lateinit var notePlacerListener: NotePlacerListener

    interface NotePlacerListener{
        fun onNotePlacerClick(string : Int, value: Int?)
    }

    fun setNotePlacerListener(listener: NotePlacerListener){
        notePlacerListener = listener
    }

    fun setNotesArray(notes: Array<Int?>) {
        this.notes = notes
    }

    fun setStringOrder(stringOrder: Int) {
        this.stringOrder = stringOrder
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (notePlacerBitmap == null){
            notePlacerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        drawCanvas(notes)
        canvas.drawBitmap(notePlacerBitmap!!, 0f, 0f, brush)
    }

    //Function where we draw the center line and the notes in the array
    private fun drawCanvas(notes: Array<Int?>){
        val canvas = Canvas(notePlacerBitmap!!)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        brush.strokeWidth = 10F
        brush.color = colorPrimary
        //We draw the line
        canvas.drawLine((canvas.width*0.10).toFloat(),canvas.height/2f,(canvas.width*0.90).toFloat(),canvas.height/2f,brush)

        //We draw the notes
        for ((string, note) in notes.withIndex()){
            if (note != null){
                brush.color = notesColor[string]

                val coordinateX = if(stringOrder == 0){
                    (string+1)*0.2f*canvas.width
                }else {
                    kotlin.math.abs(string - 4) *0.2f*canvas.width
                }
                val coordinateY = canvas.height/2f
                val fretTextCoordinateY = coordinateY - (textPaint.descent() + textPaint.ascent()) / 2

                canvas.drawCircle(coordinateX,coordinateY,50F,brush)
                canvas.drawText(note.toString(), coordinateX, fretTextCoordinateY, textPaint)
            }
        }

        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pressX = event.x.toInt()
                pressY = event.y.toInt()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val releaseX = event.x.toInt()
                val releaseY = event.y.toInt()
                // Check if the release and previous press coordinates are within the press Areas
                for((index, notesPressArea) in getNotePressAreas().withIndex()){
                    if (notesPressArea.contains(pressX, pressY)&&notesPressArea.contains(releaseX, releaseY)) {
                        showNoteSelectorDialog(index)
                        return true // Return true to stop the the event
                    }
                }
            }
        }
        return super.onTouchEvent(event) // Return the result of the superclass implementation
    }

    private fun getNotePressAreas(): Array<Rect>{
        val width = right - left
        val length = bottom - top

        return arrayOf(
            Rect((width*0.15).toInt(),(length*0.2).toInt(),(width*0.25).toInt(),(length*0.8).toInt()),
            Rect((width*0.35).toInt(),(length*0.2).toInt(),(width*0.45).toInt(),(length*0.8).toInt()),
            Rect((width*0.55).toInt(),(length*0.2).toInt(),(width*0.65).toInt(),(length*0.8).toInt()),
            Rect((width*0.75).toInt(),(length*0.2).toInt(),(width*0.85).toInt(),(length*0.8).toInt())
        )
    }

    private fun showNoteSelectorDialog(string: Int) {
        // Create an alert builder
        val builder = AlertDialog.Builder(context)

        // set the custom layout
        val customLayout: View = LayoutInflater.from(context).inflate(R.layout.dialog_note_creation, null)
        builder.setView(customLayout)

        val deleteNote = customLayout.findViewById<TextView>(R.id.deleteNoteButton)
        val noteSelector: Array<Button> = arrayOf(
            customLayout.findViewById(R.id.note0), customLayout.findViewById(R.id.note1), customLayout.findViewById(R.id.note2), customLayout.findViewById(R.id.note3),
            customLayout.findViewById(R.id.note4), customLayout.findViewById(R.id.note5), customLayout.findViewById(R.id.note6), customLayout.findViewById(R.id.note7),
            customLayout.findViewById(R.id.note8), customLayout.findViewById(R.id.note9), customLayout.findViewById(R.id.note10), customLayout.findViewById(R.id.note11),
            customLayout.findViewById(R.id.note12), customLayout.findViewById(R.id.note13), customLayout.findViewById(R.id.note14), customLayout.findViewById(R.id.note15)
        )

        val dialog = builder.create()
        dialog.show()

        // Add click listeners to the buttons
        for ((index,fret) in noteSelector.withIndex()) {
            fret.setOnClickListener {
                notePlacerListener.onNotePlacerClick(string, index)
                dialog.dismiss()
            }
        }
        deleteNote.setOnClickListener {
            notePlacerListener.onNotePlacerClick(string, null)
            dialog.dismiss()
        }
    }
}

