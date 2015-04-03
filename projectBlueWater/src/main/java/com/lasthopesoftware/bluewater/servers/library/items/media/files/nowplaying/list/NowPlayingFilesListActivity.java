package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.list;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewFlipListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.OnViewFlippedListener;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import java.util.ArrayList;

public class NowPlayingFilesListActivity extends FragmentActivity {
	
	private ListView mFileListView;
	private ProgressBar mLoadingProgressBar;

    private ViewFlipper mFlippedView;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_files);
        mFileListView = (ListView)findViewById(R.id.lvFilelist);
        mLoadingProgressBar = (ProgressBar)findViewById(R.id.pbLoadingFileList);
        
        this.setTitle(R.string.title_view_now_playing_files);     
        
        LibrarySession.GetLibrary(this, new OnGetLibraryNowComplete(this, mFileListView, mLoadingProgressBar));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return ViewUtils.buildStandardMenu(this, menu);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		if (!InstantiateSessionConnectionActivity.restoreSessionConnection(this)) return;
		
		mFileListView.setVisibility(View.INVISIBLE);
		mLoadingProgressBar.setVisibility(View.VISIBLE);		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != InstantiateSessionConnectionActivity.ACTIVITY_ID) return;
		
		LibrarySession.GetLibrary(this, new OnGetLibraryNowComplete(this, mFileListView, mLoadingProgressBar));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (ViewUtils.handleNavMenuClicks(this, item)) return true;
		return super.onOptionsItemSelected(item);
	}
	
	private static class OnGetLibraryNowComplete implements OnCompleteListener<Integer, Void, Library> {
		
		private final NowPlayingFilesListActivity mNowPlayingFilesListActivity;
		private final ListView mFileListView;
		private final ProgressBar mLoadingProgressBar;
		
		public OnGetLibraryNowComplete(NowPlayingFilesListActivity nowPlayingFilesListActivity, ListView fileListView, ProgressBar loadingProgressBar) {
            mNowPlayingFilesListActivity = nowPlayingFilesListActivity;
			mFileListView = fileListView;
			mLoadingProgressBar = loadingProgressBar;
		}
		
		@Override
		public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library library) {
			if (library == null) return;

	        final SimpleTask<Void, Void, ArrayList<IFile>> getFileStringTask = new SimpleTask<Void, Void, ArrayList<IFile>>(new OnExecuteListener<Void, Void, ArrayList<IFile>>() {
				
				@Override
				public ArrayList<IFile> onExecute(ISimpleTask<Void, Void, ArrayList<IFile>> owner, Void... params) throws Exception {
					return Files.parseFileStringList(library.getSavedTracksString());
				}
			});
	        
	        getFileStringTask.addOnCompleteListener(new OnCompleteListener<Void, Void, ArrayList<IFile>>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, ArrayList<IFile>> owner, final ArrayList<IFile> result) {
			        mFileListView.setAdapter(new NowPlayingFileListAdapter(mNowPlayingFilesListActivity, R.id.tvStandard, result, library.getNowPlayingId()));

                    final LongClickViewFlipListener longClickViewFlipListener = new LongClickViewFlipListener();
                    longClickViewFlipListener.setOnViewFlipped(new OnViewFlippedListener() {
                        @Override
                        public void onViewFlipped(ViewFlipper viewFlipper) {
                            mNowPlayingFilesListActivity.setFlippedView(viewFlipper);
                        }
                    });
                    mFileListView.setOnItemLongClickListener(longClickViewFlipListener);
			        
			        if (library.getNowPlayingId() < result.size())
			        	mFileListView.setSelection(library.getNowPlayingId());
			        
			        mFileListView.setVisibility(View.VISIBLE);
			        mLoadingProgressBar.setVisibility(View.INVISIBLE);
				}
			});
	        
	        getFileStringTask.execute();
		}
	}

    public void setFlippedView(ViewFlipper flippedView) {
        mFlippedView = flippedView;
    }

    @Override
    public void onBackPressed() {
        if (LongClickViewFlipListener.tryFlipToPreviousView(mFlippedView)) return;

        super.onBackPressed();
    }
}
