package com.palliums.widget.dividers

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.palliums.content.ContextProvider

class RecycleViewItemDividers(
    val top: Int,
    val bottom: Int,
    val left: Int = 0,
    val right: Int = 0,
    val showFirstTop: Boolean = false,
    val onlyShowLastBottom: Boolean = false,
    val context: Context = ContextProvider.getContext()
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.left = left
        outRect.right = right
        if (!onlyShowLastBottom || parent.getChildAdapterPosition(view) == (parent.adapter!!.itemCount - 1)) {
            outRect.bottom = bottom
        }
        if (parent.getChildAdapterPosition(view) != 0 || showFirstTop) {
            outRect.top = top
        }
    }
}
