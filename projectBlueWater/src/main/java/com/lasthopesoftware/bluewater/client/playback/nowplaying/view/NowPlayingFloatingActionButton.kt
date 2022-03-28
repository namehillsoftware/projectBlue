package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import android.content.Context
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingFileProvider.Companion.fromActiveLibrary
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.NowPlayingActivity.Companion.startNowPlayingActivity
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistMessages.PlaybackStarted
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils.getThemedDrawable
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise

class NowPlayingFloatingActionButton private constructor(context: Context) : FloatingActionButton(context) {
	private var isNowPlayingFileSet = false

	init {
		setImageDrawable(context.getThemedDrawable(R.drawable.av_play_dark))
		initializeNowPlayingFloatingActionButton()
	}

	private fun initializeNowPlayingFloatingActionButton() {
		setOnClickListener { v -> startNowPlayingActivity(v.context) }

		visibility = ViewUtils.getVisibility(false)
		// The user can change the library, so let's check if the state of visibility on the
		// now playing menu item should change

		fromActiveLibrary(context)
			.then {
				it
					?.nowPlayingFile
					?.eventually(LoopedInPromise.response({result ->
						isNowPlayingFileSet = result != null
						visibility = ViewUtils.getVisibility(isNowPlayingFileSet)
						if (isNowPlayingFileSet) return@response

						val applicationMessageBus = ApplicationMessageBus.getInstance()
						applicationMessageBus.registerReceiver(object : (PlaybackStarted) -> Unit {
							override fun invoke(p1: PlaybackStarted) {
								isNowPlayingFileSet = true
								visibility = ViewUtils.getVisibility(true)
								applicationMessageBus.unregisterReceiver(this)
							}
						})
					}, context))
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
}
