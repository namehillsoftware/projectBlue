package com.lasthopesoftware.resources.viewmodels

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.databinding.BindingAdapter

@BindingAdapter("src")
fun loadImage(imageView: ImageView, src: Bitmap?) {
	imageView.setImageBitmap(src)
}
