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
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ClickItemListener implements OnItemClickListener {

	private static final Logger logger = LoggerFactory.getLogger(ClickItemListener.class);

	private final List<Item> items;
	private final View loadingView;

	public ClickItemListener(List<Item> items, View loadingView) {
		this.items = items;
		this.loadingView = loadingView;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final Item item = items.get(position);

        final Context context = view.getContext();

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
			}), new VoidResponse<>(e -> logger.error("An error occurred getting nested items for item " + item.getKey(), e)))
			.eventually(v -> new LoopedInPromise<>(() -> {
				parent.setVisibility(ViewUtils.getVisibility(true));
				loadingView.setVisibility(ViewUtils.getVisibility(false));
				return null;
			}, context, Duration.millis(100)));
	}
}
