package com.lasthopesoftware.bluewater.activities;

import java.util.ArrayList;

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
import com.lasthopesoftware.bluewater.activities.common.LongClickFlipListener;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.JrFiles;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class ViewNowPlayingFiles extends FragmentActivity {
	
	private ProgressBar pbLoading;
	private ListView fileListView;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_files);
        fileListView = (ListView)findViewById(R.id.lvFilelist);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingFileList);
        
        this.setTitle(R.string.title_view_now_playing_files);     
        
        final ViewNowPlayingFiles _this = this;
        final Library library = JrSession.GetLibrary(_this);
        
        final SimpleTask<Void, Void, ArrayList<JrFile>> getFileStringTask = new SimpleTask<Void, Void, ArrayList<JrFile>>();
        
        getFileStringTask.setOnExecuteListener(new OnExecuteListener<Void, Void, ArrayList<JrFile>>() {
			
			@Override
			public ArrayList<JrFile> onExecute(ISimpleTask<Void, Void, ArrayList<JrFile>> owner, Void... params) throws Exception {
				return JrFiles.deserializeFileStringList(library.getSavedTracksString());
			}
		});
        
        getFileStringTask.addOnCompleteListener(new OnCompleteListener<Void, Void, ArrayList<JrFile>>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, ArrayList<JrFile>> owner, ArrayList<JrFile> result) {
				final ArrayList<JrFile> _result = result;
				final FileListAdapter fileListAdapter = new FileListAdapter(_this, R.id.tvStandard, _result);
		        fileListView.setAdapter(fileListAdapter);
		        fileListView.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						StreamingMusicService.streamMusic(view.getContext(), position);
					}
				});
		        fileListView.setOnItemLongClickListener(new LongClickFlipListener());
		        
		        fileListView.setVisibility(View.VISIBLE);
		        pbLoading.setVisibility(View.INVISIBLE);
			}
		});
        
        getFileStringTask.execute();
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
