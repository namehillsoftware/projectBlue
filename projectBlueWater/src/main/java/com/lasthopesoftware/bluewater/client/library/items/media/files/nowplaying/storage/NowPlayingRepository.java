package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 1/29/17.
 */
public class NowPlayingRepository implements INowPlayingRepository {

	private final Context context;
	private final int libraryId;

	public NowPlayingRepository(Context context, Library library) {
		this.context = context;
		this.libraryId = library.getId();
	}

	@Override
	public IPromise<NowPlaying> updateNowPlaying(NowPlaying nowPlaying) {
		LibrarySession
			.getLibrary(context, libraryId)
			.thenPromise(library -> {
				library.setNowPlayingId(nowPlaying.playlistPosition);
				library.setNowPlayingProgress(nowPlaying.filePosition);

				return
					FileStringListUtilities
						.promiseSerializedFileStringList(nowPlaying.playlist)
						.thenPromise(serializedPlaylist -> {
							library.setSavedTracksString(serializedPlaylist);

							return LibrarySession.saveLibrary(context, library);
						})
						.then(savedLibrary -> nowPlaying);
			});
	}

	@Override
	public IPromise<NowPlaying> getNowPlaying() {
		return null;
	}
}
