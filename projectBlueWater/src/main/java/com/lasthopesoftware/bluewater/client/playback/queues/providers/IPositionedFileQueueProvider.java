package com.lasthopesoftware.bluewater.client.playback.queues.providers;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.queues.IPositionedFileQueue;

import java.util.List;

/**
 * Created by david on 11/1/16.
 */

public interface IPositionedFileQueueProvider {
	IPositionedFileQueue provideQueue(List<ServiceFile> playlist, int startingAt);

	boolean isRepeating();
}
