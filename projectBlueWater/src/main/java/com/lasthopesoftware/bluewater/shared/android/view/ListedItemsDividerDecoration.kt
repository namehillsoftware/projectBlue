package com.lasthopesoftware.bluewater.shared.android.view

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView

class ListedItemsDividerDecoration(private val drawable: Drawable) : RecyclerView.ItemDecoration() {

	// Sourced from https://stackoverflow.com/a/31243174
	override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		val left = parent.paddingLeft
		val right = parent.width - parent.paddingRight
		val childCount = parent.childCount

		for (i in 0 until childCount - 1) {
			val child = parent.getChildAt(i)
			val params = child.layoutParams as RecyclerView.LayoutParams
			val top = child.bottom + params.bottomMargin
			val bottom = top + drawable.intrinsicHeight
			drawable.setBounds(left, top, right, bottom)
			drawable.draw(c)
		}
	}
}
