package com.lasthopesoftware.bluewater.servers.library.items.media.files.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.Item;

/**
 * Created by david on 11/25/15.
 */
public class ItemFileProvider extends AbstractFileProvider {
	public ItemFileProvider(ConnectionProvider connectionProvider, Item item) {
		this(connectionProvider, item, -1);
	}

	public ItemFileProvider(ConnectionProvider connectionProvider, Item item, int option) {
		super(connectionProvider, option, "Browse/Files", "ID=" + String.valueOf(item.getKey()));
	}
}
