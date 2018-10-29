package com.lasthopesoftware.resources.specs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.LinkedList;
import java.util.List;

public class BroadcastRecorder extends BroadcastReceiver {

	public final List<Intent> recordedIntents = new LinkedList<>();

	@Override
	public void onReceive(Context context, Intent intent) {
		recordedIntents.add(intent);
	}
}
