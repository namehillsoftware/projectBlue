package com.lasthopesoftware.bluewater.client.stored.service.GivenARequestToSync;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.AndroidContext;
import com.lasthopesoftware.bluewater.client.stored.service.StoredSyncService;

import org.junit.Test;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@TargetApi(Build.VERSION_CODES.O)
public class WhenStartingTheService extends AndroidContext {

	private final Context spiedContext = spy(ApplicationProvider.getApplicationContext());

	@Override
	public void before() {
		StoredSyncService.doSync(spiedContext);
		shadowOf(spiedContext.getMainLooper()).idle();
	}

	@Test
	public void thenStartForegroundIsCalled() {
		verify(spiedContext, times(1)).startForegroundService(argThat(a -> StoredSyncService.class.getName().equals(a.getComponent().getClassName())));
	}
}
