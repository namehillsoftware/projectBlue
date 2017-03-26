package com.lasthopesoftware.bluewater.client.library.items.media.files;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.shared.IIntKey;

public class File implements IIntKey<File> {

	private int key;

	public File(int key) {
		this.setKey(key);
	}
	
	@Override
	public int getKey() {
		return key;
	}

	@Override
	public void setKey(int key) {
		this.key = key;
	}

	@Override
	public int compareTo(@NonNull File another) {
		return getKey() - another.getKey();
	}

	@Override
	public int hashCode() {
		return key;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof File ? compareTo((File)obj) == 0 : super.equals(obj);
	}
}
