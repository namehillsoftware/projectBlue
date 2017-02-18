package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage;

import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by david on 1/29/17.
 */
public class NowPlayingRepository implements INowPlayingRepository {

	private static final Map<Integer, NowPlaying> nowPlayingCache = new ConcurrentHashMap<>();

	private final ISpecificLibraryProvider libraryProvider;
	private final ILibraryStorage libraryRepository;

	private volatile int libraryId = -1;

	public NowPlayingRepository(ISpecificLibraryProvider libraryProvider, ILibraryStorage libraryRepository) {
		this.libraryProvider = libraryProvider;
		this.libraryRepository = libraryRepository;
	}

	@Override
	public IPromise<NowPlaying> getNowPlaying() {
		if (nowPlayingCache.containsKey(libraryId))
			return new Promise<>(nowPlayingCache.get(libraryId));

		return
			libraryProvider
				.getLibrary()
				.thenPromise(library -> {
					libraryId = library.getId();

					return
						FileStringListUtilities
							.promiseParsedFileStringList(library.getSavedTracksString())
							.then(files -> {
								final NowPlaying nowPlaying =
									new NowPlaying(
										files,
										library.getNowPlayingId(),
										library.getNowPlayingProgress(),
										library.isRepeating());

								nowPlayingCache.put(libraryId, nowPlaying);

								return nowPlaying;
							});
				});
	}

	@Override
	public IPromise<NowPlaying> updateNowPlaying(NowPlaying nowPlaying) {
		if (libraryId < 0)
			return getNowPlaying().thenPromise(np -> updateNowPlaying(nowPlaying));

		nowPlayingCache.put(libraryId, nowPlaying);

		libraryProvider
			.getLibrary()
			.thenPromise(library -> {
				library.setNowPlayingId(nowPlaying.playlistPosition);
				library.setNowPlayingProgress(nowPlaying.filePosition);
				library.setRepeating(nowPlaying.isRepeating);

				return
					FileStringListUtilities
						.promiseSerializedFileStringList(nowPlaying.playlist)
						.thenPromise(serializedPlaylist -> {
							library.setSavedTracksString(serializedPlaylist);

							return libraryRepository.saveLibrary(library);
						});
			});

		return new Promise<>(nowPlaying);
	}
}
