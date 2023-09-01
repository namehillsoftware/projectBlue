package com.lasthopesoftware.bluewater.tutorials

import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.repository.Entity

@Keep
data class DisplayedTutorial(
	var tutorialKey: String? = null,
) : Entity
