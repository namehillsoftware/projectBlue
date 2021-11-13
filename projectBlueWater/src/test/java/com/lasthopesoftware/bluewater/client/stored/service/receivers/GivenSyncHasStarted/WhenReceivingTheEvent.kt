package com.lasthopesoftware.bluewater.client.stored.service.receivers.GivenSyncHasStarted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.SyncStartedReceiver;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

public class WhenReceivingTheEvent {

	private static final Collection<String> notifications = new ArrayList<>();

	@BeforeClass
	public static void context() {
		final SyncStartedReceiver receiver = new SyncStartedReceiver(notifications::add);
		receiver.onReceive(mock(Context.class), new Intent(StoredFileSynchronization.onSyncStartEvent));
	}

	@Test
	public void thenNotificationsBegin() {
		assertThat(notifications).containsExactly((String)null);
	}
}
