package com.lasthopesoftware.bluewater.client.playback.engine.view;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.playback.engine.LookupSelectedPlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.SelectPlaybackEngineType;

public class PlaybackEngineTypeSelectionView {

	private final Context context;
	private final LookupSelectedPlaybackEngineType selectedPlaybackEngineTypeLookup;
	private final SelectPlaybackEngineType playbackEngineTypeSelection;

	public PlaybackEngineTypeSelectionView(
		Context context,
		LookupSelectedPlaybackEngineType selectedPlaybackEngineTypeLookup,
		SelectPlaybackEngineType playbackEngineTypeSelection) {
		this.context = context;
		this.selectedPlaybackEngineTypeLookup = selectedPlaybackEngineTypeLookup;
		this.playbackEngineTypeSelection = playbackEngineTypeSelection;
	}

	public Stream<RadioButton> buildPlaybackEngineTypeSelections() {
		return Stream.of(PlaybackEngineType.values()).map(this::buildPlaybackEngineButton);
	}

	private RadioButton buildPlaybackEngineButton(PlaybackEngineType playbackEngineType) {
		final RadioButton radioButton = new RadioButton(context);

		radioButton.setSelected(selectedPlaybackEngineTypeLookup.getSelectedPlaybackEngineType() == playbackEngineType);
		radioButton.setLayoutParams(
			new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		radioButton.setText(playbackEngineType.name());
		radioButton.setOnClickListener(v ->
			playbackEngineTypeSelection.selectPlaybackEngine(playbackEngineType));

		return radioButton;
	}
}
