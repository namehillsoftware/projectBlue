package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.list;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ViewAnimator;

import androidx.appcompat.app.AppCompatActivity;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.client.browsing.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.FileProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.parameters.SearchFileParameterProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.nowplaying.NowPlayingFileProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.browsing.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import java.util.List;

import static com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.forward;

public class SearchFilesActivity extends AppCompatActivity implements IItemListViewContainer, ImmediateResponse<List<ServiceFile>, Void> {

	private final LazyViewFinder<ProgressBar> pbLoading = new LazyViewFinder<>(this, R.id.pbLoadingItems);
	private final LazyViewFinder<ListView> fileListView = new LazyViewFinder<>(this, R.id.lvItems);
    private ViewAnimator viewAnimator;
    private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_items);

        fileListView.findView().setVisibility(View.INVISIBLE);
        pbLoading.findView().setVisibility(View.VISIBLE);

        nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.rlViewItems));
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

		fileListView.findView().setVisibility(View.VISIBLE);
		pbLoading.findView().setVisibility(View.INVISIBLE);

		final PromisedResponse<List<ServiceFile>, Void> onSearchFilesComplete = LoopedInPromise.response(this, this);

		final Runnable fillFileListAction = new Runnable() {
			@Override
			public void run() {
				SessionConnection.getInstance(SearchFilesActivity.this).promiseSessionConnection()
					.then(FileStringListProvider::new)
					.then(FileProvider::new)
					.eventually(p -> p.promiseFiles(FileListParameters.Options.None, SearchFileParameterProvider.getFileListParameters(query)))
					.eventually(onSearchFilesComplete)
					.excuse(new HandleViewIoException(SearchFilesActivity.this, this))
					.excuse(forward())
					.eventually(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(SearchFilesActivity.this), SearchFilesActivity.this))
					.then(new VoidResponse<>(v -> finish()));
			}
		};

		fillFileListAction.run();
	}

	@Override
	public Void respond(List<ServiceFile> serviceFiles) {
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
