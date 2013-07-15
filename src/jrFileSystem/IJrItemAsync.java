package jrFileSystem;


public interface IJrItemAsync {
	void getSubItemsAsync();

	/* Required Methods for Sub Item Async retrieval */
//	String[] getSubItemParams();
//	void setOnItemsCompleteListener(IJrDataTask.OnCompleteListener<List<T>> listener);
	void setOnItemsStartListener(IJrDataTask.OnStartListener listener);
	void setOnItemsErrorListener(IJrDataTask.OnErrorListener listener);
//	OnConnectListener<List<T>> getOnItemConnectListener();
//	List<IJrDataTask.OnCompleteListener<List<T>>> getOnItemsCompleteListeners();
//	List<IJrDataTask.OnStartListener> getOnItemsStartListeners();
//	List<IJrDataTask.OnErrorListener> getOnItemsErrorListeners();
}
