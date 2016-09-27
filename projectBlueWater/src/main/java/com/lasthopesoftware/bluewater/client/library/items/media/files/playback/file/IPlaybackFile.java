package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

public interface IPlaybackFile extends
		IPlaybackFilePreparation,
		IPlaybackFilePlaybackController,
		IMediaPlayerResourceManager,
		IPlaybackFileVolumeController,
		IPlaybackFileErrorBroadcaster {
	@NonNull IFile getFile();
}
