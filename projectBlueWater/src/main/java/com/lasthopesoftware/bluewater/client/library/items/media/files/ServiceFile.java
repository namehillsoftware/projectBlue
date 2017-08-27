package com.lasthopesoftware.bluewater.client.library.items.media.files;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.shared.IIntKey;

public final class ServiceFile implements IIntKey<ServiceFile> {

	private int key;

	public ServiceFile(int key) {
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
	public int compareTo(@NonNull ServiceFile another) {
		return getKey() - another.getKey();
	}

	@Override
	public int hashCode() {
		return key;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ServiceFile ? compareTo((ServiceFile)obj) == 0 : super.equals(obj);
	}

	@Override
	public String toString() {
		return "key: " + key;
	}
}
