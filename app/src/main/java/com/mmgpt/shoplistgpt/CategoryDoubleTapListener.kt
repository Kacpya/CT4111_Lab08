package com.mmgpt.shoplistgpt

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Detects a double-tap on a row of a categories list and reports which pager
 * page it should open.
 *
 * Fix for the intermittent double-tap bug: the old code fed the gesture
 * detector from RecyclerView.setOnTouchListener, which only receives events
 * when no child view in the row (checkbox, buttons) claims the touch first.
 * An OnItemTouchListener's onInterceptTouchEvent sees every event in the
 * RecyclerView before any child does, so the double-tap is detected no matter
 * where on the row it lands. It always returns false, so normal clicks,
 * scrolling and drag-to-reorder keep working.
 *
 * @param ignoredViewIds views inside a row (e.g. edit/delete buttons) where a
 *        double-tap must NOT navigate, because the taps belong to that button.
 * @param pageForPosition maps an adapter position to a pager page, or null if
 *        that row is not a category (e.g. an "add" placeholder row).
 */
class CategoryDoubleTapListener(
    context: Context,
    private val recyclerView: RecyclerView,
    private val ignoredViewIds: Set<Int> = emptySet(),
    private val pageForPosition: (Int) -> Int?,
    private val onNavigate: (Int) -> Unit,
) : RecyclerView.SimpleOnItemTouchListener() {

    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Events given to an OnItemTouchListener are already in the
            // RecyclerView's own coordinates, so no raw/screen conversion needed.
            val row = recyclerView.findChildViewUnder(e.x, e.y) ?: return false
            if (isOverIgnoredView(row, e)) return false
            val position = recyclerView.getChildAdapterPosition(row)
            if (position == RecyclerView.NO_POSITION) return false
            val page = pageForPosition(position) ?: return false
            onNavigate(page)
            return true
        }
    })

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        detector.onTouchEvent(e)
        return false // observe only; never steal the touch
    }

    private fun isOverIgnoredView(row: View, e: MotionEvent): Boolean {
        val rvLocation = IntArray(2)
        recyclerView.getLocationOnScreen(rvLocation)
        val screenX = rvLocation[0] + e.x
        val screenY = rvLocation[1] + e.y
        return ignoredViewIds.any { id ->
            val view = row.findViewById<View>(id)
            if (view == null || !view.isShown || !view.isEnabled) {
                false
            } else {
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                screenX >= location[0] && screenX < location[0] + view.width &&
                    screenY >= location[1] && screenY < location[1] + view.height
            }
        }
    }
}
