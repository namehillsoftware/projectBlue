package com.lasthopesoftware.bluewater;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class ViewUtils {

	public static boolean handleMenuClicks(Context context, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_connection_settings:
			Intent intent = new Intent(context, ConnectionSettings.class);
			context.startActivity(intent);
			return true;
		}
		
		return false;
	}
	
	public static boolean handleNavMenuClicks(Context context, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask((Activity)context);
			return true;
		}
		return ViewUtils.handleMenuClicks(context, item);
		
	}
}
