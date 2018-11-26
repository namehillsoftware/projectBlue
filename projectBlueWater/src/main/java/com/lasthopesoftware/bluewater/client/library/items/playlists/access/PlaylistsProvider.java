package com.lasthopesoftware.bluewater.client.library.items.playlists.access;

import android.util.SparseArray;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.providers.AbstractProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;

public final class PlaylistsProvider implements CancellableMessageWriter<List<Playlist>> {

	private static Logger logger = LoggerFactory.getLogger(PlaylistsProvider.class);

    public static final String PlaylistsItemKey = "Playlists";

    private static List<Playlist> cachedPlaylists;
    private static SparseArray<Playlist> mappedPlaylists;
    private static UrlKeyHolder<Integer> urlKeyHolder;

    private final IConnectionProvider connectionProvider;
	private final Integer serverRevision;
	private final int playlistId;

	public static Promise<List<Playlist>> promisePlaylists(IConnectionProvider connectionProvider) {
		return promisePlaylists(connectionProvider, -1);
	}

	public static Promise<List<Playlist>> promisePlaylists(IConnectionProvider connectionProvider, int playlistId) {
		return RevisionChecker
			.promiseRevision(connectionProvider)
			.eventually(serverRevision -> {
				final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serverRevision);
				if (cachedPlaylists != null && urlKeyHolder.equals(PlaylistsProvider.urlKeyHolder))
					return new Promise<>(getPlaylists(playlistId));

				return new QueuedPromise<>(new PlaylistsProvider(connectionProvider, serverRevision, playlistId), AbstractProvider.providerExecutor);
			});
	}

	private PlaylistsProvider(IConnectionProvider connectionProvider, Integer serverRevision, int playlistId) {
		this.connectionProvider = connectionProvider;
		this.serverRevision = serverRevision;
		this.playlistId = playlistId;
	}

	@Override
	public List<Playlist> prepareMessage(CancellationToken cancellationToken) throws Throwable {
		if (cancellationToken.isCancelled())
			throw new CancellationException("Retrieving the playlist was cancelled");

		try {
			final HttpURLConnection connection = connectionProvider.getConnection(PlaylistsItemKey + "/List");
			try (InputStream is = connection.getInputStream()) {
				final List<Playlist> streamResult = PlaylistRequest.GetItems(is);

				int i = 0;
				while (i < streamResult.size()) {
					if (streamResult.get(i).getParent() != null) streamResult.remove(i);
					else i++;
				}

				PlaylistsProvider.urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serverRevision);
				cachedPlaylists = streamResult;
				mappedPlaylists = null;
				return getPlaylists(playlistId);
			}
		} catch (IOException e) {
			logger.error("There was an error getting the inputstream", e);
			throw e;
		}
	}

	private static List<Playlist> getPlaylists(int playlistId) {
        if (playlistId == -1) return cachedPlaylists;

        if (mappedPlaylists == null) {
            mappedPlaylists = new SparseArray<>(cachedPlaylists.size());
            denormalizeAndMap(cachedPlaylists);
        }

        final Playlist playlist = mappedPlaylists.get(playlistId);
        return playlist != null ? playlist.getChildren() : Collections.emptyList();
	}

    private static void denormalizeAndMap(List<Playlist> items) {
        for (Playlist playlist : items) {
            mappedPlaylists.append(playlist.getKey(), playlist);
            if (playlist.getChildren().size() > 0) denormalizeAndMap(playlist.getChildren());
        }
    }
}
