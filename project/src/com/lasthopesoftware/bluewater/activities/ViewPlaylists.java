package com.lasthopesoftware.bluewater.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.adapters.PlaylistAdapter;
import com.lasthopesoftware.bluewater.activities.adapters.filelist.FileListAdapter;
import com.lasthopesoftware.bluewater.activities.common.LongClickFlipListener;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.activities.listeners.ClickFileListener;
import com.lasthopesoftware.bluewater.activities.listeners.ClickPlaylistListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.FileSystem;
import com.lasthopesoftware.bluewater.data.service.objects.FileSystem.OnGetFileSystemCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.data.service.objects.IItem;
import com.lasthopesoftware.bluewater.data.service.objects.Playlist;
import com.lasthopesoftware.bluewater.data.service.objects.Playlists;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class ViewPlaylists extends FragmentActivity {

	public static final String KEY = "com.lasthopesoftware.bluewater.activities.ViewPlaylist.key";
	private int mPlaylistId;
	private Playlist mPlaylist;

	private ProgressBar pbLoading;
	private ListView playlistView;

	private Context thisContext = this;
	
	private ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem<?>>> visibleViewsAsyncComplete;

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
        
        visibleViewsAsyncComplete = new ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem<?>>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, ArrayList<IItem<?>>> owner, ArrayList<IItem<?>> result) {
				if (owner.getState() == SimpleTaskState.ERROR) {
					for (Exception exception : owner.getExceptions()) {
						if (!(exception instanceof IOException)) continue;
						
						PollConnection.Instance.get(thisContext).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
							
							@Override
							public void onConnectionRegained() {
								FileSystem.Instance.get(thisContext, new OnGetFileSystemCompleteListener() {
									
									@Override
									public void onGetFileSystemComplete(FileSystem fileSystem) {
										fileSystem.getVisibleViewsAsync(visibleViewsAsyncComplete);
									}
								});
							}
						});
						
						PollConnection.Instance.get(thisContext).startPolling();
						
						thisContext.startActivity(new Intent(thisContext, WaitForConnection.class));
						break;
					}
					return;
				}
				
				if (result == null) return;
				
				for (IItem<?> item : result) {
					if (!item.getValue().equalsIgnoreCase("Playlist")) continue;
					
					mPlaylist = ((Playlists)item).getMappedPlaylists().get(mPlaylistId);
					break;
				}
				
				BuildPlaylistView();
			}
		};
		
		FileSystem.Instance.get(thisContext, new OnGetFileSystemCompleteListener() {
			
			@Override
			public void onGetFileSystemComplete(FileSystem fileSystem) {
				fileSystem.getVisibleViewsAsync(visibleViewsAsyncComplete);
			}
		});
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnection.restoreSessionConnection(this);
	}
	
	private void BuildPlaylistView() {
                
        if (mPlaylist.getSubItems().size() > 0) {
        	playlistView.setAdapter(new PlaylistAdapter(thisContext, R.id.tvStandard, mPlaylist.getSubItems()));
        	playlistView.setOnItemClickListener(new ClickPlaylistListener(this, mPlaylist.getSubItems()));
        	playlistView.setOnItemLongClickListener(new LongClickFlipListener());
        } else {
        	playlistView.setVisibility(View.INVISIBLE);
        	pbLoading.setVisibility(View.VISIBLE);
        	Files filesContainer = (Files)mPlaylist.getFiles();
        	filesContainer.setOnFilesCompleteListener(new OnCompleteListener<List<File>>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, List<File>> owner, List<File> result) {
					playlistView.setAdapter(new FileListAdapter(thisContext, R.id.tvStandard, result));
		        	playlistView.setOnItemClickListener(new ClickFileListener(mPlaylist.getFiles()));
		        	playlistView.setOnItemLongClickListener(new LongClickFlipListener());
		        	
		        	playlistView.setVisibility(View.VISIBLE);
		        	pbLoading.setVisibility(View.INVISIBLE);
				}
			});
        	filesContainer.getFilesAsync();
        }
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