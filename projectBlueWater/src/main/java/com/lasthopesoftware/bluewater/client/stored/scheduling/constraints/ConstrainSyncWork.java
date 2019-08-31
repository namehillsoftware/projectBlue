package com.lasthopesoftware.bluewater.client.stored.scheduling.constraints;

import androidx.work.Constraints;

public interface ConstrainSyncWork {
	Constraints getCurrentConstraints();
}
