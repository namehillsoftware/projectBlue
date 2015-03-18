package com.lasthopesoftware.bluewater.servers.library.items.playlists.access;

import com.lasthopesoftware.bluewater.servers.library.items.access.AbstractCollectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlists;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class PlaylistProvider extends AbstractCollectionProvider<Playlists, Playlist> {

    private static Playlists mPlaylists = new Playlists();

	public PlaylistProvider() {
		this(null);
	}
	
	public PlaylistProvider(HttpURLConnection connection) {
		super(connection, mPlaylists);
	}

    @Override
    protected List<Playlist> getItems(final HttpURLConnection connection, final Playlists playlists) throws Exception {
        final InputStream is = connection.getInputStream();
        try {
            final ArrayList<Playlist> streamResult = PlaylistRequest.GetItems(is);

            int i = 0;
            while (i < streamResult.size()) {
                if (streamResult.get(i).getParent() != null) streamResult.remove(i);
                else i++;
            }

            return streamResult;
        } finally {
            is.close();
        }
    }
}
