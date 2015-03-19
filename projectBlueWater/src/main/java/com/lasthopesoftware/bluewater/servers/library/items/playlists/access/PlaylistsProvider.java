package com.lasthopesoftware.bluewater.servers.library.items.playlists.access;

import com.lasthopesoftware.bluewater.servers.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.servers.library.items.access.AbstractCollectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsProvider extends AbstractCollectionProvider<Playlist> {

    private static List<Playlist> mCachedPlaylists;
    private static Integer mRevision;

	public PlaylistsProvider() {
		this(null);
	}
	
	public PlaylistsProvider(HttpURLConnection connection) {
		super(connection, "Playlists/List");
	}

    @Override
    protected List<Playlist> getItems(final HttpURLConnection connection, final String... params) throws Exception {

        final Integer revision = RevisionChecker.getRevision();
        if (mCachedPlaylists != null && revision.equals(mRevision))
            return mCachedPlaylists;

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
            return streamResult;
        } finally {
            is.close();
        }
    }
}
