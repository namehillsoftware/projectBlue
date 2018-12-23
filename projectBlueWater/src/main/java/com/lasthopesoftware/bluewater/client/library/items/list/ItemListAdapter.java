package com.lasthopesoftware.bluewater.client.library.items.list;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.library.items.menu.ListItemMenuBuilder;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.ViewChangedHandler;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

import java.util.List;

public class ItemListAdapter<T extends IItem> extends ArrayAdapter<T> {

	private final ListItemMenuBuilder<T> listItemMenuBuilder;

	public ItemListAdapter(Activity activity, int resource, List<T> items, IFileListParameterProvider<T> fileListParameterProvider, IItemListMenuChangeHandler itemListMenuEvents, StoredItemAccess storedItemAccess, Library library) {
		super(activity, resource, items);

		final ViewChangedHandler viewChangedHandler = new ViewChangedHandler();
		viewChangedHandler.setOnAllMenusHidden(itemListMenuEvents);
		viewChangedHandler.setOnAnyMenuShown(itemListMenuEvents);
		viewChangedHandler.setOnViewChangedListener(itemListMenuEvents);

		listItemMenuBuilder = new ListItemMenuBuilder<>(storedItemAccess, library, fileListParameterProvider);
		listItemMenuBuilder.setOnViewChangedListener(viewChangedHandler);
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull final ViewGroup parent) {
		return listItemMenuBuilder.getView(position, getItem(position), convertView, parent);
	}
}
