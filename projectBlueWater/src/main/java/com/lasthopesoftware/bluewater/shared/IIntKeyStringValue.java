package com.lasthopesoftware.bluewater.shared;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;

public interface IIntKeyStringValue extends Comparable<IItem> {
	/**
	 * @return the mKey
	 */
	int getKey();

	/**
	 * @param mKey the mKey to set
	 */
	void setKey(int mKey);

	/**
	 * @return the mValue
	 */
	String getValue();

	/**
	 * @param mValue the mValue to set
	 */
	void setValue(String mValue);
}
