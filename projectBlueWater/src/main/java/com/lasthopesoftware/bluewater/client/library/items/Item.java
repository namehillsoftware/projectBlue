package com.lasthopesoftware.bluewater.client.library.items;

import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;



public class Item extends AbstractIntKeyStringValue implements IItem {

	public Item(int key, String value) {
		super(key, value);

	}
	
	public Item(int key) {
		super();

		this.setKey(key);
	}
	
	public Item() {
		super();

	}

	@Override
    public int hashCode() {
        return getKey();
    }
}
