package com.lasthopesoftware.bluewater.client.library.items.media.files.list;

import android.content.Context;
import android.widget.ArrayAdapter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;

import java.util.List;

public abstract class AbstractFileListAdapter extends ArrayAdapter<ServiceFile> {

	private final List<ServiceFile> mServiceFiles;

	protected AbstractFileListAdapter(Context context, int resource, List<ServiceFile> serviceFiles) {
		super(context, resource, serviceFiles);
		
		mServiceFiles = serviceFiles;
	}

	public final List<ServiceFile> getFiles() {
		return mServiceFiles;
	}
}
