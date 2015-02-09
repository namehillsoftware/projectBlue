package com.lasthopesoftware.bluewater.data.service.objects;


public interface IItem extends Comparable<IItem> {
//	void addOnItemsCompleteListener(OnCompleteListener<List<T>> listener);
//	void removeOnItemsCompleteListener(OnCompleteListener<List<T>> listener);
//	void getSubItemsAsync();
//	ArrayList<T> getSubItems() throws IOException;
	String[] getSubItemParams();
	int getKey();
	void setKey(int mKey);
	String getValue();
	void setValue(String mValue);		
}
