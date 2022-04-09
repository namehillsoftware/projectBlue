package com.lasthopesoftware.bluewater.client.browsing.items.media.files.details

import android.content.Intent
import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.FileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler

class ViewFileDetailsClickListener(
    viewFlipper: NotifyOnFlipViewAnimator,
    private val serviceFile: ServiceFile
) : AbstractMenuClickHandler(viewFlipper)
{
    override fun onClick(v: View) {
        val intent = Intent(v.context, FileDetailsActivity::class.java)
        intent.putExtra(FileDetailsActivity.fileKey, serviceFile.key)
        v.context.startActivity(intent)
        super.onClick(v)
    }
}
