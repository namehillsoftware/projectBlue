package com.lasthopesoftware.bluewater.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.adapters.FileListAdapter;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.data.service.objects.JrFiles;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;

public class ViewNowPlayingFiles extends FragmentActivity {

	private Context mContext;
	
	private ProgressBar pbLoading;
	private ListView fileListView;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_files);
        fileListView = (ListView)findViewById(R.id.lvFilelist);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingFileList);
        
        this.setTitle("Now Playing Playlist");
        
        FileListAdapter fileListAdapter = new FileListAdapter(mContext, StreamingMusicService.getPlaylist().size() > 0 ? StreamingMusicService.getPlaylist() : JrFiles.deserializeFileStringList(JrSession.GetLibrary(this).getSavedTracksString()));
        fileListView.setAdapter(fileListAdapter);
        fileListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				StreamingMusicService.SeekToFile(view.getContext(), StreamingMusicService.getPlaylist().get(position).getKey());
			}
		});
        fileListView.setVisibility(View.VISIBLE);
        pbLoading.setVisibility(View.INVISIBLE);
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		menu.findItem(R.id.menu_view_now_playing).setVisible(ViewUtils.displayNowPlayingMenu(this));
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (ViewUtils.handleNavMenuClicks(this, item)) return true;
		return super.onOptionsItemSelected(item);
	}

}
