package com.lasthopesoftware.bluewater.servers.library.items.playlists;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import com.lasthopesoftware.bluewater.data.service.access.PlaylistRequest;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.AbstractCollectionProvider;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class PlaylistsProvider extends AbstractCollectionProvider<Playlist> {

	private static final String mPlaylistParams = "Playlists/List";
	
	public PlaylistsProvider() {
		this(null);
	}
	
	public PlaylistsProvider(HttpURLConnection connection) {
		super(connection, mPlaylistParams);
	}
	
	@Override
	protected SimpleTask<Void, Void, List<Playlist>> getNewTask() {
		final SimpleTask<Void, Void, List<Playlist>> getPlaylistsTask = new SimpleTask<Void, Void, List<Playlist>>(new OnExecuteListener<Void, Void, List<Playlist>>() {
			
			@Override
			public List<Playlist> onExecute(ISimpleTask<Void, Void, List<Playlist>> owner, Void... voidParams) throws Exception {
				final HttpURLConnection conn = mConnection == null ? ConnectionProvider.getConnection(mParams) : mConnection;

				try {
					final InputStream is = conn.getInputStream();
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
				} finally {
					if (mConnection == null) conn.disconnect();
				}
			}
		});
		

		if (mOnGetItemsComplete != null)
			getPlaylistsTask.addOnCompleteListener(mOnGetItemsComplete);
		
		if (mOnGetItemsError != null)
			getPlaylistsTask.addOnErrorListener(mOnGetItemsError);
		
		return getPlaylistsTask;
	}

}
