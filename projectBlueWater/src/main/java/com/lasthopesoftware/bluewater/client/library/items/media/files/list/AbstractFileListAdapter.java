package com.lasthopesoftware.bluewater.client.library.items.media.files.list;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;

import java.util.List;

public abstract class AbstractFileListAdapter extends ArrayAdapter<File> {

	private final List<File> mFiles;

	protected AbstractFileListAdapter(Context context, int resource, List<File> files) {
		super(context, resource, files);
		
		mFiles = files;
	}

	public final List<File> getFiles() {
		return mFiles;
	}
}
