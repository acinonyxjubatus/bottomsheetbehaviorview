package com.andreyaleev.bottomsheet

import android.content.Context
import android.graphics.Point
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import kotlin.math.roundToInt

fun dpToPx(context: Context, dp: Float): Int {
    val displayMetrics = context.resources.displayMetrics
    val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics)
    return px.roundToInt()
}

fun calculateViewHeight(view: View): Int {
    val widthMeasureSpec =
        View.MeasureSpec.makeMeasureSpec(getScreenWidth(view.context), View.MeasureSpec.AT_MOST)
    val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    view.measure(widthMeasureSpec, heightMeasureSpec)
    return view.measuredHeight
}


fun getScreenWidth(context: Context): Int {
    Point().run {
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(
            this
        )
        return x
    }
}

fun getScreenHeight(context: Context): Int {
    Point().run {
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(
            this
        )
        return y
    }
}