package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileErrorListener;

public interface IPlaybackFile extends
		IPlaybackFilePreparation,
		IPlaybackFilePlaybackController,
		IMediaPlayerResourceManager,
		IPlaybackFileVolumeController {
	IFile getFile();

	/* Listener methods */
	void addOnFileErrorListener(OnFileErrorListener listener);
	void removeOnFileErrorListener(OnFileErrorListener listener);
}
