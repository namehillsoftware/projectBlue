package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.util.ArrayList;

public interface IItem<T extends IItem<?>> extends Comparable<T> {
	
	ArrayList<T> getSubItems() throws IOException;
	int getKey();
	void setKey(int mKey);
	String getValue();
	void setValue(String mValue);		
}
