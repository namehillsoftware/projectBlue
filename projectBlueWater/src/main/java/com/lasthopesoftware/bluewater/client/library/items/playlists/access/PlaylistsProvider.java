package com.lasthopesoftware.bluewater.client.library.items.playlists.access;

import android.util.SparseArray;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.messenger.promise.queued.cancellation.CancellationToken;
import com.lasthopesoftware.providers.AbstractConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsProvider extends AbstractConnectionProvider<List<Playlist>> {

	private static Logger logger = LoggerFactory.getLogger(PlaylistsProvider.class);

    public static final String PlaylistsItemKey = "Playlists";

    private static List<Playlist> cachedPlaylists;
    private static SparseArray<Playlist> mappedPlaylists;
    private static UrlKeyHolder<Integer> urlKeyHolder;

    private final int playlistId;

    private final ConnectionProvider connectionProvider;

	public PlaylistsProvider(ConnectionProvider connectionProvider) {
		this(connectionProvider, -1);
	}
	
	public PlaylistsProvider(ConnectionProvider connectionProvider, int playlistId) {
		super(connectionProvider, PlaylistsItemKey + "/List");

		this.connectionProvider = connectionProvider;
        this.playlistId = playlistId;
	}

    @Override
    protected List<Playlist> getData(final HttpURLConnection connection, CancellationToken cancellation) throws IOException {

        final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), RevisionChecker.getRevision(connectionProvider));
        if (cachedPlaylists != null && urlKeyHolder.equals(PlaylistsProvider.urlKeyHolder))
            return getPlaylists(playlistId);

        if (cancellation.isCancelled()) return new ArrayList<>();

	    try {
			try (InputStream is = connection.getInputStream()) {
				final List<Playlist> streamResult = PlaylistRequest.GetItems(is);

				int i = 0;
				while (i < streamResult.size()) {
					if (streamResult.get(i).getParent() != null) streamResult.remove(i);
					else i++;
				}

				PlaylistsProvider.urlKeyHolder = urlKeyHolder;
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

        return mappedPlaylists.get(playlistId).getChildren();
    }

    private static void denormalizeAndMap(List<Playlist> items) {
        for (Playlist playlist : items) {
            mappedPlaylists.append(playlist.getKey(), playlist);
            if (playlist.getChildren().size() > 0) denormalizeAndMap(playlist.getChildren());
        }
    }
}
