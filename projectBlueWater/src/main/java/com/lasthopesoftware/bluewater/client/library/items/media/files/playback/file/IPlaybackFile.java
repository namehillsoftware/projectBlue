package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.IPlaybackFileErrorBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackFilePreparation;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.resources.IMediaPlayerResourceManager;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.volume.IPlaybackFileVolumeController;

public interface IPlaybackFile extends
		IPlaybackFilePreparation,
		IPlaybackController,
		IMediaPlayerResourceManager,
		IPlaybackFileVolumeController,
		IPlaybackFileErrorBroadcaster {
	@NonNull IFile getFile();
}
