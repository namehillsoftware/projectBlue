package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.fragment.adapter.viewholder;

import android.widget.TextView;

import com.lasthopesoftware.promises.IPromise;

import java.util.Map;

/**
 * Created by david on 8/23/15.
 */
public class ActiveFileDownloadsViewHolder {

	public final TextView textView;
	public IPromise<Map<String, String>> filePropertiesProvider;

	public ActiveFileDownloadsViewHolder(TextView textView) {

		this.textView = textView;
	}
}
