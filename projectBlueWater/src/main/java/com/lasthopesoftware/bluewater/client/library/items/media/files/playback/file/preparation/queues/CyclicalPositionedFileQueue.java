package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFile;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Created by david on 9/26/16.
 */
class CyclicalPositionedFileQueue implements IPositionedFileQueue
{
	private final Queue<PositionedFile> playlist;

	CyclicalPositionedFileQueue(List<PositionedFile> playlist) {
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
}
