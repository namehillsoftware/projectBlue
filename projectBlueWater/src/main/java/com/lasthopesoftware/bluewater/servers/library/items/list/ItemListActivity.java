package com.lasthopesoftware.bluewater.servers.library.items.list;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewFlipListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.OnViewFlippedListener;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 3/15/15.
 */
public class ItemListActivity extends FragmentActivity implements OnViewFlippedListener {

    public static final String KEY = "com.lasthopesoftware.bluewater.servers.library.items.list.key";
    public static final String VALUE = "com.lasthopesoftware.bluewater.servers.library.items.list.value";

    private ListView itemListView;
    private ProgressBar pbLoading;

    private ViewFlipper mFlippedView;

    private int mItemId;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        itemListView = (ListView) findViewById(R.id.lvItems);
        pbLoading = (ProgressBar) findViewById(R.id.pbLoadingItems);

        mItemId = 0;
        if (savedInstanceState != null) mItemId = savedInstanceState.getInt(KEY);
        if (mItemId == 0) mItemId = getIntent().getIntExtra(KEY, 0);

        itemListView.setVisibility(View.INVISIBLE);
        pbLoading.setVisibility(View.VISIBLE);

        setTitle(getIntent().getStringExtra(VALUE));

        final ItemProvider itemProvider = new ItemProvider(SessionConnection.getSessionConnectionProvider(), mItemId);
        itemProvider.onComplete(new ISimpleTask.OnCompleteListener<Void, Void, List<Item>>() {
            @Override
            public void onComplete(ISimpleTask<Void, Void, List<Item>> owner, List<Item> items) {
                if (owner.getState() == SimpleTaskState.ERROR || items == null) return;

                BuildItemListView(items);

                itemListView.setVisibility(View.VISIBLE);
                pbLoading.setVisibility(View.INVISIBLE);
            }
        });
        itemProvider.onError(new HandleViewIoException(this, new PollConnection.OnConnectionRegainedListener() {
            @Override
            public void onConnectionRegained() {
                itemProvider.execute();
            }
        }));
        itemProvider.execute();
    }

    private void BuildItemListView(final List<Item> items) {
        itemListView.setAdapter(new ItemListAdapter(this, R.id.tvStandard, items));
        itemListView.setOnItemClickListener(new ClickItemListener(this, items instanceof ArrayList ? (ArrayList<Item>)items : new ArrayList<>(items)));
        final LongClickViewFlipListener longClickViewFlipListener = new LongClickViewFlipListener();
        longClickViewFlipListener.setOnViewFlipped(this);
        itemListView.setOnItemLongClickListener(longClickViewFlipListener);
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
        if (LongClickViewFlipListener.tryFlipToPreviousView(mFlippedView)) return;

        super.onBackPressed();
    }

    @Override
    public void onViewFlipped(ViewFlipper viewFlipper) {
        mFlippedView = viewFlipper;
    }
}
