package com.lasthopesoftware.jrmediastreamer;

import jrAccess.JrConnection;
import jrAccess.JrSession;
import jrFileSystem.JrFile;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.SeekBar;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

public class ViewNowPlaying extends Activity implements Runnable {
	private TextView mNowPlayingText;
	private ImageView mNowPlayingImg;
	private SeekBar mSeekbar;
	
	private static int UPDATE_ALL = 0;
	private static int UPDATE_SB = 1;
	private static int SET_STOPPED = 2;
	
	@SuppressLint("HandlerLeak")
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 == SET_STOPPED) {
				mSeekbar.setProgress(0);
			} else if (msg.arg1 == UPDATE_ALL) {
				setView();
			} else if (msg.arg1 == UPDATE_SB) {
				mSeekbar.setMax(JrSession.playingFile.getMediaPlayer().getDuration());
				mSeekbar.setProgress(JrSession.playingFile.getMediaPlayer().getCurrentPosition());
			}
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_now_playing);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mSeekbar = (SeekBar)findViewById(R.id.sbNowPlaying);
        mNowPlayingImg = (ImageView)findViewById(R.id.imgNowPlaying);
        mNowPlayingText = (TextView)findViewById(R.id.tvNowPlaying);
        
        Thread trackerThread = new Thread(this);
        trackerThread.setPriority(Thread.MIN_PRIORITY);
        trackerThread.setName("Tracker Thread");
        trackerThread.start();
    }
    
    private void setView() {
    	String title = JrSession.playingFile.getArtist() + " - " + JrSession.playingFile.getValue();
    	if (JrSession.playingFile.getAlbum() != null)
    		title += "\n (" + JrSession.playingFile.getAlbum() + ")";
        
        mNowPlayingText.setText(title);
        try {
        	mNowPlayingImg.setImageBitmap(new GetFileImage().execute(JrSession.playingFile.getKey().toString()).get());
        	mNowPlayingImg.setScaleType(ScaleType.CENTER_INSIDE);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        mSeekbar.setMax(JrSession.playingFile.getMediaPlayer().getDuration());
		mSeekbar.setProgress(JrSession.playingFile.getMediaPlayer().getCurrentPosition());
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
	        	JrConnection conn = new JrConnection("File/GetImage", "File=" + params[0], "Size=Large", "Type=Full");
	        	returnBmp = BitmapFactory.decodeStream(conn.getInputStream());
			} catch (Exception e) {
				e.printStackTrace();
			}
	        
	        return returnBmp;
		}
    }

	@Override
	public void run() {
		JrFile playingFile = null;
		Message msg;
		while (JrSession.playlist != null && !JrSession.playlist.isEmpty()) {
			msg = null;
			try {
				if (JrSession.playingFile == null) {
					playingFile = null;
					msg = new Message();
					msg.arg1 = SET_STOPPED;
				}
				else if ((playingFile == null && JrSession.playingFile != null) || !playingFile.equals(JrSession.playingFile)) {
					playingFile = JrSession.playingFile;
					msg = new Message();
					msg.arg1 = UPDATE_ALL;
				}
				else if (JrSession.playingFile.isPlaying()) {
					msg = new Message();
					msg.arg1 = UPDATE_SB;
				}
				if (msg != null) handler.sendMessage(msg);
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
