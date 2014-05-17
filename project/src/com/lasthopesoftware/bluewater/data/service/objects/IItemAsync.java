package com.lasthopesoftware.bluewater.data.service.objects;

import java.util.List;

import com.lasthopesoftware.bluewater.data.service.access.IDataTask;


public interface IItemAsync<T> {
	void getSubItemsAsync();

	/* Required Methods for Sub Item Async retrieval */
//	String[] getSubItemParams();
//	void setOnItemsCompleteListener(ISimpleTask.OnCompleteListener<List<T>> listener);
	void setOnItemsStartListener(IDataTask.OnStartListener<List<T>> listener);
	void setOnItemsErrorListener(IDataTask.OnErrorListener<List<T>> listener);
//	OnConnectListener<List<T>> getOnItemConnectListener();
//	List<ISimpleTask.OnCompleteListener<List<T>>> getOnItemsCompleteListeners();
//	List<ISimpleTask.OnStartListener> getOnItemsStartListeners();
//	List<ISimpleTask.OnErrorListener> getOnItemsErrorListeners();
}
