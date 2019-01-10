package com.lasthopesoftware.bluewater.client.sync.constraints;

import androidx.work.Constraints;

public interface ConstrainSyncWork {
	Constraints getCurrentConstraints();
}
