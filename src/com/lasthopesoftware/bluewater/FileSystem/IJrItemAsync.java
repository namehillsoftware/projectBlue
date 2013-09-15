package com.lasthopesoftware.bluewater.FileSystem;

import java.util.List;

import com.lasthopesoftware.bluewater.access.IJrDataTask;


public interface IJrItemAsync<T> {
	void getSubItemsAsync();

	/* Required Methods for Sub Item Async retrieval */
//	String[] getSubItemParams();
//	void setOnItemsCompleteListener(ISimpleTask.OnCompleteListener<List<T>> listener);
	void setOnItemsStartListener(IJrDataTask.OnStartListener<List<T>> listener);
	void setOnItemsErrorListener(IJrDataTask.OnErrorListener<List<T>> listener);
//	OnConnectListener<List<T>> getOnItemConnectListener();
//	List<ISimpleTask.OnCompleteListener<List<T>>> getOnItemsCompleteListeners();
//	List<ISimpleTask.OnStartListener> getOnItemsStartListeners();
//	List<ISimpleTask.OnErrorListener> getOnItemsErrorListeners();
}
