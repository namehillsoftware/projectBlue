package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import android.widget.ImageButton;

public class BaseMenuViewHolder {
	public BaseMenuViewHolder(final ImageButton viewFileDetailsButton, final ImageButton playButton) {
		this.viewFileDetailsButton = viewFileDetailsButton;
		this.playButton = playButton;
	}
	
	public final ImageButton viewFileDetailsButton;
	public final ImageButton playButton;
}
