package com.lasthopesoftware.bluewater.client.library.items.playlists;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ViewAnimator;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.access.ISelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.list.DemoableItemListAdapter;
import com.lasthopesoftware.bluewater.client.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.client.library.items.list.ItemListAdapter;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import java.util.List;

public class PlaylistListActivity extends AppCompatActivity implements IItemListViewContainer {

	public static final String KEY = MagicPropertyBuilder.buildMagicPropertyName(PlaylistListActivity.class, "key");
	public static final String VALUE = MagicPropertyBuilder.buildMagicPropertyName(PlaylistListActivity.class, "value");

	private final CreateAndHold<ISelectedBrowserLibraryProvider> lazySpecificLibraryProvider =
		new AbstractSynchronousLazy<ISelectedBrowserLibraryProvider>() {
			@Override
			protected ISelectedBrowserLibraryProvider create() {
				return new SelectedBrowserLibraryProvider(
					new SelectedBrowserLibraryIdentifierProvider(PlaylistListActivity.this),
					new LibraryRepository(PlaylistListActivity.this));
			}
		};

	private final LazyViewFinder<ProgressBar> pbLoading = new LazyViewFinder<>(this, R.id.pbLoadingItems);
	private final LazyViewFinder<ListView> playlistView = new LazyViewFinder<>(this, R.id.lvItems);
    private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;
	private int playlistId;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        playlistId = 0;
        if (savedInstanceState != null) playlistId = savedInstanceState.getInt(KEY);
        if (playlistId == 0) playlistId = getIntent().getIntExtra(KEY, 0);
        
        playlistView.findView().setVisibility(View.INVISIBLE);
    	pbLoading.findView().setVisibility(View.VISIBLE);

        setTitle(getIntent().getStringExtra(VALUE));

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.rlViewItems));

		final PromisedResponse<List<Playlist>, Void> onPlaylistProviderComplete = LoopedInPromise.response(result -> {
			if (result == null) return null;

			BuildPlaylistView(result);

			playlistView.findView().setVisibility(View.VISIBLE);
			pbLoading.findView().setVisibility(View.INVISIBLE);

			return null;
		}, this);

		PlaylistsProvider
			.promisePlaylists(SessionConnection.getSessionConnectionProvider(), playlistId)
			.eventually(onPlaylistProviderComplete)
			.excuse(new HandleViewIoException(PlaylistListActivity.this, new Runnable() {

				@Override
				public void run() {
					PlaylistsProvider
						.promisePlaylists(SessionConnection.getSessionConnectionProvider(), playlistId)
						.eventually(onPlaylistProviderComplete)
						.excuse(new HandleViewIoException(PlaylistListActivity.this, this));
				}
			}));
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnectionActivity.restoreSessionConnection(this);
	}
	
	private void BuildPlaylistView(List<Playlist> playlist) {
		lazySpecificLibraryProvider.getObject().getBrowserLibrary()
			.eventually(LoopedInPromise.response(library -> {
				final StoredItemAccess storedItemAccess = new StoredItemAccess(this, library);
				final ItemListAdapter<Playlist> itemListAdapter = new DemoableItemListAdapter<>(this, R.id.tvStandard, playlist, new ItemListMenuChangeHandler(this), storedItemAccess, library);

				final ListView localPlaylistView = playlistView.findView();
				localPlaylistView.setAdapter(itemListAdapter);
				localPlaylistView.setOnItemClickListener(new ClickPlaylistListener(this, playlist));
				final LongClickViewAnimatorListener longClickViewAnimatorListener = new LongClickViewAnimatorListener();
				localPlaylistView.setOnItemLongClickListener(longClickViewAnimatorListener);

				return null;
			}, this));
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt(KEY, playlistId);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		playlistId = savedInstanceState.getInt(KEY);
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