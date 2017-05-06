package com.lasthopesoftware.bluewater.client.playback.queues;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Created by david on 9/26/16.
 */
class RepeatingPositionedFileQueue implements IPositionedFileQueue
{
	private final Queue<PositionedFile> playlist;

	RepeatingPositionedFileQueue(List<PositionedFile> playlist) {
		this.playlist = new ArrayDeque<>(playlist);
	}

	@Override
	public PositionedFile poll() {
		if (playlist.size() == 0)
			return null;

		final PositionedFile positionedFile = playlist.poll();
		playlist.offer(positionedFile);

		return positionedFile;
	}

	@Override
	public PositionedFile peek() {
		return playlist.size() > 0 ? playlist.peek() : null;
	}
}
