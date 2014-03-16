package com.lasthopesoftware.bluewater.data.service.objects;

public abstract class JrObject {
	private int mKey;
	private String mValue;
	
	public JrObject(int key, String value) {
		this.setKey(key);
		this.setValue(value);
	}
	
	public JrObject(String value) {
		this.setKey(-1);
		this.setValue(value);
	}
	
	public JrObject() {
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
}
