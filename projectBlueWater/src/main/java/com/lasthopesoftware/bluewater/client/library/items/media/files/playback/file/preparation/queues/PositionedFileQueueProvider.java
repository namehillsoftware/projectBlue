package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 11/1/16.
 */
public class PositionedFileQueueProvider implements IPositionedFileQueueProvider {

	@Override
	public IPositionedFileQueue getCompletableQueue(List<IFile> playlist, int startingAt) {
		return new CompletingPositionedFileQueue(getTruncatedList(playlist, startingAt));
	}

	@Override
	public IPositionedFileQueue getCyclicalQueue(List<IFile> playlist, int startingAt) {
		final List<PositionedFile> truncatedList = getTruncatedList(playlist, startingAt);

		final int endingPosition = playlist.size() - truncatedList.size();
		for (int i = 0; i < endingPosition; i++)
			truncatedList.add(new PositionedFile(i, playlist.get(i)));

		return new CyclicalPositionedFileQueue(truncatedList);
	}

	private static List<PositionedFile> getTruncatedList(List<IFile> playlist, int startingAt) {
		final List<PositionedFile> positionedFiles = new ArrayList<>(playlist.size());

		for (int i = startingAt; i < playlist.size(); i++)
			positionedFiles.add(new PositionedFile(i, playlist.get(i)));

		return positionedFiles;
	}
}
