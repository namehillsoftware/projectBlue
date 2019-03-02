package com.lasthopesoftware.bluewater.client.library.views.handlers;

import android.app.Activity;
import android.view.View;
import android.widget.ListView;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.list.ClickItemListener;
import com.lasthopesoftware.bluewater.client.library.items.list.DemoableItemListAdapter;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import java.util.List;

/**
 * Created by david on 11/5/15.
 */
public class OnGetLibraryViewItemResultsComplete implements ImmediateResponse<List<Item>, Void> {

	private final Activity activity;
	private final ListView listView;
	private final View loadingView;
	private final IItemListMenuChangeHandler itemListMenuChangeHandler;
	private final IFileListParameterProvider fileListParameterProvider;
	private final StoredItemAccess storedItemAccess;
	private final Library library;

    public OnGetLibraryViewItemResultsComplete(Activity activity, ListView listView, View loadingView, IItemListMenuChangeHandler itemListMenuChangeHandler, IFileListParameterProvider fileListParameterProvider, StoredItemAccess storedItemAccess, Library library) {
		this.activity = activity;
		this.listView = listView;
        this.loadingView = loadingView;
		this.itemListMenuChangeHandler = itemListMenuChangeHandler;
		this.fileListParameterProvider = fileListParameterProvider;
		this.storedItemAccess = storedItemAccess;
		this.library = library;
	}

    @Override
    public Void respond(List<Item> result) {
        if (result == null) return null;

        listView.setOnItemLongClickListener(new LongClickViewAnimatorListener());
        listView.setAdapter(new DemoableItemListAdapter<>(activity, R.id.tvStandard, result, fileListParameterProvider, itemListMenuChangeHandler, storedItemAccess, library));
        loadingView.setVisibility(View.INVISIBLE);
        listView.setVisibility(View.VISIBLE);
        listView.setOnItemClickListener(new ClickItemListener(result, loadingView));

        return null;
    }
}
