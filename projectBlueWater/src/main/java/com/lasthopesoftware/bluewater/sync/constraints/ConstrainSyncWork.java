package com.lasthopesoftware.bluewater.sync.constraints;

import androidx.work.Constraints;

public interface ConstrainSyncWork {
	Constraints getCurrentConstraints();
}
