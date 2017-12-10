package com.lasthopesoftware.bluewater.client.playback.queues.providers;


import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;

import java.util.ArrayList;
import java.util.List;

class QueueSplicers {
	static List<PositionedFile> getTruncatedList(List<ServiceFile> playlist, int startingAt) {
		final List<PositionedFile> positionedFiles = new ArrayList<>(playlist.size());

		for (int i = startingAt; i < playlist.size(); i++)
			positionedFiles.add(new PositionedFile(i, playlist.get(i)));

		return positionedFiles;
	}
}
