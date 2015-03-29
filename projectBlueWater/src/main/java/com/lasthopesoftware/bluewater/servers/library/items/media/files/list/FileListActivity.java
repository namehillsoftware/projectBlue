package com.lasthopesoftware.bluewater.servers.library.items.media.files.list;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFilesContainer;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.listeners.ClickFileListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewFlipListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.OnViewFlippedListener;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.threading.IDataTask;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.List;

public class FileListActivity extends FragmentActivity {

	public static final String KEY = "com.lasthopesoftware.bluewater.servers.library.items.media.files.list.key";
	public static final String VALUE = "com.lasthopesoftware.bluewater.servers.library.items.media.files.list.value";
	public static final String VIEW_ITEM_FILES = "com.lasthopesoftware.bluewater.servers.library.items.media.files.list.view_item_files";
	public static final String VIEW_PLAYLIST_FILES = "com.lasthopesoftware.bluewater.servers.library.items.media.files.list.view_playlist_files";
	
	private int mItemId;
	private IItem mItem;
	
	private ProgressBar pbLoading;
	private ListView fileListView;

    private ViewFlipper mFlippedView;
	
	@SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_files);
        fileListView = (ListView)findViewById(R.id.lvFilelist);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingFileList);
        
        fileListView.setVisibility(View.INVISIBLE);
        pbLoading.setVisibility(View.VISIBLE);
        if (savedInstanceState != null) mItemId = savedInstanceState.getInt(KEY);
        if (mItemId == 0) mItemId = this.getIntent().getIntExtra(KEY, 1);
        mItem = getIntent().getAction().equals(VIEW_PLAYLIST_FILES) ? new Playlist(mItemId) : new Item(mItemId);
        
        setTitle(getIntent().getStringExtra(VALUE));
        final Files filesContainer = (Files)((IFilesContainer)mItem).getFiles();
        final FileListActivity _this = this;
        filesContainer.setOnFilesCompleteListener(new IDataTask.OnCompleteListener<List<IFile>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, List<IFile>> owner, List<IFile> result) {
				if (result == null) return;
				
		    	fileListView.setOnItemClickListener(new ClickFileListener(((IFilesContainer)mItem).getFiles()));
                final LongClickViewFlipListener longClickViewFlipListener = new LongClickViewFlipListener();
                longClickViewFlipListener.setOnViewFlipped(new OnViewFlippedListener() {
                    @Override
                    public void onViewFlipped(ViewFlipper viewFlipper) {
                        mFlippedView = viewFlipper;
                    }
                });
		    	fileListView.setOnItemLongClickListener(longClickViewFlipListener);
		    	fileListView.setAdapter(new FileListAdapter(_this, R.id.tvStandard, result));
		    	
		    	fileListView.setVisibility(View.VISIBLE);
		        pbLoading.setVisibility(View.INVISIBLE);
			}
		});
        
        filesContainer.setOnFilesErrorListener(new HandleViewIoException(_this, new OnConnectionRegainedListener() {
			
				@Override
				public void onConnectionRegained() {
					filesContainer.getFilesAsync();
				}
			})
        );
        
        filesContainer.getFilesAsync();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnectionActivity.restoreSessionConnection(this);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt(KEY, mItemId);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mItemId = savedInstanceState.getInt(KEY);
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

    @Override
    public void onBackPressed() {
        if (LongClickViewFlipListener.tryFlipToPreviousView(mFlippedView)) return;

        super.onBackPressed();
    }
}
