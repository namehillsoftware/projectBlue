package com.lasthopesoftware.bluewater.client.library.items.playlists;

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
import com.lasthopesoftware.bluewater.client.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.client.library.items.list.ItemListAdapter;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.util.List;

public class PlaylistListActivity extends AppCompatActivity implements IItemListViewContainer {

	public static final String KEY = MagicPropertyBuilder.buildMagicPropertyName(PlaylistListActivity.class, "key");
	public static final String VALUE = MagicPropertyBuilder.buildMagicPropertyName(PlaylistListActivity.class, "value");
	private int mPlaylistId;

	private ProgressBar pbLoading;
	private ListView playlistView;
    private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        playlistView = (ListView)findViewById(R.id.lvItems);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingItems);
        
        mPlaylistId = 0;
        if (savedInstanceState != null) mPlaylistId = savedInstanceState.getInt(KEY);
        if (mPlaylistId == 0) mPlaylistId = getIntent().getIntExtra(KEY, 0);
        
        playlistView.setVisibility(View.INVISIBLE);
    	pbLoading.setVisibility(View.VISIBLE);

        setTitle(getIntent().getStringExtra(VALUE));

		final TwoParameterRunnable<FluentTask<String, Void, List<Playlist>>, List<Playlist>> onPlaylistProviderComplete = new TwoParameterRunnable<FluentTask<String,Void,List<Playlist>>, List<Playlist>>() {

			@Override
			public void run(FluentTask<String, Void, List<Playlist>> owner, List<Playlist> result) {
				if (result == null) return;

				BuildPlaylistView(result);

				playlistView.setVisibility(View.VISIBLE);
				pbLoading.setVisibility(View.INVISIBLE);
			}
		};

		new PlaylistsProvider(SessionConnection.getSessionConnectionProvider(), mPlaylistId)
		        .onComplete(onPlaylistProviderComplete)
		        .onError(new HandleViewIoException<>(PlaylistListActivity.this, new Runnable() {

			        @Override
			        public void run() {
				        new PlaylistsProvider(SessionConnection.getSessionConnectionProvider(), mPlaylistId)
						        .onComplete(onPlaylistProviderComplete)
						        .onError(new HandleViewIoException<>(PlaylistListActivity.this, this))
						        .execute();
			        }
		        }))
		        .execute();

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.rlViewItems));
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnectionActivity.restoreSessionConnection(this);
	}
	
	private void BuildPlaylistView(List<Playlist> playlist) {
		final ItemListAdapter<Playlist> itemListAdapter = new ItemListAdapter<>(this, R.id.tvStandard, playlist, new ItemListMenuChangeHandler(this));
        playlistView.setAdapter(itemListAdapter);
        playlistView.setOnItemClickListener(new ClickPlaylistListener(this, playlist));
        final LongClickViewAnimatorListener longClickViewAnimatorListener = new LongClickViewAnimatorListener();
        playlistView.setOnItemLongClickListener(longClickViewAnimatorListener);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt(KEY, mPlaylistId);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mPlaylistId = savedInstanceState.getInt(KEY);
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