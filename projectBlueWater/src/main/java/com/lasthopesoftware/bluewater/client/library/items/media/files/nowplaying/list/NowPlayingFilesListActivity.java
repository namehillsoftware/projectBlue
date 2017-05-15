package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.list;

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
import com.lasthopesoftware.bluewater.client.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;
import com.lasthopesoftware.bluewater.shared.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.CarelessOneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.lazyj.AbstractSynchronousLazy;
import com.vedsoft.lazyj.ILazy;

public class NowPlayingFilesListActivity extends AppCompatActivity implements IItemListViewContainer {
	
	private final LazyViewFinder<ListView> fileListView = new LazyViewFinder<>(this, R.id.lvItems);
	private final LazyViewFinder<ProgressBar> mLoadingProgressBar = new LazyViewFinder<>(this, R.id.pbLoadingItems);
	private final ILazy<ThreeParameterAction<NowPlaying, IResolvedPromise<Void>, IRejectedPromise>> lazyDispatchedLibraryCompleteResolution =
		new AbstractSynchronousLazy<ThreeParameterAction<NowPlaying,IResolvedPromise<Void>,IRejectedPromise>>() {
			@Override
			protected ThreeParameterAction<NowPlaying, IResolvedPromise<Void>, IRejectedPromise> initialize() throws Exception {
				return
					Dispatch.toContext(
						VoidFunc.runCarelessly(
							new OnGetLibraryNowComplete(
								NowPlayingFilesListActivity.this,
								fileListView.findView(),
								mLoadingProgressBar.findView())),
						NowPlayingFilesListActivity.this);
			}
		};
	private final ILazy<INowPlayingRepository> lazyNowPlayingRepository =
		new AbstractSynchronousLazy<INowPlayingRepository>() {
			@Override
			protected INowPlayingRepository initialize() throws Exception {
				final LibraryRepository libraryRepository = new LibraryRepository(NowPlayingFilesListActivity.this);
				final SelectedBrowserLibraryIdentifierProvider selectedBrowserLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(NowPlayingFilesListActivity.this);
				final ISpecificLibraryProvider specificLibraryProvider = new SpecificLibraryProvider(selectedBrowserLibraryIdentifierProvider.getSelectedLibraryId(), libraryRepository);
				return new NowPlayingRepository(specificLibraryProvider, libraryRepository);
			}
		};

    private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_items);
        
        this.setTitle(R.string.title_view_now_playing_files);

		lazyNowPlayingRepository.getObject()
			.getNowPlaying()
			.then(lazyDispatchedLibraryCompleteResolution.getObject());
		
		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.rlViewItems));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return ViewUtils.buildStandardMenu(this, menu);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		if (!InstantiateSessionConnectionActivity.restoreSessionConnection(this)) return;
		
		fileListView.findView().setVisibility(View.INVISIBLE);
		mLoadingProgressBar.findView().setVisibility(View.VISIBLE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != InstantiateSessionConnectionActivity.ACTIVITY_ID) return;

		lazyNowPlayingRepository.getObject().getNowPlaying()
			.then(lazyDispatchedLibraryCompleteResolution.getObject());
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

	private static class OnGetLibraryNowComplete implements CarelessOneParameterAction<NowPlaying> {
		
		private final NowPlayingFilesListActivity nowPlayingFilesListActivity;
		private final ListView fileListView;
		private final ProgressBar loadingProgressBar;
		
		OnGetLibraryNowComplete(NowPlayingFilesListActivity nowPlayingFilesListActivity, ListView fileListView, ProgressBar loadingProgressBar) {
            this.nowPlayingFilesListActivity = nowPlayingFilesListActivity;
			this.fileListView = fileListView;
			this.loadingProgressBar = loadingProgressBar;
		}
		
		@Override
		public void runWith(final NowPlaying nowPlaying) {
			if (nowPlaying == null) return;


			final NowPlayingFileListAdapter nowPlayingFilesListAdapter = new NowPlayingFileListAdapter(nowPlayingFilesListActivity, R.id.tvStandard, new ItemListMenuChangeHandler(nowPlayingFilesListActivity), nowPlaying.playlist, nowPlaying.playlistPosition);
			fileListView.setAdapter(nowPlayingFilesListAdapter);

			final LongClickViewAnimatorListener longClickViewAnimatorListener = new LongClickViewAnimatorListener();
			fileListView.setOnItemLongClickListener(longClickViewAnimatorListener);

			if (nowPlaying.playlistPosition < nowPlaying.playlist.size())
				fileListView.setSelection(nowPlaying.playlistPosition);

			fileListView.setVisibility(View.VISIBLE);
			loadingProgressBar.setVisibility(View.INVISIBLE);
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
