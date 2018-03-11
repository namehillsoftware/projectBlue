package com.lasthopesoftware.resources.notifications.channel.specs.GivenAChannelConfiguration;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.lasthopesoftware.resources.notifications.channel.ChannelConfiguration;
import com.lasthopesoftware.resources.notifications.channel.NotificationChannelActivator;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static android.content.Context.NOTIFICATION_SERVICE;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RequiresApi(api = Build.VERSION_CODES.O)
@RunWith(RobolectricTestRunner.class)
public class WhenActivatingTheChannel {

	private static final CreateAndHold<NotificationManager> notificationManager = new Lazy<>(() -> (NotificationManager) RuntimeEnvironment.application.getSystemService(NOTIFICATION_SERVICE));
	private static final CreateAndHold<String> channelId = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() throws Throwable {
			final NotificationChannelActivator activeNotificationChannelId =
				new NotificationChannelActivator(notificationManager.getObject());

			return activeNotificationChannelId.activateChannel(new ChannelConfiguration() {
				@Override
				public String getChannelId() {
					return "myActiveChannel";
				}

				@Override
				public String getChannelName() {
					return "a-name";
				}

				@Override
				public String getChannelDescription() {
					return "description";
				}

				@Override
				public int getChannelImportance() {
					return 4;
				}
			});
		}
	};

	@Test
	public void thenTheReturnedChannelIdIsCorrect() {
		assertThat(channelId.getObject()).isEqualTo("myActiveChannel");
	}

	@Test
	public void thenTheChannelNameIsCorrect() {
		assertThat(((NotificationChannel)shadowOf(notificationManager.getObject())
			.getNotificationChannel(channelId.getObject()))
			.getName())
			.isEqualTo("a-name");
	}

	@Test
	public void thenTheChannelIdIsCorrect() {
		assertThat(((NotificationChannel)shadowOf(notificationManager.getObject())
			.getNotificationChannel(channelId.getObject()))
			.getId())
			.isEqualTo("myActiveChannel");
	}

	@Test
	public void thenTheChannelDescriptionIsCorrect() {
		assertThat(((NotificationChannel)shadowOf(notificationManager.getObject())
			.getNotificationChannel(channelId.getObject()))
			.getDescription())
			.isEqualTo("description");
	}

	@Test
	public void thenTheChannelImportanceIsCorrect() {
		assertThat(((NotificationChannel)shadowOf(notificationManager.getObject())
			.getNotificationChannel(channelId.getObject()))
			.getImportance())
			.isEqualTo(4);
	}
}
