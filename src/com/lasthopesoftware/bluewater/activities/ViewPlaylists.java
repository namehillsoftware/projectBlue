package com.lasthopesoftware.bluewater.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.lasthopesoftware.bluewater.activities.adapters.FileListAdapter;
import com.lasthopesoftware.bluewater.activities.adapters.PlaylistAdapter;
import com.lasthopesoftware.bluewater.activities.common.BrowseItemMenu;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.activities.listeners.ClickFileListener;
import com.lasthopesoftware.bluewater.activities.listeners.ClickPlaylistListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.objects.JrFile;
import com.lasthopesoftware.bluewater.data.objects.JrFiles;
import com.lasthopesoftware.bluewater.data.objects.JrItem;
import com.lasthopesoftware.bluewater.data.objects.JrPlaylist;
import com.lasthopesoftware.bluewater.data.objects.JrPlaylists;
import com.lasthopesoftware.bluewater.data.objects.JrSession;
import com.lasthopesoftware.threading.ISimpleTask;

public class ViewPlaylists extends FragmentActivity {

	public static final String KEY = "com.lasthopesoftware.ViewPlaylist.key";
	private int mPlaylistId;
	private JrPlaylist mPlaylist;

	private ProgressBar pbLoading;
	private ListView playlistView;

	private Context mContext;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_playlists);
        mContext = this;
        playlistView = (ListView)findViewById(R.id.lvPlaylist);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingPlaylist);
        
        mPlaylistId = 0;
        if (savedInstanceState != null) mPlaylistId = savedInstanceState.getInt(KEY);
        if (mPlaylistId == 0) mPlaylistId = getIntent().getIntExtra(KEY, 0);
        
        JrSession.JrFs.getVisibleViewsAsync(new ISimpleTask.OnCompleteListener<String, Void, HashMap<String,IJrItem<?>>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, HashMap<String, IJrItem<?>>> owner, HashMap<String, IJrItem<?>> result) {
				mPlaylist = (JrPlaylist) result.get("Playlist");
				BuildPlaylistView();
			}
		});
        
	}
	
	private void BuildPlaylistView() {
//		try {
//			for (IJrItem<?> page : JrSession.getCategoriesList()) {
//				if (!page.getValue().equals("Playlist")) continue;
//				
//				mPlaylist = ((JrPlaylists)page).getMappedPlaylists().get(mPlaylistId);
//				break;
//			}
//		} catch (IOException e) {
//			PollConnectionTask.Instance.get().addOnCompleteListener(new ISimpleTask.OnCompleteListener<String, Void, Boolean>() {
//				
//				@Override
//				public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
//					BuildPlaylistView();
//				}
//			});
//			
//			WaitForConnectionDialog.show(this);
//		}
                
        if (mPlaylist.getSubItems().size() > 0) {
        	playlistView.setAdapter(new PlaylistAdapter(mPlaylist.getSubItems()));
        	playlistView.setOnItemClickListener(new ClickPlaylistListener(this, mPlaylist.getSubItems()));
        	playlistView.setOnItemLongClickListener(new BrowseItemMenu.ClickListener());
        } else {
        	playlistView.setVisibility(View.INVISIBLE);
        	pbLoading.setVisibility(View.VISIBLE);
        	JrFiles filesContainer = (JrFiles)mPlaylist.getJrFiles();
        	filesContainer.setOnFilesCompleteListener(new OnCompleteListener<List<JrFile>>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, List<JrFile>> owner, List<JrFile> result) {
					playlistView.setAdapter(new FileListAdapter(mContext, (ArrayList<JrFile>) result));
		        	playlistView.setOnItemClickListener(new ClickFileListener(mPlaylist.getJrFiles()));
		        	
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
		getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		menu.findItem(R.id.menu_view_now_playing).setVisible(ViewUtils.displayNowPlayingMenu());
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (ViewUtils.handleNavMenuClicks(this, item)) return true;
		return super.onOptionsItemSelected(item);
	}
}