package com.lasthopesoftware.bluewater.servers.library.items.list;

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
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.shared.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 3/15/15.
 */
public class ItemListActivity extends AppCompatActivity implements IItemListViewContainer {

    public static final String KEY = "com.lasthopesoftware.bluewater.servers.library.items.list.key";
    public static final String VALUE = "com.lasthopesoftware.bluewater.servers.library.items.list.value";

    private final LazyViewFinder<ListView> itemListView = new LazyViewFinder<>(this, R.id.lvItems);
    private final LazyViewFinder<ProgressBar> pbLoading = new LazyViewFinder<>(this, R.id.pbLoadingItems);
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
        itemProvider.onComplete((owner, items) -> {
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
        final ItemListAdapter<Item> itemListAdapter = new ItemListAdapter<>(this, R.id.tvStandard, items, new ItemListMenuChangeHandler(this));

	    final ListView localItemListView = this.itemListView.findView();
        localItemListView.setAdapter(itemListAdapter);
        localItemListView.setOnItemClickListener(new ClickItemListener(this, items instanceof ArrayList ? (ArrayList<Item>) items : new ArrayList<>(items)));
        localItemListView.setOnItemLongClickListener(new LongClickViewAnimatorListener());
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
