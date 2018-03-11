package com.lasthopesoftware.resources.notifications.channel.specs.GivenAChannelConfiguration;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.lasthopesoftware.resources.notifications.channel.ChannelConfiguration;
import com.lasthopesoftware.resources.notifications.channel.NotificationChannelBuilder;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(RobolectricTestRunner.class)
@RequiresApi(api = Build.VERSION_CODES.O)
public class WhenBuildingTheChannel {

	private static final CreateAndHold<NotificationChannel> notificationChannelBuilder = new AbstractSynchronousLazy<NotificationChannel>() {
		@Override
		protected NotificationChannel create() throws Throwable {
			return new NotificationChannelBuilder(new ChannelConfiguration() {
				@Override
				public String getChannelId() {
					return "MyCrazyChannel";
				}

				@Override
				public String getChannelName() {
					return "My Crazy Channel Name?";
				}

				@Override
				public String getChannelDescription() {
					return "This is some crazy person's channel";
				}

				@Override
				public int getChannelImportance() {
					return NotificationManager.IMPORTANCE_MAX;
				}
			}).buildNotificationChannel();
		}
	};

	@Test
	public void thenTheChannelIdIsCorrect() {
		assertThat(notificationChannelBuilder.getObject().getId()).isEqualTo("MyCrazyChannel");
	}

	@Test
	public void thenTheChannelNameIsCorrect() {
		assertThat(notificationChannelBuilder.getObject().getName()).isEqualTo("My Crazy Channel Name?");
	}

	@Test
	public void thenTheChannelDescriptionIsCorrect() {
		assertThat(notificationChannelBuilder.getObject().getDescription()).isEqualTo("This is some crazy person's channel");
	}

	@Test
	public void thenTheChannelIsSuperImportant() {
		assertThat(notificationChannelBuilder.getObject().getImportance()).isEqualTo(NotificationManager.IMPORTANCE_MAX);
	}
}
