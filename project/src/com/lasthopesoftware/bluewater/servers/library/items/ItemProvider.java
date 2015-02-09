package com.lasthopesoftware.bluewater.servers.library.items;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import com.lasthopesoftware.bluewater.data.service.access.FilesystemResponse;
import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.service.objects.IItem;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class ItemProvider extends AbstractIItemProvider { 

	public ItemProvider(String... params) {
		super(null, params);
	}
	
	public ItemProvider(HttpURLConnection connection, String... params) {
		super(connection, params);
	}
			
	protected SimpleTask<Void, Void, List<IItem>> getNewTask() {

		final SimpleTask<Void, Void, List<IItem>> getItemsTask = new SimpleTask<Void, Void, List<IItem>>(new OnExecuteListener<Void, Void, List<IItem>>() {
			
			@Override
			public List<IItem> onExecute(ISimpleTask<Void, Void, List<IItem>> owner, Void... voidParams) throws Exception {
				final HttpURLConnection conn = mConnection == null ? ConnectionManager.getConnection(mParams) : mConnection;
				try {
					final InputStream is = conn.getInputStream();
					try {
						return new ArrayList<IItem>(FilesystemResponse.GetItems(is));
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
