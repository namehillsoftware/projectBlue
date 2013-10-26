package com.lasthopesoftware.bluewater.data.objects;

import java.io.IOException;
import java.util.ArrayList;

public interface IJrItem<T extends JrObject> {
	ArrayList<T> getSubItems() throws IOException;
	Integer getKey();
	void setKey(Integer mKey);
	String getValue();
	void setValue(String mValue);		
}
