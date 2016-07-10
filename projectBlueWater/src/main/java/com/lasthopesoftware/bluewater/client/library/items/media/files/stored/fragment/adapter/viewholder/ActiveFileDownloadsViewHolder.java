package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.fragment.adapter.viewholder;

import android.widget.TextView;

import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;

/**
 * Created by david on 8/23/15.
 */
public class ActiveFileDownloadsViewHolder {

	public final TextView textView;
	public CachedFilePropertiesProvider filePropertiesProvider;

	public ActiveFileDownloadsViewHolder(TextView textView) {

		this.textView = textView;
	}
}
