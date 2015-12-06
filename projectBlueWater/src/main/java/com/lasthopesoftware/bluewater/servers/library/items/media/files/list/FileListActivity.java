package com.lasthopesoftware.bluewater.servers.library.items.media.files.list;

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
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.access.FileProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;
import com.lasthopesoftware.threading.FluentTask;

import java.util.List;

public class FileListActivity extends AppCompatActivity implements IItemListViewContainer {

	public static final String KEY = "com.lasthopesoftware.bluewater.servers.library.items.media.files.list.key";
	public static final String VALUE = "com.lasthopesoftware.bluewater.servers.library.items.media.files.list.value";
	public static final String VIEW_ITEM_FILES = "com.lasthopesoftware.bluewater.servers.library.items.media.files.list.view_item_files";
	public static final String VIEW_PLAYLIST_FILES = "com.lasthopesoftware.bluewater.servers.library.items.media.files.list.view_playlist_files";
	
	private int mItemId;

	private ProgressBar pbLoading;
	private ListView fileListView;

    private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_files);

		fileListView = (ListView)findViewById(R.id.lvFilelist);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingFileList);
        
        fileListView.setVisibility(View.INVISIBLE);
        pbLoading.setVisibility(View.VISIBLE);
        if (savedInstanceState != null) mItemId = savedInstanceState.getInt(KEY);
        if (mItemId == 0) mItemId = this.getIntent().getIntExtra(KEY, 1);

        setTitle(getIntent().getStringExtra(VALUE));

		final ITwoParameterRunnable<FluentTask<Void, Void, List<IFile>>, List<IFile>> onFileProviderComplete = new ITwoParameterRunnable<FluentTask<Void,Void,List<IFile>>, List<IFile>>() {

			@Override
			public void run(FluentTask<Void, Void, List<IFile>> owner, List<IFile> result) {
				if (result == null) return;

				final LongClickViewAnimatorListener longClickViewAnimatorListener = new LongClickViewAnimatorListener();

				fileListView.setOnItemLongClickListener(longClickViewAnimatorListener);
				final FileListAdapter fileListAdapter = new FileListAdapter(FileListActivity.this, R.id.tvStandard, result, new ItemListMenuChangeHandler(FileListActivity.this));

				fileListView.setAdapter(fileListAdapter);

				fileListView.setVisibility(View.VISIBLE);
				pbLoading.setVisibility(View.INVISIBLE);
			}
		};

		getNewFileProvider()
			.onComplete(onFileProviderComplete)
			.onError(new HandleViewIoException<Void, Void, List<IFile>>(this, new Runnable() {

						@Override
						public void run() {
							getNewFileProvider()
									.onComplete(onFileProviderComplete)
									.onError(new HandleViewIoException<Void, Void, List<IFile>>(FileListActivity.this, this))
									.execute();
						}
					})
			)
			.execute();

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.rlViewFiles));
	}

	private FileProvider getNewFileProvider() {
		return new FileProvider(SessionConnection.getSessionConnectionProvider(), getIntent().getAction().equals(VIEW_PLAYLIST_FILES) ? new Playlist(mItemId) : new Item(mItemId));
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
        if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return;

        super.onBackPressed();
    }

	@Override
	public void updateViewAnimator(ViewAnimator viewAnimator) {
		this.viewAnimator = viewAnimator;
	}

	@Override
	public NowPlayingFloatingActionButton getNowPlayingFloatingActionButton() {
		return nowPlayingFloatingActionButton;
	}
}
