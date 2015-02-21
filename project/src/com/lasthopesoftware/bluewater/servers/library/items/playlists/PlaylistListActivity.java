package com.lasthopesoftware.bluewater.servers.library.items.playlists;

import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.list.FileListAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.listeners.ClickFileListener;
import com.lasthopesoftware.bluewater.shared.listener.LongClickFlipListener;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;
import com.lasthopesoftware.threading.IDataTask.OnCompleteListener;

public class PlaylistListActivity extends FragmentActivity {

	public static final String KEY = "com.lasthopesoftware.bluewater.activities.ViewPlaylist.key";
	private int mPlaylistId;

	private ProgressBar pbLoading;
	private ListView playlistView;

	private Context thisContext = this;

	@SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_playlists);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        playlistView = (ListView)findViewById(R.id.lvPlaylist);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingPlaylist);
        
        mPlaylistId = 0;
        if (savedInstanceState != null) mPlaylistId = savedInstanceState.getInt(KEY);
        if (mPlaylistId == 0) mPlaylistId = getIntent().getIntExtra(KEY, 0);
        
        playlistView.setVisibility(View.INVISIBLE);
    	pbLoading.setVisibility(View.VISIBLE);
    	
        final PlaylistsProvider playlistsProvider = new PlaylistsProvider();
        playlistsProvider.onComplete(new ISimpleTask.OnCompleteListener<Void, Void, List<Playlist>>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Playlist>> owner, List<Playlist> result) {
				if (owner.getState() == SimpleTaskState.ERROR || result == null) return;
				
				BuildPlaylistView((new Playlists(0, result)).getMappedPlaylists().get(mPlaylistId));
				
				playlistView.setVisibility(View.VISIBLE);
	        	pbLoading.setVisibility(View.INVISIBLE);
			}
		}).onError(new HandleViewIoException(thisContext, new OnConnectionRegainedListener() {
					
			@Override
			public void onConnectionRegained() {
				playlistsProvider.execute();
			}
		})).execute();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnectionActivity.restoreSessionConnection(this);
	}
	
	private void BuildPlaylistView(final Playlist playlist) {
                
        if (playlist.getChildren().size() > 0) {
        	playlistView.setAdapter(new PlaylistListAdapter(thisContext, R.id.tvStandard, playlist.getChildren()));
        	playlistView.setOnItemClickListener(new ClickPlaylistListener(this, playlist.getChildren()));
        	playlistView.setOnItemLongClickListener(new LongClickFlipListener());
        	return;
        }
        
    	playlistView.setVisibility(View.INVISIBLE);
    	pbLoading.setVisibility(View.VISIBLE);
    	Files filesContainer = (Files)playlist.getFiles();
    	filesContainer.setOnFilesCompleteListener(new OnCompleteListener<List<IFile>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, List<IFile>> owner, List<IFile> result) {
				playlistView.setAdapter(new FileListAdapter(thisContext, R.id.tvStandard, result));
	        	playlistView.setOnItemClickListener(new ClickFileListener(playlist.getFiles()));
	        	playlistView.setOnItemLongClickListener(new LongClickFlipListener());
	        	
	        	playlistView.setVisibility(View.VISIBLE);
	        	pbLoading.setVisibility(View.INVISIBLE);
			}
		});
    	filesContainer.getFilesAsync();
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt(KEY, mPlaylistId);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mPlaylistId = savedInstanceState.getInt(KEY);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return ViewUtils.buildStandardMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (ViewUtils.handleNavMenuClicks(this, item)) return true;
		return super.onOptionsItemSelected(item);
	}
}