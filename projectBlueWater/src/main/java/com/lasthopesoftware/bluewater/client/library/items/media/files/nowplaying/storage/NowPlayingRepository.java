package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.PassThroughPromise;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by david on 1/29/17.
 */
public class NowPlayingRepository implements INowPlayingRepository {

	private final Context context;
	private final int libraryId;
	private final ReadWriteLock nowPlayingLock = new ReentrantReadWriteLock();

	private NowPlaying internalNowPlaying;

	public NowPlayingRepository(Context context, Library library) {
		this.context = context;
		this.libraryId = library.getId();
	}

	@Override
	public IPromise<NowPlaying> getNowPlaying() {
		nowPlayingLock.readLock().lock();
		try {
			if (internalNowPlaying != null)
				return new PassThroughPromise<>(
					new NowPlaying(
						internalNowPlaying.playlist,
						internalNowPlaying.playlistPosition,
						internalNowPlaying.filePosition,
						internalNowPlaying.isRepeating));
		} finally {
			nowPlayingLock.readLock().unlock();
		}

		return
			LibrarySession
				.getLibrary(context, libraryId)
				.thenPromise(library ->
					FileStringListUtilities
						.promiseParsedFileStringList(library.getSavedTracksString())
						.then(files -> {
							nowPlayingLock.writeLock().lock();
							try {
								return internalNowPlaying = new NowPlaying(files, library.getNowPlayingId(), library.getNowPlayingProgress(), library.isRepeating());
							} finally {
								nowPlayingLock.writeLock().unlock();
							}
						}));
	}

	@Override
	public IPromise<NowPlaying> updateNowPlaying(NowPlaying nowPlaying) {
		nowPlayingLock.writeLock().lock();
		try {
			internalNowPlaying = nowPlaying;
		} finally {
			nowPlayingLock.writeLock().unlock();
		}

		return
			LibrarySession
				.getLibrary(context, libraryId)
				.thenPromise(library -> {
					library.setNowPlayingId(nowPlaying.playlistPosition);
					library.setNowPlayingProgress(nowPlaying.filePosition);
					library.setRepeating(nowPlaying.isRepeating);

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
}
