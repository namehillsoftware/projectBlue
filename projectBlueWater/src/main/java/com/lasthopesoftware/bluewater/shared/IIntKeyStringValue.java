package com.lasthopesoftware.bluewater.shared;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;

public interface IIntKeyStringValue extends Comparable<IItem> {
	int getKey();
	void setKey(int key);
	String getValue();
	void setValue(String value);
}
