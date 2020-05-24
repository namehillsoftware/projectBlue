package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;

import java.util.Collection;
import java.util.List;

public abstract class AbstractFileListAdapter extends ArrayAdapter<ServiceFile> {

	private final Collection<ServiceFile> serviceFiles;

	protected AbstractFileListAdapter(Context context, int resource, List<ServiceFile> serviceFiles) {
		super(context, resource, serviceFiles);

		this.serviceFiles = serviceFiles;
	}

	public final Collection<ServiceFile> getFiles() {
		return serviceFiles;
	}
}
