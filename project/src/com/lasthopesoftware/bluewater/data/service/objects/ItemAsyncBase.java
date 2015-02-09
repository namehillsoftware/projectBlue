package com.lasthopesoftware.bluewater.data.service.objects;



public abstract class ItemAsyncBase extends AbstractIntKeyStringValue implements IItem {
	
	public ItemAsyncBase(int key, String value) {
		super(key, value);
	}
	
	public ItemAsyncBase(String value) {
		super(value);
	}
	
	public ItemAsyncBase() {
	}

	@Override
	public int compareTo(IItem another) {
		int result = this.getValue().compareTo(another.getValue());
		if (result == 0) result = this.getKey() - another.getKey();
		return result;
	}
}
