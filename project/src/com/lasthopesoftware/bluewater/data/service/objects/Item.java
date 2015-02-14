package com.lasthopesoftware.bluewater.data.service.objects;



public class Item extends AbstractIntKeyStringValue implements IItem, IFilesContainer {
	private Files mFiles;

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
	public IItemFiles getFiles() {
		if (mFiles == null) mFiles = new Files("Browse/Files", "ID=" + String.valueOf(this.getKey()));
		return mFiles;
	}

	@Override
	public String[] getSubItemParams() {
		return new String[] { "Browse/Children", "ID=" + String.valueOf(this.getKey())};
	}
}
