package com.lasthopesoftware.bluewater.servers.library.items.playlists;

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
import com.lasthopesoftware.bluewater.servers.library.items.playlists.access.PlaylistProvider;
import com.lasthopesoftware.bluewater.shared.listener.LongClickFlipListener;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

import java.util.List;

public class PlaylistListActivity extends FragmentActivity {

    public static final String KEY = "com.lasthopesoftware.bluewater.servers.library.items.playlists.key";
    public static final String VALUE = "com.lasthopesoftware.bluewater.servers.library.items.playlists.value";
	private int mPlaylistId;

	private ProgressBar pbLoading;
	private ListView playlistView;

	private Context thisContext = this;

	@SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        playlistView = (ListView)findViewById(R.id.lvItems);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingItems);
        
        mPlaylistId = 0;
        if (savedInstanceState != null) mPlaylistId = savedInstanceState.getInt(KEY);
        if (mPlaylistId == 0) mPlaylistId = getIntent().getIntExtra(KEY, 0);
        
        playlistView.setVisibility(View.INVISIBLE);
    	pbLoading.setVisibility(View.VISIBLE);

        setTitle(getIntent().getStringExtra(VALUE));

        final PlaylistProvider playlistProvider = new PlaylistProvider();
        playlistProvider.onComplete(new ISimpleTask.OnCompleteListener<Void, Void, List<Playlist>>() {
			
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
				playlistProvider.execute();
			}
		})).execute();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnectionActivity.restoreSessionConnection(this);
	}
	
	private void BuildPlaylistView(final Playlist playlist) {
        playlistView.setAdapter(new PlaylistListAdapter(thisContext, R.id.tvStandard, playlist.getChildren()));
        playlistView.setOnItemClickListener(new ClickPlaylistListener(this, playlist.getChildren()));
        playlistView.setOnItemLongClickListener(new LongClickFlipListener());
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