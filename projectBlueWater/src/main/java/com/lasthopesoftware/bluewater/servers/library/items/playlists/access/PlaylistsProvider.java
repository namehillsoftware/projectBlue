package com.lasthopesoftware.bluewater.servers.library.items.playlists.access;

import android.util.SparseArray;

import com.lasthopesoftware.bluewater.servers.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.servers.library.items.access.AbstractCollectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsProvider extends AbstractCollectionProvider<Playlist> {

    private static List<Playlist> mCachedPlaylists;
    private static SparseArray<Playlist> mMappedPlaylists;
    private static Integer mRevision;

    private final int mPlaylistId;

    public PlaylistsProvider() {
        this(-1);
    }

	public PlaylistsProvider(int playlistId) {
		this(null, playlistId);
	}
	
	public PlaylistsProvider(HttpURLConnection connection, int playlistId) {
		super(connection, "Playlists/List");

        mPlaylistId = playlistId;
	}

    @Override
    protected List<Playlist> getItems(final HttpURLConnection connection, final String... params) throws Exception {

        final Integer revision = RevisionChecker.getRevision();
        if (mCachedPlaylists != null && revision.equals(mRevision))
            return getPlaylists(mPlaylistId);

        final InputStream is = connection.getInputStream();
        try {
            final ArrayList<Playlist> streamResult = PlaylistRequest.GetItems(is);

            int i = 0;
            while (i < streamResult.size()) {
                if (streamResult.get(i).getParent() != null) streamResult.remove(i);
                else i++;
            }

            mRevision = revision;
            mCachedPlaylists = streamResult;
            mMappedPlaylists = null;
            return getPlaylists(mPlaylistId);
        } finally {
            is.close();
        }
    }

    private static List<Playlist> getPlaylists(int playlistId) {
        if (playlistId == -1) return mCachedPlaylists;

        if (mMappedPlaylists == null) {
            mMappedPlaylists = new SparseArray<Playlist>(mCachedPlaylists.size());
            denormalizeAndMap(mCachedPlaylists);
        }

        return mMappedPlaylists.get(playlistId).getChildren();
    }

    private static void denormalizeAndMap(List<Playlist> items) {
        for (Playlist playlist : items) {
            mMappedPlaylists.append(playlist.getKey(), playlist);
            if (playlist.getChildren().size() > 0) denormalizeAndMap(playlist.getChildren());
        }
    }
}
