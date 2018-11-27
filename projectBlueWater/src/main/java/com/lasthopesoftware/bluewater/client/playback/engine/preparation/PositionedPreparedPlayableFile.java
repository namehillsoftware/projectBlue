package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import android.support.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;

class PositionedPreparedPlayableFile {
	final PositionedFile positionedFile;
	final PreparedPlayableFile preparedPlayableFile;

	PositionedPreparedPlayableFile(@NonNull PositionedFile positionedFile, PreparedPlayableFile preparedPlayableFile) {
		this.positionedFile = positionedFile;
		this.preparedPlayableFile = preparedPlayableFile;
	}

	boolean isEmpty() {
		return preparedPlayableFile == null;
	}

	static PositionedPreparedPlayableFile emptyHandler(PositionedFile positionedFile) {
		return new PositionedPreparedPlayableFile(positionedFile, null);
	}
}
