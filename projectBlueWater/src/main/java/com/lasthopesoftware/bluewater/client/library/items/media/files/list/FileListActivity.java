package com.lasthopesoftware.bluewater.client.library.items.media.files.list;

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
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.FileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;
import com.lasthopesoftware.messenger.promises.response.PromisedResponse;

import java.util.List;

public class FileListActivity extends AppCompatActivity implements IItemListViewContainer, ImmediateResponse<List<ServiceFile>, Void> {

	public static final String KEY = "com.lasthopesoftware.bluewater.servers.library.items.media.files.list.key";
	public static final String VALUE = "com.lasthopesoftware.bluewater.servers.library.items.media.files.list.value";
	public static final String VIEW_ITEM_FILES = "com.lasthopesoftware.bluewater.servers.library.items.media.files.list.view_item_files";
	public static final String VIEW_PLAYLIST_FILES = "com.lasthopesoftware.bluewater.servers.library.items.media.files.list.view_playlist_files";
	
	private int mItemId;

	private LazyViewFinder<ProgressBar> pbLoading = new LazyViewFinder<>(this, R.id.pbLoadingItems);
	private LazyViewFinder<ListView> fileListView = new LazyViewFinder<>(this, R.id.lvItems);

    private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_items);

        fileListView.findView().setVisibility(View.INVISIBLE);
        pbLoading.findView().setVisibility(View.VISIBLE);
        if (savedInstanceState != null) mItemId = savedInstanceState.getInt(KEY);
        if (mItemId == 0) mItemId = this.getIntent().getIntExtra(KEY, 1);

        setTitle(getIntent().getStringExtra(VALUE));

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.rlViewItems));

		final PromisedResponse<List<ServiceFile>, Void> onFileProviderComplete = LoopedInPromise.response(this, this);

		final String[] parameters = (getIntent().getAction().equals(VIEW_PLAYLIST_FILES) ? new Playlist(mItemId) : new Item(mItemId)).getFileListParameters();

		getNewFileProvider()
			.promiseFiles(FileListParameters.Options.None, parameters)
			.eventually(onFileProviderComplete)
			.excuse(new HandleViewIoException(this, new Runnable() {

					@Override
					public void run() {
						getNewFileProvider()
							.promiseFiles(FileListParameters.Options.None, parameters)
							.eventually(onFileProviderComplete)
							.excuse(new HandleViewIoException(FileListActivity.this, this));
					}
				}));
	}

	@Override
	public Void respond(List<ServiceFile> serviceFiles) throws Throwable {
		if (serviceFiles == null) return null;

		final LongClickViewAnimatorListener longClickViewAnimatorListener = new LongClickViewAnimatorListener();

		fileListView.findView().setOnItemLongClickListener(longClickViewAnimatorListener);
		final FileListAdapter fileListAdapter =
			new FileListAdapter(
				this,
				R.id.tvStandard,
				serviceFiles,
				new ItemListMenuChangeHandler(this),
				NowPlayingFileProvider.fromActiveLibrary(this));

		fileListView.findView().setAdapter(fileListAdapter);

		fileListView.findView().setVisibility(View.VISIBLE);
		pbLoading.findView().setVisibility(View.INVISIBLE);

		return null;
	}

	private FileProvider getNewFileProvider() {
		return new FileProvider(new FileStringListProvider(SessionConnection.getSessionConnectionProvider()));
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
		return ViewUtils.handleNavMenuClicks(this, item) || super.onOptionsItemSelected(item);
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
