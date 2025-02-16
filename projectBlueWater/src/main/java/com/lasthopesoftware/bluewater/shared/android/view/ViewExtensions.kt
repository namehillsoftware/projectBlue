package com.lasthopesoftware.bluewater.shared.android.view

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

@Composable
fun findWindow(): Window? =
	(LocalView.current.parent as? DialogWindowProvider)?.window
		?: LocalView.current.context.findWindow()

private tailrec fun Context.findWindow(): Window? =
	when (this) {
		is Activity -> window
		is ContextWrapper -> baseContext.findWindow()
		else -> null
	}
