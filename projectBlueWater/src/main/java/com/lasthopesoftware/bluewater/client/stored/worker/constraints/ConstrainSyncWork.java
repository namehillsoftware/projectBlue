package com.lasthopesoftware.bluewater.client.stored.worker.constraints;

import androidx.work.Constraints;

public interface ConstrainSyncWork {
	Constraints getCurrentConstraints();
}
