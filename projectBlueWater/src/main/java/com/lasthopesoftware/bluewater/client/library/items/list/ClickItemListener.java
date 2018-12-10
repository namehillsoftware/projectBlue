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
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import java.util.List;

public class ClickItemListener implements OnItemClickListener {

	private final List<Item> items;
	private final Context context;

	public ClickItemListener(Context context, List<Item> items) {
		this.context = context;
        this.items = items;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Item item = items.get(position);

		SessionConnection.getInstance(context).promiseSessionConnection()
			.eventually(c -> ItemProvider.provide(c, item.getKey()))
            .then(new VoidResponse<>(items -> {
				if (items == null) return;

				if (items.size() > 0) {
					final Intent itemListIntent = new Intent(context, ItemListActivity.class);
					itemListIntent.putExtra(ItemListActivity.KEY, item.getKey());
					itemListIntent.putExtra(ItemListActivity.VALUE, item.getValue());
					context.startActivity(itemListIntent);

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
