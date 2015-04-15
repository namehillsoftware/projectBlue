package com.lasthopesoftware.bluewater.servers.library.items.media.files.list;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;

import java.util.List;

public abstract class AbstractFileListAdapter extends ArrayAdapter<IFile> {

	private final List<IFile> mFiles;

	public AbstractFileListAdapter(Context context, int resource, List<IFile> files) {
		super(context, resource, files);
		
		mFiles = files;
	}

	public final List<IFile> getFiles() {
		return mFiles;
	}
}
