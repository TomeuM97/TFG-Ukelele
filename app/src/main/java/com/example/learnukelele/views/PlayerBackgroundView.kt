package com.example.learnukelele.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class PlayerBackgroundView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val brush = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw a solid color background
        canvas.drawColor(Color.BLUE)

        //We paint the background color
        val colorOnPrimary = getThemeColor(context, com.google.android.material.R.attr.colorOnPrimary)
        brush.color = colorOnPrimary
        canvas.drawRect(Rect(0,0,canvas.width,canvas.height), brush)

        //We paint the guitar/ukelele neck and strings
        val colorPrimary = getThemeColor(context, com.google.android.material.R.attr.colorPrimary)
        val colorSecondary = getThemeColor(context, com.google.android.material.R.attr.colorSecondary)
        val colorTertiary = getThemeColor(context, com.google.android.material.R.attr.colorTertiary)
        //Paint neck
        brush.color = colorSecondary
        canvas.drawRect(Rect((canvas.width*0.10).toInt(),0,(canvas.width*0.90).toInt(),canvas.height),brush)
        //Paint neck border
        brush.color = colorPrimary
        brush.strokeWidth = 10F
        canvas.drawLine((canvas.width*0.10).toFloat(),0F,(canvas.width*0.10).toFloat(),canvas.height.toFloat(),brush)
        canvas.drawLine((canvas.width*0.90).toFloat(),0F,(canvas.width*0.90).toFloat(),canvas.height.toFloat(),brush)
        //Paint strings
        brush.color = colorTertiary
        canvas.drawLine((canvas.width*0.20).toFloat(),0F,(canvas.width*0.20).toFloat(),canvas.height.toFloat(),brush)
        canvas.drawLine((canvas.width*0.40).toFloat(),0F,(canvas.width*0.40).toFloat(),canvas.height.toFloat(),brush)
        canvas.drawLine((canvas.width*0.60).toFloat(),0F,(canvas.width*0.60).toFloat(),canvas.height.toFloat(),brush)
        canvas.drawLine((canvas.width*0.80).toFloat(),0F,(canvas.width*0.80).toFloat(),canvas.height.toFloat(),brush)
    }
}
// Function to get a color from themes.xml
fun getThemeColor(context: Context, attrRes: Int): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue.data
}
