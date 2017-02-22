package com.lasthopesoftware.bluewater.client.library.items.list;

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
import com.lasthopesoftware.bluewater.client.library.access.ISelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;
import com.lasthopesoftware.bluewater.shared.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.lazyj.AbstractThreadLocalLazy;
import com.vedsoft.lazyj.ILazy;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 3/15/15.
 */
public class ItemListActivity extends AppCompatActivity implements IItemListViewContainer {

	private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(ItemListActivity.class);

    public static final String KEY = magicPropertyBuilder.buildProperty("key");
    public static final String VALUE = magicPropertyBuilder.buildProperty("value");

    private final LazyViewFinder<ListView> itemListView = new LazyViewFinder<>(this, R.id.lvItems);
    private final LazyViewFinder<ProgressBar> pbLoading = new LazyViewFinder<>(this, R.id.pbLoadingItems);
	private final ILazy<ISelectedBrowserLibraryProvider> lazySpecificLibraryProvider =
		new AbstractThreadLocalLazy<ISelectedBrowserLibraryProvider>() {
			@Override
			protected ISelectedBrowserLibraryProvider initialize() throws Exception {
				return new SelectedBrowserLibraryProvider(
					new SelectedBrowserLibraryIdentifierProvider(ItemListActivity.this),
					new LibraryRepository(ItemListActivity.this));
			}
		};

    private ViewAnimator viewAnimator;
    private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

    private int mItemId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mItemId = 0;
        if (savedInstanceState != null) mItemId = savedInstanceState.getInt(KEY);
        if (mItemId == 0) mItemId = getIntent().getIntExtra(KEY, 0);

	    final ListView localItemListView = itemListView.findView();
	    localItemListView.setVisibility(View.INVISIBLE);

	    final ProgressBar localLoadingProgressBar = pbLoading.findView();
	    localLoadingProgressBar.setVisibility(View.VISIBLE);

        setTitle(getIntent().getStringExtra(VALUE));

        final ItemProvider itemProvider = new ItemProvider(SessionConnection.getSessionConnectionProvider(), mItemId);
        itemProvider.onComplete((items) -> {
            if (items == null) return;

            BuildItemListView(items);

	        localItemListView.setVisibility(View.VISIBLE);
	        localLoadingProgressBar.setVisibility(View.INVISIBLE);
        });
        itemProvider.onError(new HandleViewIoException<>(this, itemProvider::execute));
        itemProvider.execute();

        nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.rlViewItems));
    }

    private void BuildItemListView(final List<Item> items) {
		lazySpecificLibraryProvider.getObject().getBrowserLibrary()
			.then(Dispatch.toContext(VoidFunc.runningCarelessly(library -> {
				final StoredItemAccess storedItemAccess = new StoredItemAccess(this, library);
				final ItemListAdapter<Item> itemListAdapter = new ItemListAdapter<>(this, R.id.tvStandard, items, new ItemListMenuChangeHandler(this), storedItemAccess, library);

				final ListView localItemListView = this.itemListView.findView();
				localItemListView.setAdapter(itemListAdapter);
				localItemListView.setOnItemClickListener(new ClickItemListener(this, items instanceof ArrayList ? (ArrayList<Item>) items : new ArrayList<>(items)));
				localItemListView.setOnItemLongClickListener(new LongClickViewAnimatorListener());
			}), this));
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
