package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list;

import android.content.Context;
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
import com.lasthopesoftware.bluewater.client.browsing.items.IItem;
import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.FileProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFileProvider;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.Lazy;

import java.util.List;
import java.util.concurrent.Callable;

public class FileListActivity extends AppCompatActivity implements IItemListViewContainer, ImmediateResponse<List<ServiceFile>, Void>, Runnable, Callable<PromisedResponse<List<ServiceFile>, Void>> {

	private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(FileListActivity.class);
	private static final String key = magicPropertyBuilder.buildProperty("key");
	private static final String value = magicPropertyBuilder.buildProperty("value");

	public static void startFileListActivity(Context context, IItem item) {
		final Intent fileListIntent = new Intent(context, FileListActivity.class);
		fileListIntent.putExtra(FileListActivity.key, item.getKey());
		fileListIntent.putExtra(FileListActivity.value, item.getValue());
		context.startActivity(fileListIntent);
	}

	private int itemId;

	private final LazyViewFinder<ProgressBar> pbLoading = new LazyViewFinder<>(this, R.id.pbLoadingItems);
	private final LazyViewFinder<ListView> fileListView = new LazyViewFinder<>(this, R.id.lvItems);
	private final Lazy<PromisedResponse<List<ServiceFile>, Void>> onFileProviderComplete = new Lazy<>(this);

    private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		final ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null)
			supportActionBar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_view_items);

        fileListView.findView().setVisibility(View.INVISIBLE);
        pbLoading.findView().setVisibility(View.VISIBLE);

        if (savedInstanceState != null) itemId = savedInstanceState.getInt(key);
        if (itemId == 0) itemId = this.getIntent().getIntExtra(key, 1);

        setTitle(getIntent().getStringExtra(value));

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.rlViewItems));

		run();
	}

	@Override
	public PromisedResponse<List<ServiceFile>, Void> call() {
		return LoopedInPromise.response(this, this);
	}

	@Override
	public void run() {
		final String[] parameters = FileListParameters.getInstance().getFileListParameters(new Item(itemId));

		SessionConnection.getInstance(this).promiseSessionConnection()
			.eventually(connection -> {
				final FileStringListProvider stringListProvider = new FileStringListProvider(connection);
				final FileProvider fileProvider = new FileProvider(stringListProvider);
				return fileProvider.promiseFiles(FileListParameters.Options.None, parameters);
			})
			.eventually(onFileProviderComplete.getObject())
			.excuse(new HandleViewIoException(this, this))
			.eventuallyExcuse(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(this), this))
			.then(new VoidResponse<>(v -> finish()));
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
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt(key, itemId);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		itemId = savedInstanceState.getInt(key);
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
