package com.lasthopesoftware.jrmediastreamer;

import java.net.URL;
import java.net.URLConnection;

import jrAccess.JrResponse;
import jrAccess.JrSession;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

public class ViewNowPlaying extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_now_playing);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        TextView tv = (TextView)findViewById(R.id.tvNowPlaying);
        tv.setText(JrSession.playingFile.mValue);
        
        ImageView iv = (ImageView)findViewById(R.id.imgNowPlaying);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_view_now_playing, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private class GetFileImage extends AsyncTask<String, Void, String> {
		
		@Override
		protected String doInBackground(String... params) {
			// Get authentication token
			String token = null;
			try {
				URLConnection authConn = (new URL(params[0] + "Authenticate")).openConnection();
				authConn.setReadTimeout(5000);
				authConn.setConnectTimeout(5000);
				if (!JrSession.UserAuthCode.isEmpty())
					authConn.setRequestProperty("Authorization", "basic " + JrSession.UserAuthCode);
				
		    	JrResponse response = JrResponse.fromInputStream(authConn.getInputStream());
		    	if (response != null && response.items.containsKey("Token"))
		    		token = response.items.get("Token");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return token;
		}
	}
}
