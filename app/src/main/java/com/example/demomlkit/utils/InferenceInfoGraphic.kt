package com.example.demomlkit.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.annotation.Nullable
import com.example.demomlkit.utils.GraphicOverlay.Graphic

/** Graphic instance for rendering inference info (latency, FPS, resolution) in an overlay view.  */
class InferenceInfoGraphic(
    private val overlay: GraphicOverlay,
    private val latency: Long, // Only valid when a stream of input images is being processed. Null for single image mode.
    private val framesPerSecond: Int? //@field:Nullable @param:Nullable
) : Graphic(overlay) {
    private val textPaint: Paint = Paint()

    companion object {
        private const val TEXT_COLOR = Color.WHITE
        private const val TEXT_SIZE = 60.0f
    }

    init {
        textPaint.color = TEXT_COLOR
        textPaint.textSize = TEXT_SIZE
        postInvalidate()
    }

    @Synchronized
    override fun draw(canvas: Canvas?) {
        val x = TEXT_SIZE * 0.5f
        val y = TEXT_SIZE * 1.5f
        if (canvas != null) {
//            canvas.drawText(
//                "InputImage size: " + overlay.imageWidth + "x" + overlay.imageHeight,
//                x,
//                y,
//                textPaint
//            )

            // Drawing FPS (if valid) and inference latency
            if (framesPerSecond != null)
                canvas.drawText(
                    "FPS: $framesPerSecond, Latency: $latency ms",
                    x,
                    y + TEXT_SIZE,
                    textPaint
                )
            else
                canvas.drawText("Latency: $latency ms", x, y + TEXT_SIZE, textPaint)
        }
    }
}