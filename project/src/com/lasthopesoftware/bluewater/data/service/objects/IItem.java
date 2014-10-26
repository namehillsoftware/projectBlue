package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;

public interface IItem<T extends IItem<?>> extends Comparable<T> {
	void addOnItemsCompleteListener(OnCompleteListener<List<T>> listener);
	void removeOnItemsCompleteListener(OnCompleteListener<List<T>> listener);
	void getSubItemsAsync();
	ArrayList<T> getSubItems() throws IOException;
	int getKey();
	void setKey(int mKey);
	String getValue();
	void setValue(String mValue);		
}
