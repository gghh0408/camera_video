package com.camera.camera_video;

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import android.view.TextureView

class AutoFitTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {

    private var ratioWidth = 0
    private var ratioHeight = 0
    private var defTransform: Matrix? = null

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        ratioWidth = height
        ratioHeight = width
        requestLayout()
    }

    fun setAspectRatio(size: Size) {
        setAspectRatio(size.width, size.height)
    }

    override fun setTransform(transform: Matrix?) {
        if (defTransform == null) {
            defTransform = transform
        }
        super.setTransform(transform)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height)
        } else {
            setTransform(defTransform)
            setMeasuredDimension(width, (width * ratioHeight) / ratioWidth)
        }
    }
}