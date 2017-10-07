package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource.MediaSourceProvider;
import com.lasthopesoftware.messenger.promises.Promise;

final class ExoPlayerPlaybackPreparer implements IPlaybackPreparer {

	private final IFileUriProvider fileUriProvider;
	private final MediaSourceProvider mediaSourceProvider;

	ExoPlayerPlaybackPreparer(IFileUriProvider fileUriProvider, MediaSourceProvider mediaSourceProvider) {
		this.fileUriProvider = fileUriProvider;
		this.mediaSourceProvider = mediaSourceProvider;
	}

	@Override
	public Promise<PreparedPlaybackFile> promisePreparedPlaybackHandler(ServiceFile serviceFile, int preparedAt) {
		return
			fileUriProvider
				.getFileUri(serviceFile)
				.eventually(new ExoPlayerPreparerTask(preparedAt, mediaSourceProvider, playbackInitialization));
	}
}
