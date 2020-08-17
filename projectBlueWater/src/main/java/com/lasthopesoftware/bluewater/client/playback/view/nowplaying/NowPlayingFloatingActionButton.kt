package com.lasthopesoftware.bluewater.client.playback.view.nowplaying

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFileProvider.Companion.fromActiveLibrary
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.NowPlayingActivity.Companion.startNowPlayingActivity
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils

class NowPlayingFloatingActionButton private constructor(context: Context) : FloatingActionButton(context) {
	private var isNowPlayingFileSet = false

	private fun initializeNowPlayingFloatingActionButton() {
		setOnClickListener { v -> startNowPlayingActivity(v.context) }

		visibility = ViewUtils.getVisibility(false)
		// The user can change the library, so let's check if the state of visibility on the
		// now playing menu item should change

		fromActiveLibrary(context)
			?.nowPlayingFile
			?.then { result ->
				isNowPlayingFileSet = result != null
				visibility = ViewUtils.getVisibility(isNowPlayingFileSet)
				if (isNowPlayingFileSet) return@then

				val localBroadcastManager = LocalBroadcastManager.getInstance(context)
				localBroadcastManager.registerReceiver(object : BroadcastReceiver() {
					@Synchronized
					override fun onReceive(context: Context, intent: Intent) {
						isNowPlayingFileSet = true
						visibility = ViewUtils.getVisibility(true)
						localBroadcastManager.unregisterReceiver(this)
					}
				}, IntentFilter(PlaylistEvents.onPlaylistStart))
			}
	}

	override fun show() {
		if (isNowPlayingFileSet) super.show()
	}

	override fun show(listener: OnVisibilityChangedListener?) {
		if (isNowPlayingFileSet) super.show(listener)
	}

	companion object {
		@JvmStatic
		fun addNowPlayingFloatingActionButton(container: RelativeLayout): NowPlayingFloatingActionButton {
			val nowPlayingFloatingActionButton = NowPlayingFloatingActionButton(container.context)
			val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END)
			val margin = ViewUtils.dpToPx(container.context, 16)
			layoutParams.setMargins(margin, margin, margin, margin)
			nowPlayingFloatingActionButton.layoutParams = layoutParams
			container.addView(nowPlayingFloatingActionButton)
			return nowPlayingFloatingActionButton
		}
	}

	init {
		setImageDrawable(ViewUtils.getDrawable(context, R.drawable.av_play_dark))
		initializeNowPlayingFloatingActionButton()
	}
}
