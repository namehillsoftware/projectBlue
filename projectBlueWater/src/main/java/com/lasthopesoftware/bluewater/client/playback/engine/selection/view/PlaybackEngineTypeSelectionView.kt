package com.lasthopesoftware.bluewater.client.playback.engine.selection.view

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType

class PlaybackEngineTypeSelectionView(private val context: Context) {
	fun buildPlaybackEngineTypeSelections(): Iterable<RadioButton> =
		PlaybackEngineType.values().map { playbackEngineType -> buildPlaybackEngineButton(playbackEngineType) }

	private fun buildPlaybackEngineButton(playbackEngineType: PlaybackEngineType): RadioButton {
		val radioButton = RadioButton(context)
		radioButton.layoutParams = LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT)
		radioButton.text = playbackEngineType.name
		radioButton.id = playbackEngineType.ordinal
		return radioButton
	}
}
