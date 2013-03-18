package com.lasthopesoftware.jrmediastreamer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.ClientProtocolException;

import jrAccess.JrAccessDao;
import jrAccess.JrLookUpResponseHandler;
import jrAccess.JrResponse;
import jrAccess.JrSession;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
        try {
        	iv.setImageBitmap(new GetFileImage().execute(JrSession.playingFile.mKey.toString()).get());
        } catch (Exception e) {
        	e.printStackTrace();
        }
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
    
    private class GetFileImage extends AsyncTask<String, Void, Bitmap> {

		@Override
		protected Bitmap doInBackground(String... params) {
			
			Bitmap returnBmp = null;
			
	        try {
	        	URLConnection conn = (new URL(JrSession.accessDao.getJrUrl("File/GetImage", "File=" + params[0], "Size=Medium"))).openConnection();
	        	returnBmp = BitmapFactory.decodeStream(conn.getInputStream());
			} catch (Exception e) {
				e.printStackTrace();
			}
	        
	        return returnBmp;
		}
    }
}
