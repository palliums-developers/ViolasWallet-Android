package com.violas.wallet.base.recycler

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecycleViewItemDivider(
    context: Context,
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int,
    val showFirstTop: Boolean = false,
    val onlyShowLastBottom: Boolean = false
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
