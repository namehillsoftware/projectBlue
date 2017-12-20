package com.lasthopesoftware.bluewater.client.playback.file.preparation.mediaplayer;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.initialization.MediaPlayerInitializer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;

public class MediaPlayerPlayableFilePreparationSourceProvider implements IPlayableFilePreparationSourceProvider {

	private final IFileUriProvider fileUriProvider;
	private final Context context;
	private final Library library;

	public MediaPlayerPlayableFilePreparationSourceProvider(Context context, IFileUriProvider fileUriProvider, Library library) {
		this.fileUriProvider = fileUriProvider;
		this.context = context;
		this.library = library;
	}

	@Override
	public PlayableFilePreparationSource providePlayableFilePreparationSource() {
		return new MediaPlayerPlaybackPreparer(
			fileUriProvider,
			new MediaPlayerInitializer(context, library));
	}

	@Override
	public int getMaxQueueSize() {
		return 1;
	}
}
