package com.lasthopesoftware.bluewater.client.library.items.media.files.list;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
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
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.SearchFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.List;

public class SearchFilesActivity extends AppCompatActivity implements IItemListViewContainer {

	private ProgressBar pbLoading;
	private ListView fileListView;
    private ViewAnimator viewAnimator;
    private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_items);
        fileListView = (ListView)findViewById(R.id.lvItems);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingItems);
        
        fileListView.setVisibility(View.INVISIBLE);
        pbLoading.setVisibility(View.VISIBLE);

        nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.rlViewItems));
        handleIntent(getIntent());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return ViewUtils.buildStandardMenu(this, menu);
	}
	
	@Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

	private void handleIntent(Intent intent) {
		if (!Intent.ACTION_SEARCH.equals(intent.getAction())) return;
        
		final String query = intent.getStringExtra(SearchManager.QUERY);
        if (query == null || query.isEmpty()) return;

        setTitle(String.format(getString(R.string.title_activity_search_results), query));

		fileListView.setVisibility(View.VISIBLE);
		pbLoading.setVisibility(View.INVISIBLE);

        final OneParameterAction<List<IFile>> onSearchFilesComplete = result -> {
			if (result == null) return;

			final FileListAdapter fileListAdapter =
				new FileListAdapter(
					this,
					R.id.tvStandard,
					result,
					new ItemListMenuChangeHandler(this),
					NowPlayingFileProvider.fromActiveLibrary(this));

			fileListView.setOnItemLongClickListener(new LongClickViewAnimatorListener());
			fileListView.setAdapter(fileListAdapter);
		};

        SearchFileProvider.get(SessionConnection.getSessionConnectionProvider(), query)
            .onComplete(onSearchFilesComplete)
            .onError(new HandleViewIoException<>(this, new Runnable() {

						@Override
						public void run() {
							SearchFileProvider.get(SessionConnection.getSessionConnectionProvider(), query)
									.onComplete(onSearchFilesComplete)
									.onError(new HandleViewIoException<>(SearchFilesActivity.this, this));
						}
					})
            ).execute();
	}

	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnectionActivity.restoreSessionConnection(this);
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
