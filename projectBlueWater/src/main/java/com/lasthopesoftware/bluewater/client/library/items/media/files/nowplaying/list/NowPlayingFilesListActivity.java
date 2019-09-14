package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.list;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ViewAnimator;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.library.access.ChosenLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.ResponseAction;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

public class NowPlayingFilesListActivity extends AppCompatActivity implements IItemListViewContainer {
	
	private final LazyViewFinder<ListView> fileListView = new LazyViewFinder<>(this, R.id.lvItems);
	private final LazyViewFinder<ProgressBar> mLoadingProgressBar = new LazyViewFinder<>(this, R.id.pbLoadingItems);

	private final CreateAndHold<INowPlayingRepository> lazyNowPlayingRepository =
		new AbstractSynchronousLazy<INowPlayingRepository>() {
			@Override
			protected INowPlayingRepository create() {
				final LibraryRepository libraryRepository = new LibraryRepository(NowPlayingFilesListActivity.this);
				final SelectedBrowserLibraryIdentifierProvider selectedBrowserLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(NowPlayingFilesListActivity.this);
				final ISpecificLibraryProvider specificLibraryProvider = new ChosenLibraryProvider(selectedBrowserLibraryIdentifierProvider, libraryRepository);
				return new NowPlayingRepository(specificLibraryProvider, libraryRepository);
			}
		};

	private final CreateAndHold<PromisedResponse<NowPlaying, Void>> lazyDispatchedLibraryCompleteResolution =
		new AbstractSynchronousLazy<PromisedResponse<NowPlaying, Void>>() {
			@Override
			protected PromisedResponse<NowPlaying, Void> create() {
				return
					LoopedInPromise.response(
						new VoidResponse<>(
							new OnGetLibraryNowComplete(
								NowPlayingFilesListActivity.this,
								fileListView.findView(),
								mLoadingProgressBar.findView(),
								lazyNowPlayingRepository.getObject())),
						NowPlayingFilesListActivity.this);
			}
		};

    private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_items);
        
        this.setTitle(R.string.title_view_now_playing_files);

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.rlViewItems));

		lazyNowPlayingRepository.getObject()
			.getNowPlaying()
			.eventually(lazyDispatchedLibraryCompleteResolution.getObject());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return ViewUtils.buildStandardMenu(this, menu);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnectionActivity.restoreSessionConnection(this)
			.eventually(LoopedInPromise.response(new VoidResponse<>(restore -> {
				if (!restore) return;

				fileListView.findView().setVisibility(View.INVISIBLE);
				mLoadingProgressBar.findView().setVisibility(View.VISIBLE);
			}), this));
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) {
			lazyNowPlayingRepository.getObject()
				.getNowPlaying()
				.eventually(lazyDispatchedLibraryCompleteResolution.getObject());
		}

		super.onActivityResult(requestCode, resultCode, data);
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

	private static class OnGetLibraryNowComplete implements ResponseAction<NowPlaying> {
		
		private final NowPlayingFilesListActivity nowPlayingFilesListActivity;
		private final ListView fileListView;
		private final ProgressBar loadingProgressBar;
		private final INowPlayingRepository nowPlayingRepository;

		OnGetLibraryNowComplete(NowPlayingFilesListActivity nowPlayingFilesListActivity, ListView fileListView, ProgressBar loadingProgressBar, INowPlayingRepository nowPlayingRepository) {
            this.nowPlayingFilesListActivity = nowPlayingFilesListActivity;
			this.fileListView = fileListView;
			this.loadingProgressBar = loadingProgressBar;
			this.nowPlayingRepository = nowPlayingRepository;
		}
		
		@Override
		public void perform(final NowPlaying nowPlaying) {
			if (nowPlaying == null) return;


			final NowPlayingFileListAdapter nowPlayingFilesListAdapter = new NowPlayingFileListAdapter(nowPlayingFilesListActivity, R.id.tvStandard, new ItemListMenuChangeHandler(nowPlayingFilesListActivity), nowPlaying.playlist, nowPlayingRepository);
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
