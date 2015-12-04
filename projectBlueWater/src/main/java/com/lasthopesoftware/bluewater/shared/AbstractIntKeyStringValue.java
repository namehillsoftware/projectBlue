package com.lasthopesoftware.bluewater.shared;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;

public abstract class AbstractIntKeyStringValue implements IIntKeyStringValue {
	private int mKey;
	private String mValue;
	
	protected AbstractIntKeyStringValue(int key, String value) {
		this.setKey(key);
		this.setValue(value);
	}
	
	public AbstractIntKeyStringValue(String value) {
		this.setKey(-1);
		this.setValue(value);
	}
	
	protected AbstractIntKeyStringValue() {
	}
	
	/**
	 * @return the mKey
	 */
	public int getKey() {
		return mKey;
	}

	/**
	 * @param mKey the mKey to set
	 */
	public void setKey(int mKey) {
		this.mKey = mKey;
	}

	/**
	 * @return the mValue
	 */
	public String getValue() {
		return mValue;
	}

	/**
	 * @param mValue the mValue to set
	 */
	public void setValue(String mValue) {
		this.mValue = mValue;
	}
	
	@Override
	public int compareTo(IItem another) {
		int result = this.getValue().compareTo(another.getValue());
		if (result == 0) result = this.getKey() - another.getKey();
		return result;
	}
}
