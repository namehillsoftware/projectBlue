package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.File;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.list.AbstractFileListAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity.adapter.viewholder.ActiveFileDownloadsViewHolder;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.GetFileListItemTextTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 8/23/15.
 */
public class ActiveFileDownloadsAdapter extends AbstractFileListAdapter {

	public ActiveFileDownloadsAdapter(Context context, int resource, ConnectionProvider connectionProvider, List<StoredFile> storedFiles) {
		super(context, resource, getFilesFromStoredFiles(connectionProvider, storedFiles));
	}

	@Override
	public final View getView(final int position, View convertView, final ViewGroup parent) {

		if (convertView == null) {
			final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.layout_standard_text, parent, false);
			convertView.setTag(new ActiveFileDownloadsViewHolder((TextView) convertView.findViewById(R.id.tvStandard)));
		}

		final ActiveFileDownloadsViewHolder viewHolder = (ActiveFileDownloadsViewHolder)convertView.getTag();

		if (viewHolder.getFileListItemTextTask != null) viewHolder.getFileListItemTextTask.cancel(false);
		viewHolder.getFileListItemTextTask = new GetFileListItemTextTask(getItem(position), viewHolder.textView);
		viewHolder.getFileListItemTextTask.execute();

		return convertView;
	}

	private static List<IFile> getFilesFromStoredFiles(ConnectionProvider connectionProvider, List<StoredFile> storedFiles) {
		final ArrayList<IFile> files = new ArrayList<>(storedFiles.size());

		for (StoredFile storedFile : storedFiles)
			files.add(new File(connectionProvider, storedFile.getId()));

		return files;
	}
}
