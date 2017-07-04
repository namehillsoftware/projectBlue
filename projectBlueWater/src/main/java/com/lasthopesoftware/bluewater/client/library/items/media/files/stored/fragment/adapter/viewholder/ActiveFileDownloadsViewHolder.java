package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.fragment.adapter.viewholder;

import android.widget.TextView;

import com.lasthopesoftware.messenger.promises.Promise;

import java.util.Map;

/**
 * Created by david on 8/23/15.
 */
public class ActiveFileDownloadsViewHolder {

	public final TextView textView;
	public Promise<Map<String, String>> filePropertiesProvider;

	public ActiveFileDownloadsViewHolder(TextView textView) {

		this.textView = textView;
	}
}
