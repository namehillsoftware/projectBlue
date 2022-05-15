package com.lasthopesoftware.bluewater.client.browsing.items.media.files.details

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.FileDetailsActivity.Companion.launchFileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler

class ViewFileDetailsClickListener(
    viewFlipper: NotifyOnFlipViewAnimator,
    private val serviceFile: ServiceFile
) : AbstractMenuClickHandler(viewFlipper)
{
    override fun onClick(v: View) {
		v.context.launchFileDetailsActivity(serviceFile)
        super.onClick(v)
    }
}
