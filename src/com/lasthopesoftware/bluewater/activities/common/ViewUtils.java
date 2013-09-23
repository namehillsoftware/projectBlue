package com.lasthopesoftware.bluewater.activities.common;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.R.id;
import com.lasthopesoftware.bluewater.R.string;
import com.lasthopesoftware.bluewater.activities.SetConnection;
import com.lasthopesoftware.bluewater.activities.ViewNowPlaying;
import com.lasthopesoftware.bluewater.data.access.JrSession;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class ViewUtils {

	public static boolean handleMenuClicks(Context context, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_connection_settings:
				Intent intent = new Intent(context, SetConnection.class);
				context.startActivity(intent);
				return true;
			case R.id.menu_view_now_playing:
				CreateNowPlayingView(context);
				return true;
			default:
				return false;
		}
	}
	
	public static boolean handleNavMenuClicks(Context context, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask((Activity)context);
			return true;
		}
		return ViewUtils.handleMenuClicks(context, item);
		
	}
	
	public static boolean displayNowPlayingMenu() {
		return JrSession.playingFile != null;
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
			// TODO Auto-generated method stub
			mResult = which == DialogInterface.BUTTON_POSITIVE;
		}
		
		public boolean getResult() {
			return mResult;
		}
	}
}
