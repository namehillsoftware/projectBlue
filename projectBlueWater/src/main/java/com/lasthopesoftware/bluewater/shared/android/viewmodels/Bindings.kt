package com.lasthopesoftware.bluewater.shared.android.viewmodels

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter

@BindingAdapter("app:src")
fun loadImage(imageView: ImageView, src: Bitmap?) {
	imageView.setImageBitmap(src)
}

@BindingAdapter("app:isVisible")
fun toggleVisibility(view: View, isVisible: Boolean) {
	view.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
}
