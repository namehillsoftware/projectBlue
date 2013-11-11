package com.lasthopesoftware.bluewater.data.objects;

import java.util.ArrayList;

public interface IJrItem<T extends IJrItem<?>> extends Comparable<T> {
	ArrayList<T> getSubItems();
	Integer getKey();
	void setKey(Integer mKey);
	String getValue();
	void setValue(String mValue);		
}
