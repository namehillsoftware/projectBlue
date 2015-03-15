package com.lasthopesoftware.bluewater.servers.library.items.playlists;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.list.FileListActivity;

import java.util.ArrayList;

public class ClickPlaylistListener implements OnItemClickListener {

	private final ArrayList<Playlist> mPlaylists;
	private final Context mContext;
	
	public ClickPlaylistListener(Context context, ArrayList<Playlist> playlists) {
		mContext = context;
		mPlaylists = playlists;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Playlist playlist = mPlaylists.get(position);
        if (playlist.getChildren().size() > 0) {
            final Intent playlistIntent = new Intent(mContext, PlaylistListActivity.class);
            playlistIntent.putExtra(PlaylistListActivity.KEY, playlist.getKey());
            playlistIntent.putExtra(PlaylistListActivity.VALUE, playlist.getValue());
            mContext.startActivity(playlistIntent);

            return;
        }

        final Intent fileListIntent = new Intent(mContext, FileListActivity.class);
        fileListIntent.putExtra(FileListActivity.KEY, playlist.getKey());
        fileListIntent.putExtra(FileListActivity.VALUE, playlist.getValue());
        fileListIntent.setAction(FileListActivity.VIEW_PLAYLIST_FILES);
        mContext.startActivity(fileListIntent);
	}

}
