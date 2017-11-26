package com.lasthopesoftware.bluewater.client.playback.queues;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;

class PositionedPreparedPlaybackFile {
	final PositionedFile positionedFile;
	final PreparedPlaybackFile preparedPlaybackFile;

	PositionedPreparedPlaybackFile(@NonNull PositionedFile positionedFile, PreparedPlaybackFile preparedPlaybackFile) {
		this.positionedFile = positionedFile;
		this.preparedPlaybackFile = preparedPlaybackFile;
	}

	boolean isEmpty() {
		return preparedPlaybackFile == null;
	}

	static PositionedPreparedPlaybackFile emptyHandler(PositionedFile positionedFile) {
		return new PositionedPreparedPlaybackFile(positionedFile, null);
	}
}
