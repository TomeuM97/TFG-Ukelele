package com.example.learnukelele.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs


class TunerMarkerView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var TunerMarkerBitmap: Bitmap? = null
    private var brush = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (TunerMarkerBitmap == null){
            TunerMarkerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        canvas.drawBitmap(TunerMarkerBitmap!!, 0f, 0f, brush)
    }

    fun updateFrame(positionPercentFromMiddle: Float, color: Int) {
        val canvas = Canvas(TunerMarkerBitmap!!)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        brush.color = color
        //Draw bar in location recived and draw bar in the edge if percent is > 0.5
        if(abs(positionPercentFromMiddle)>0.5){
            if(positionPercentFromMiddle>0){
                canvas.drawRect(0f, 0f, 0f+3f, canvas.height.toFloat(), brush)
            }else{
                canvas.drawRect(canvas.width.toFloat()-3f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), brush)
            }
        }else{
            canvas.drawRect(positionPercentFromMiddle*canvas.width+canvas.width/2-3f, 0f, positionPercentFromMiddle*canvas.width+canvas.width/2+3f, canvas.height.toFloat(), brush)
        }
        //Redraw View
        invalidate()
    }

    fun eraseCanvas(){
        val canvas = Canvas(TunerMarkerBitmap!!)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }
}