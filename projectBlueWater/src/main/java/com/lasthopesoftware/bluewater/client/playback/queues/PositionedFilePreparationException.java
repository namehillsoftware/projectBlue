package com.lasthopesoftware.bluewater.client.playback.queues;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;

import java.io.IOException;


public class PositionedFilePreparationException extends IOException {
	private final PositionedFile positionedFile;

	public PositionedFilePreparationException(PositionedFile positionedFile, IOException cause) {
		super(cause);
		this.positionedFile = positionedFile;
	}

	public PositionedFile getPositionedFile() {
		return positionedFile;
	}
}
