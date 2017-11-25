package com.lasthopesoftware.resources.intents.specs;

import android.content.ComponentName;
import android.content.Intent;

import com.lasthopesoftware.resources.intents.IIntentFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class FakeIntentFactory implements IIntentFactory {
	@Override
	public Intent getIntent(Class cls) {
		final Intent mockIntent = mock(Intent.class);
		final ComponentName mockComponent = mock(ComponentName.class);
		when(mockComponent.getClassName())
			.thenReturn(cls.getName());
		when(mockComponent.getPackageName())
			.thenReturn("Test");
		when(mockIntent.getComponent()).thenReturn(mockComponent);

		return mockIntent;
	}
}
