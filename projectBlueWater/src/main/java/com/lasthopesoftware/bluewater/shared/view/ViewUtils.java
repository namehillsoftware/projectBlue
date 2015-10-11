package com.lasthopesoftware.bluewater.shared.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.ServerListActivity;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.NowPlayingActivity;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackController;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class ViewUtils {

    private static boolean mIsNowPlayingVisible = false;

	public final static boolean buildStandardMenu(final Activity activity, final Menu menu) {
		activity.getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		
//		final SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
//	    final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
//	    searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
		
//		final int id = searchView.getResources().getIdentifier("android:id/search_src_text", null, null);
//		final TextView textView = (TextView) searchView.findViewById(id);
//		textView.setTextColor(Color.WHITE);
		return true;
	}
	
	public static boolean handleMenuClicks(final Context context, final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_connection_settings:
				context.startActivity(new Intent(context, ServerListActivity.class));
				return true;
			default:
				return false;
		}
	}
	
	public static boolean handleNavMenuClicks(Activity activity, MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent upIntent = NavUtils.getParentActivityIntent(activity);
		        if (NavUtils.shouldUpRecreateTask(activity, upIntent)) {
		            // This activity is NOT part of this app's task, so create a new task
		            // when navigating up, with a synthesized back stack.
		            TaskStackBuilder.create(activity)
		                    // Add all of this activity's parents to the back stack
		                    .addNextIntentWithParentStack(upIntent)
		                    // Navigate up to the closest parent
		                    .startActivities();
		        } else {
		            // This activity is part of this app's task, so simply
		            // navigate up to the logical parent activity.
		            NavUtils.navigateUpTo(activity, upIntent);
		        }

				return true;
		}
		return ViewUtils.handleMenuClicks(activity, item);
		
	}
		
	public static void CreateNowPlayingView(final Context context) {
    	final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(viewIntent);
    }

	public static void InitializeNowPlayingFloatingActionButton(final FloatingActionButton floatingActionButton) {
		floatingActionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CreateNowPlayingView(v.getContext());
			}
		});

		floatingActionButton.setVisibility(GetVisibility(false));
		// The user can change the library, so let's check if the state of visibility on the
		// now playing menu item should change
		LibrarySession.GetLibrary(floatingActionButton.getContext(), new OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				mIsNowPlayingVisible = result != null && result.getNowPlayingId() >= 0;
				floatingActionButton.setVisibility(GetVisibility(mIsNowPlayingVisible));

				if (mIsNowPlayingVisible) return;

				// If now playing shouldn't be visible, detect when it should be
				PlaybackService.addOnStreamingStartListener(new OnNowPlayingStartListener() {
					@Override
					public void onNowPlayingStart(PlaybackController controller, IPlaybackFile filePlayer) {
						mIsNowPlayingVisible = true;
						floatingActionButton.setVisibility(GetVisibility(mIsNowPlayingVisible));
						PlaybackService.removeOnStreamingStartListener(this);
					}
				});
			}
		});
	}

	public static int GetVisibility(boolean isVisible) {
		return isVisible ? View.VISIBLE : View.INVISIBLE;
	}
}
