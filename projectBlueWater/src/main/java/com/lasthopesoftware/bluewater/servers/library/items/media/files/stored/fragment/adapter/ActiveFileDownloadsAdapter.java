package com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.fragment.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.File;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.list.AbstractFileListAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.FileNameTextViewSetter;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.fragment.adapter.viewholder.ActiveFileDownloadsViewHolder;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.repository.StoredFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 8/23/15.
 */
public class ActiveFileDownloadsAdapter extends AbstractFileListAdapter {

	public ActiveFileDownloadsAdapter(Context context, List<StoredFile> storedFiles) {
		super(context, R.id.tvStandard, getFilesFromStoredFiles(storedFiles));
	}

	@Override
	public final View getView(final int position, View convertView, final ViewGroup parent) {

		if (convertView == null) {
			final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.layout_standard_text, parent, false);
			convertView.setTag(new ActiveFileDownloadsViewHolder((TextView) convertView.findViewById(R.id.tvStandard)));
		}

		final ActiveFileDownloadsViewHolder viewHolder = (ActiveFileDownloadsViewHolder)convertView.getTag();

		if (viewHolder.filePropertiesProvider != null) viewHolder.filePropertiesProvider.cancel(false);
		viewHolder.filePropertiesProvider = FileNameTextViewSetter.startNew(getItem(position), viewHolder.textView);

		return convertView;
	}

	private static List<IFile> getFilesFromStoredFiles(List<StoredFile> storedFiles) {
		final ArrayList<IFile> files = new ArrayList<>(storedFiles.size());

		for (StoredFile storedFile : storedFiles)
			files.add(new File(storedFile.getServiceId()));

		return files;
	}
}
