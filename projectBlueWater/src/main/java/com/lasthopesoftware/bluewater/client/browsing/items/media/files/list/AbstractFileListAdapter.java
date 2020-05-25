package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list;

import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;

import java.util.Collection;
import java.util.List;

public abstract class AbstractFileListAdapter<ViewHolder extends RecyclerView.ViewHolder> extends ListAdapter<ServiceFile, ViewHolder>
{
	private final Collection<ServiceFile> serviceFiles;

	protected AbstractFileListAdapter(List<ServiceFile> serviceFiles) {
		super();

		this.serviceFiles = serviceFiles;
	}

	public final Collection<ServiceFile> getFiles() {
		return serviceFiles;
	}
}
