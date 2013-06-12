package com.lasthopesoftware.bluewater;

import jrAccess.JrSession;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

public class ViewUtils {

	public static boolean handleMenuClicks(Context context, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_connection_settings:
				Intent intent = new Intent(context, ConnectionSettings.class);
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
}
