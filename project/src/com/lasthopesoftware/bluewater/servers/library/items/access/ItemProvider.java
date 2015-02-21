package com.lasthopesoftware.bluewater.servers.library.items.access;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.access.FilesystemResponse;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class ItemProvider extends AbstractCollectionProvider<Item> { 
	
	public static ItemProvider provide(String... params) {
		return new ItemProvider(params);
	}
	
	public ItemProvider(String... params) {
		super(null, params);
	}
	
	public ItemProvider(HttpURLConnection connection, String... params) {
		super(connection, params);
	}
			
	protected SimpleTask<Void, Void, List<Item>> getNewTask() {

		final SimpleTask<Void, Void, List<Item>> getItemsTask = new SimpleTask<Void, Void, List<Item>>(new OnExecuteListener<Void, Void, List<Item>>() {
			
			@Override
			public List<Item> onExecute(ISimpleTask<Void, Void, List<Item>> owner, Void... voidParams) throws Exception {
				final HttpURLConnection conn = mConnection == null ? ConnectionProvider.getConnection(mParams) : mConnection;
				try {
					final InputStream is = conn.getInputStream();
					try {
						return FilesystemResponse.GetItems(is);
					} finally {
						is.close();
					}

				} finally {
					if (mConnection == null) conn.disconnect();
				}
			}
		});
		
		if (mOnGetItemsComplete != null)
			getItemsTask.addOnCompleteListener(mOnGetItemsComplete);
		
		if (mOnGetItemsError != null)
			getItemsTask.addOnErrorListener(mOnGetItemsError);
		
		return getItemsTask;
	}
}
