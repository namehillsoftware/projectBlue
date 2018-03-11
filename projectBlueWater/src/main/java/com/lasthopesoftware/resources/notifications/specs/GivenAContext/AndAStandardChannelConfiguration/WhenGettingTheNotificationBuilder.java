package com.lasthopesoftware.resources.notifications.specs.GivenAContext.AndAStandardChannelConfiguration;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import com.lasthopesoftware.resources.notifications.NotificationBuilderSupplier;
import com.lasthopesoftware.resources.notifications.channel.ChannelConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(RobolectricTestRunner.class)
public class WhenGettingTheNotificationBuilder {

	private static NotificationCompat.Builder builder;

	@Before
	public void context() {
		final NotificationBuilderSupplier notificationBuilderSupplier =
			new NotificationBuilderSupplier(
				RuntimeEnvironment.application,
				new ChannelConfiguration() {
					@Override
					public String getChannelId() {
						return "MyFancyChannelId";
					}

					@Override
					public String getChannelName() {
						return null;
					}

					@Override
					public String getChannelDescription() {
						return null;
					}

					@Override
					public int getChannelImportance() {
						return 0;
					}
				});
		builder  = notificationBuilderSupplier.getNotificationBuilder();
	}

	@Test
	@RequiresApi(api = Build.VERSION_CODES.O)
	public void thenTheNotificationChannelIdIsCorrect() {
		assertThat(builder.build().getChannelId()).isEqualTo("MyFancyChannelId");
	}
}
