package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.list;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ViewAnimator;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;
import com.lasthopesoftware.threading.FluentTask;
import com.lasthopesoftware.threading.IFluentTask;
import com.lasthopesoftware.threading.OnExecuteListener;

import java.util.ArrayList;

public class NowPlayingFilesListActivity extends AppCompatActivity implements IItemListViewContainer {
	
	private ListView mFileListView;
	private ProgressBar mLoadingProgressBar;

    private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_files);
        mFileListView = (ListView)findViewById(R.id.lvFilelist);
        mLoadingProgressBar = (ProgressBar)findViewById(R.id.pbLoadingFileList);
        
        this.setTitle(R.string.title_view_now_playing_files);
		
		LibrarySession.GetActiveLibrary(this, new OnGetLibraryNowComplete(this, mFileListView, mLoadingProgressBar));
		
		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.rlViewFiles));
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
		
		LibrarySession.GetActiveLibrary(this, new OnGetLibraryNowComplete(this, mFileListView, mLoadingProgressBar));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        return ViewUtils.handleNavMenuClicks(this, item) || super.onOptionsItemSelected(item);
    }

	@Override
	public void updateViewAnimator(ViewAnimator viewAnimator) {
		this.viewAnimator = viewAnimator;
	}

	@Override
	public NowPlayingFloatingActionButton getNowPlayingFloatingActionButton() {
		return nowPlayingFloatingActionButton;
	}

	private static class OnGetLibraryNowComplete implements ITwoParameterRunnable<IFluentTask<Integer, Void, Library>, Library> {
		
		private final NowPlayingFilesListActivity mNowPlayingFilesListActivity;
		private final ListView mFileListView;
		private final ProgressBar mLoadingProgressBar;
		
		public OnGetLibraryNowComplete(NowPlayingFilesListActivity nowPlayingFilesListActivity, ListView fileListView, ProgressBar loadingProgressBar) {
            mNowPlayingFilesListActivity = nowPlayingFilesListActivity;
			mFileListView = fileListView;
			mLoadingProgressBar = loadingProgressBar;
		}
		
		@Override
		public void run(IFluentTask<Integer, Void, Library> owner, final Library library) {
			if (library == null) return;

	        final FluentTask<Void, Void, ArrayList<IFile>> getFileStringTask = new FluentTask<>(new OnExecuteListener<Void, Void, ArrayList<IFile>>() {
				
				@Override
				public ArrayList<IFile> onExecute(IFluentTask<Void, Void, ArrayList<IFile>> owner, Void... params) throws Exception {
					return FileStringListUtilities.parseFileStringList(SessionConnection.getSessionConnectionProvider(), library.getSavedTracksString());
				}
			});
	        
	        getFileStringTask.onComplete(new ITwoParameterRunnable<IFluentTask<Void, Void, ArrayList<IFile>>, ArrayList<IFile>>() {

		        @Override
		        public void run(IFluentTask<Void, Void, ArrayList<IFile>> owner, final ArrayList<IFile> result) {
			        final NowPlayingFileListAdapter nowPlayingFilesListAdapter = new NowPlayingFileListAdapter(mNowPlayingFilesListActivity, R.id.tvStandard, new ItemListMenuChangeHandler(mNowPlayingFilesListActivity), result, library.getNowPlayingId());
			        mFileListView.setAdapter(nowPlayingFilesListAdapter);

			        final LongClickViewAnimatorListener longClickViewAnimatorListener = new LongClickViewAnimatorListener();
			        mFileListView.setOnItemLongClickListener(longClickViewAnimatorListener);

			        if (library.getNowPlayingId() < result.size())
				        mFileListView.setSelection(library.getNowPlayingId());

			        mFileListView.setVisibility(View.VISIBLE);
			        mLoadingProgressBar.setVisibility(View.INVISIBLE);
		        }
	        });
	        
	        getFileStringTask.execute();
		}
	}

    public void setViewAnimator(ViewAnimator viewAnimator) {
        this.viewAnimator = viewAnimator;
    }

    @Override
    public void onBackPressed() {
        if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return;

        super.onBackPressed();
    }
}
