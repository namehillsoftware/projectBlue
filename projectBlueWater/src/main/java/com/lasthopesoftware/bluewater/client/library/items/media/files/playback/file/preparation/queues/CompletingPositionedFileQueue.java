package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedFile;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Created by david on 9/26/16.
 */
class CompletingPositionedFileQueue implements IPositionedFileQueue {
	private final Queue<PositionedFile> playlist;

	CompletingPositionedFileQueue(List<PositionedFile> playlist) {
		this.playlist = new ArrayDeque<>(playlist);
	}

	@Override
	public PositionedFile poll() {
		return playlist.size() > 0 ? playlist.poll() : null;
	}

	@Override
	public PositionedFile peek() {
		return playlist.size() > 0 ? playlist.peek() : null;
	}
}
