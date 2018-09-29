package com.lasthopesoftware.bluewater.client.library.items.list;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.list.FileListActivity;

import java.util.ArrayList;

import static com.namehillsoftware.handoff.promises.response.ImmediateAction.perform;

public class ClickItemListener implements OnItemClickListener {

	private final ArrayList<Item> items;
	private final Context context;

	public ClickItemListener(Context context, ArrayList<Item> items) {
		this.context = context;
        this.items = items;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Item item = items.get(position);

		SessionConnection.getInstance(context).promiseSessionConnection()
			.eventually(c -> ItemProvider.provide(c, item.getKey()))
            .then(perform(items -> {
				if (items == null) return;

				if (items.size() > 0) {
					final Intent itemlistIntent = new Intent(context, ItemListActivity.class);
					itemlistIntent.putExtra(ItemListActivity.KEY, item.getKey());
					itemlistIntent.putExtra(ItemListActivity.VALUE, item.getValue());
					context.startActivity(itemlistIntent);

					return;
				}

				final Intent fileListIntent = new Intent(context, FileListActivity.class);
				fileListIntent.putExtra(FileListActivity.KEY, item.getKey());
				fileListIntent.putExtra(FileListActivity.VALUE, item.getValue());
				fileListIntent.setAction(FileListActivity.VIEW_ITEM_FILES);
				context.startActivity(fileListIntent);
			}));
	}

}
