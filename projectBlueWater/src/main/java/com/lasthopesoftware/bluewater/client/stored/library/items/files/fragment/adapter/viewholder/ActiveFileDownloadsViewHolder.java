package com.lasthopesoftware.bluewater.client.stored.library.items.files.fragment.adapter.viewholder;

import android.widget.TextView;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileNameTextViewSetter;
import com.namehillsoftware.handoff.promises.Promise;

public class ActiveFileDownloadsViewHolder {

	public final TextView textView;
	public Promise<?> filePropertiesProvider;
	public final FileNameTextViewSetter fileNameTextViewSetter;

	public ActiveFileDownloadsViewHolder(TextView textView) {
		this.textView = textView;
		fileNameTextViewSetter = new FileNameTextViewSetter(textView);
	}
}
