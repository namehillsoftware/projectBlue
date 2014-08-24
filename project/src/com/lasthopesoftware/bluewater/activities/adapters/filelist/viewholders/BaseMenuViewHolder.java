package com.lasthopesoftware.bluewater.activities.adapters.filelist.viewholders;

import android.widget.ImageButton;

public class BaseMenuViewHolder {
	public BaseMenuViewHolder(final ImageButton viewFileDetailsButton, final ImageButton playButton) {
		this.viewFileDetailsButton = viewFileDetailsButton;
		this.playButton = playButton;
	}
	
	public final ImageButton viewFileDetailsButton;
	public final ImageButton playButton;
}
