package com.lasthopesoftware.bluewater.client.playback.engine.selection.view;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;

public class PlaybackEngineTypeSelectionView {

	private final Context context;

	public PlaybackEngineTypeSelectionView(Context context) {
		this.context = context;
	}

	public Stream<RadioButton> buildPlaybackEngineTypeSelections() {
		return Stream.of(PlaybackEngineType.values()).map(this::buildPlaybackEngineButton);
	}

	private RadioButton buildPlaybackEngineButton(PlaybackEngineType playbackEngineType) {
		final RadioButton radioButton = new RadioButton(context);

		radioButton.setLayoutParams(
			new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));

		radioButton.setText(playbackEngineType.name());
		radioButton.setId(playbackEngineType.ordinal());

		return radioButton;
	}
}
