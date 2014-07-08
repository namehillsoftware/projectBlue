package com.lasthopesoftware.bluewater.activities.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.MenuItem;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.SelectServer;
import com.lasthopesoftware.bluewater.activities.ViewNowPlaying;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class ViewUtils {

	public static boolean handleMenuClicks(Context context, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_connection_settings:
				context.startActivity(new Intent(context, SelectServer.class));
				return true;
			case R.id.menu_view_now_playing:
				CreateNowPlayingView(context);
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
	
	public static void displayNowPlayingInMenu(final Context context, final OnGetNowPlayingSetListener listener) {
		
		JrSession.GetLibrary(context, new OnCompleteListener<Integer, Void, Library>() {
			
			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				listener.onGetNowPlayingSetComplete(result.getNowPlayingId() >= 0);
			}
		});
	}
	
	public static void CreateNowPlayingView(Context context) {
    	Intent viewIntent = new Intent(context, ViewNowPlaying.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(viewIntent);
    }
	
	public static boolean OkCancelDialog(Context context, String title, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title).setMessage(message);
		OkCancelListener listener = new OkCancelListener();
		builder.setPositiveButton(R.string.btn_ok, listener);
		builder.setNegativeButton(R.string.btn_cancel, listener);
		builder.create();
		return listener.getResult();
	}
	
	private static class OkCancelListener implements DialogInterface.OnClickListener {
		boolean mResult = false;
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			mResult = which == DialogInterface.BUTTON_POSITIVE;
		}
		
		public boolean getResult() {
			return mResult;
		}
	}
	
	public interface OnGetNowPlayingSetListener {
		void onGetNowPlayingSetComplete(Boolean isSet);
	}
}
