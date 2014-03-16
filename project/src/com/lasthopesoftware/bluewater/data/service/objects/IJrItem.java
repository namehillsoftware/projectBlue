package com.lasthopesoftware.bluewater.data.service.objects;

import java.util.ArrayList;

public interface IJrItem<T extends IJrItem<?>> extends Comparable<T> {
	ArrayList<T> getSubItems();
	int getKey();
	void setKey(int mKey);
	String getValue();
	void setValue(String mValue);		
}
