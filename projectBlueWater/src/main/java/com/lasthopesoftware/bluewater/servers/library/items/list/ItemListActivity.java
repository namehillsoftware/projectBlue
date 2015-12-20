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
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 3/15/15.
 */
public class ItemListActivity extends AppCompatActivity implements IItemListViewContainer {

    public static final String KEY = "com.lasthopesoftware.bluewater.servers.library.items.list.key";
    public static final String VALUE = "com.lasthopesoftware.bluewater.servers.library.items.list.value";

    private ListView itemListView;
    private ProgressBar pbLoading;
    private ViewAnimator viewAnimator;
    private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

    private int mItemId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        itemListView = (ListView) findViewById(R.id.lvItems);
        pbLoading = (ProgressBar) findViewById(R.id.pbLoadingItems);

        mItemId = 0;
        if (savedInstanceState != null) mItemId = savedInstanceState.getInt(KEY);
        if (mItemId == 0) mItemId = getIntent().getIntExtra(KEY, 0);

        itemListView.setVisibility(View.INVISIBLE);
        pbLoading.setVisibility(View.VISIBLE);

        setTitle(getIntent().getStringExtra(VALUE));

        final ItemProvider itemProvider = new ItemProvider(SessionConnection.getSessionConnectionProvider(), mItemId);
        itemProvider.onComplete(new TwoParameterRunnable<FluentTask<String,Void,List<Item>>, List<Item>>() {
            @Override
            public void run(FluentTask<String, Void, List<Item>> owner, List<Item> items) {
                if (items == null) return;

                BuildItemListView(items);

                itemListView.setVisibility(View.VISIBLE);
                pbLoading.setVisibility(View.INVISIBLE);
            }
        });
        itemProvider.onError(new HandleViewIoException<String, Void, List<Item>>(this, new Runnable() {
            @Override
            public void run() {
                itemProvider.execute();
            }
        }));
        itemProvider.execute();

        nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.rlViewItems));
    }

    private void BuildItemListView(final List<Item> items) {
        final ItemListAdapter<Item> itemListAdapter = new ItemListAdapter<>(this, R.id.tvStandard, items, new ItemListMenuChangeHandler(this));
        itemListView.setAdapter(itemListAdapter);
        itemListView.setOnItemClickListener(new ClickItemListener(this, items instanceof ArrayList ? (ArrayList<Item>) items : new ArrayList<>(items)));
        itemListView.setOnItemLongClickListener(new LongClickViewAnimatorListener());
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
        if (ViewUtils.handleNavMenuClicks(this, item)) return true;
        return super.onOptionsItemSelected(item);
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
