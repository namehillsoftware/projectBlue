package com.lasthopesoftware.bluewater.servers.library.items;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFilesContainer;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IItemFiles;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;



public class Item extends AbstractIntKeyStringValue implements IItem, IFilesContainer {
	private Files mFiles;
	private final ConnectionProvider connectionProvider;

	public Item(ConnectionProvider connectionProvider, int key, String value) {
		super(key, value);

		this.connectionProvider = connectionProvider;
	}
	
	public Item(ConnectionProvider connectionProvider, int key) {
		super();

		this.connectionProvider = connectionProvider;
		this.setKey(key);
	}
	
	public Item(ConnectionProvider connectionProvider) {
		super();

		this.connectionProvider = connectionProvider;
	}
		
	@Override
	public IItemFiles getFiles() {
		if (mFiles == null) mFiles = new Files(connectionProvider, "Browse/Files", "ID=" + String.valueOf(this.getKey()));
		return mFiles;
	}

	@Override
	public String[] getSubItemParams() {
		return new String[] { "Browse/Children", "ID=" + String.valueOf(this.getKey())};
	}

    @Override
    public int hashCode() {
        return getKey();
    }
}
