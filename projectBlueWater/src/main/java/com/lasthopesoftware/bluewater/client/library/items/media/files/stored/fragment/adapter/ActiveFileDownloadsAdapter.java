package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.fragment.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.list.AbstractFileListAdapter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.menu.FileNameTextViewSetter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.fragment.adapter.viewholder.ActiveFileDownloadsViewHolder;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;

import java.util.List;

/**
 * Created by david on 8/23/15.
 */
public class ActiveFileDownloadsAdapter extends AbstractFileListAdapter {

	public ActiveFileDownloadsAdapter(Context context, List<StoredFile> storedFiles) {
		super(context, R.id.tvStandard, Stream.of(storedFiles).map(s -> new File(s.getServiceId())).collect(Collectors.toList()));
	}

	@NonNull
	@Override
	public final View getView(final int position, View convertView, @NonNull final ViewGroup parent) {

		if (convertView == null) {
			final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.layout_standard_text, parent, false);
			convertView.setTag(new ActiveFileDownloadsViewHolder((TextView) convertView.findViewById(R.id.tvStandard)));
		}

		final ActiveFileDownloadsViewHolder viewHolder = (ActiveFileDownloadsViewHolder)convertView.getTag();

		if (viewHolder.filePropertiesProvider != null) viewHolder.filePropertiesProvider.cancel();
		viewHolder.filePropertiesProvider = FileNameTextViewSetter.startNew(getItem(position), viewHolder.textView);

		return convertView;
	}
}
