package com.lasthopesoftware.bluewater.activities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers.HandleViewNowPlayingMessages;
import com.lasthopesoftware.bluewater.activities.ViewNowPlayingHelpers.ProgressTrackerThread;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.data.service.access.connection.JrConnection;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.services.OnStreamingStartListener;
import com.lasthopesoftware.bluewater.services.OnStreamingStopListener;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class ViewNowPlaying extends Activity implements OnStreamingStartListener, OnStreamingStopListener {
	private Thread mTrackerThread;
	private HandleViewNowPlayingMessages mHandler;
	private ImageButton mPlay;
	private ImageButton mPause;
	private ImageButton mNext;
	private ImageButton mPrevious;
	private RatingBar mSongRating;
	private static FrameLayout mContentView;
	private static RelativeLayout mControlNowPlaying, mViewCoverArt;
	private Timer mHideTimer;
	private TimerTask mTimerTask;
	
	private ProgressBar mSongProgressBar;
	private ProgressBar mLoadingImg;
	private ImageView mNowPlayingImg;
	private TextView mNowPlayingArtist;
	private TextView mNowPlayingTitle;
	private static GetFileImage getFileImageTask;
	
	private JrFile playingFile = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Library library = JrSession.GetLibrary(this);
		
		mContentView = new FrameLayout(this);
		setContentView(mContentView);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		mViewCoverArt = (RelativeLayout) inflater.inflate(R.layout.activity_view_cover_art, null);
		mControlNowPlaying = (RelativeLayout) inflater.inflate(R.layout.activity_control_now_playing, null);
		mControlNowPlaying.setVisibility(View.INVISIBLE);
		mContentView.addView(mViewCoverArt);
		mContentView.addView(mControlNowPlaying);
		
		mHideTimer = new Timer("Fade Timer");
		
		
		mContentView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mControlNowPlaying.setVisibility(View.VISIBLE);
				mContentView.invalidate();
				if (mTimerTask != null) mTimerTask.cancel();
				mHideTimer.purge();
				mTimerTask = new TimerTask() {
					
					@Override
					public void run() {
						Message msg = new Message();
						msg.what = HandleViewNowPlayingMessages.HIDE_CONTROLS;
						mHandler.sendMessage(msg);
					}
				};
				mHideTimer.schedule(mTimerTask, 5000);
			}
		});

		mPlay = (ImageButton) findViewById(R.id.btnPlay);
		mPause = (ImageButton) findViewById(R.id.btnPause);
		mNext = (ImageButton) findViewById(R.id.btnNext);
		mPrevious = (ImageButton) findViewById(R.id.btnPrevious);
		mSongRating = (RatingBar) findViewById(R.id.rbSongRating);
		mSongProgressBar = (ProgressBar) findViewById(R.id.pbNowPlaying);
		mLoadingImg = (ProgressBar) findViewById(R.id.pbLoadingImg);
		mNowPlayingImg = (ImageView) findViewById(R.id.imgNowPlaying);
		mNowPlayingArtist = (TextView) findViewById(R.id.tvSongArtist);
		mNowPlayingTitle = (TextView) findViewById(R.id.tvSongTitle);
		
		StreamingMusicService.AddOnStreamingStartListener(this);
		StreamingMusicService.AddOnStreamingStopListener(this);
		
		/* Toggle play/pause */
		TogglePlayPauseListener togglePlayPauseListener = new TogglePlayPauseListener();
		mPlay.setOnClickListener(togglePlayPauseListener);
		mPause.setOnClickListener(togglePlayPauseListener);

		mNext.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!mControlNowPlaying.isShown()) return;
				StreamingMusicService.Next(v.getContext());
			}
		});
		
		mPrevious.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mControlNowPlaying.isShown()) return;
				StreamingMusicService.Previous(v.getContext());
			}
		});
		
		mHandler = new HandleViewNowPlayingMessages(this);
		
		playingFile = StreamingMusicService.getNowPlayingFile();
		if (playingFile == null) {
			if (library.getNowPlayingId() <= 0) return;
			
			playingFile = new JrFile(library.getNowPlayingId());
		}
			
		
		if (mTrackerThread != null && mTrackerThread.isAlive()) mTrackerThread.interrupt();

		mTrackerThread = new Thread(new ProgressTrackerThread(playingFile, mHandler));
		mTrackerThread.setPriority(Thread.MIN_PRIORITY);
		mTrackerThread.setName("Tracker Thread");
		mTrackerThread.start();
		
		setView(playingFile);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		menu.findItem(R.id.menu_view_now_playing).setVisible(ViewUtils.displayNowPlayingMenu(this));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (ViewUtils.handleNavMenuClicks(this, item)) return true;
		return super.onOptionsItemSelected(item);
	}
	
	private static class TogglePlayPauseListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (!mControlNowPlaying.isShown()) return;
			
			if (StreamingMusicService.getNowPlayingFile() != null) {
				if (StreamingMusicService.getNowPlayingFile().isPlaying()) {
					StreamingMusicService.Pause(v.getContext());
					return;
				}
				
				if (StreamingMusicService.getNowPlayingFile().isPrepared()) StreamingMusicService.Play(v.getContext());
				return;
			}
			Library library = JrSession.GetLibrary(v.getContext());
			StreamingMusicService.StreamMusic(v.getContext(), library.getNowPlayingId(), library.getNowPlayingProgress(), library.getSavedTracksString());
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mHideTimer != null) {
			mHideTimer.cancel();
			mHideTimer.purge();
		}
		
		if (mTrackerThread != null) mTrackerThread.interrupt();
		StreamingMusicService.RemoveOnStreamingStartListener(this);
		StreamingMusicService.RemoveOnStreamingStopListener(this);
	}
	
	public FrameLayout getContentView() {
		return mContentView;
	}
	
	public RelativeLayout getControlNowPlaying() {
		return mControlNowPlaying;
	}
	
	public RelativeLayout getViewCoverArt() {
		return mViewCoverArt;
	}
	
	public ProgressBar getSongProgressBar() {
		return mSongProgressBar;
	}
	
	@SuppressWarnings("unchecked")
	private void setView(JrFile file) {
		final JrFile playingFile = file;
				
		try {
			@SuppressWarnings("rawtypes")
			OnErrorListener onSimpleIoExceptionErrors = new OnErrorListener() {
				
				@Override
				public boolean onError(ISimpleTask owner, Exception innerException) {
					return !(innerException instanceof IOException);
				}
			};
			
			SimpleTask<Void, Void, String> getArtistTask = new SimpleTask<Void, Void, String>();
			getArtistTask.addOnExecuteListener(new OnExecuteListener<Void, Void, String>() {
				
				@Override
				public void onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
					owner.setResult(playingFile.getProperty("Artist"));
				}
			});
			getArtistTask.addOnCompleteListener(new OnCompleteListener<Void, Void, String>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
					if (owner.getState() == SimpleTaskState.ERROR && containsIoException(owner.getExceptions())) {
						resetViewOnReconnect(playingFile);
						return;
					}
					
					mNowPlayingArtist.setText(result);
				}
			});
			getArtistTask.addOnErrorListener(onSimpleIoExceptionErrors);
			getArtistTask.execute();
			
			SimpleTask<Void, Void, String> getTitleTask = new SimpleTask<Void, Void, String>();
			getTitleTask.addOnExecuteListener(new OnExecuteListener<Void, Void, String>() {
				
				@Override
				public void onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
					owner.setResult(playingFile.getValue());
				}
			});
			getTitleTask.addOnCompleteListener(new OnCompleteListener<Void, Void, String>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
					if (owner.getState() == SimpleTaskState.ERROR && containsIoException(owner.getExceptions())) {
						resetViewOnReconnect(playingFile);
						return;
					}
					
					mNowPlayingTitle.setText(result);
				}
			});
			getTitleTask.addOnErrorListener(onSimpleIoExceptionErrors);
			getTitleTask.execute();
			
			SimpleTask<Void, Void, String> getAlbumTask = new SimpleTask<Void, Void, String>();
			getAlbumTask.addOnExecuteListener(new OnExecuteListener<Void, Void, String>() {
				
				@Override
				public void onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
					if (playingFile.getProperty("Album") != null)
						owner.setResult(playingFile.getProperty("Artist") + ":" + playingFile.getProperty("Album"));
				}
			});
			getAlbumTask.addOnCompleteListener(new OnCompleteListener<Void, Void, String>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
					if (owner.getState() == SimpleTaskState.ERROR && containsIoException(owner.getExceptions())) {
						resetViewOnReconnect(playingFile);
						return;
					}
					
					try {			
						// Cancel the getFileImageTask if it is already in progress
						if (getFileImageTask != null && (getFileImageTask.getStatus() == AsyncTask.Status.PENDING || getFileImageTask.getStatus() == AsyncTask.Status.RUNNING)) {
							getFileImageTask.cancel(true);
						}
						
						getFileImageTask = new GetFileImage(mNowPlayingImg, mLoadingImg);
						
						getFileImageTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, result == null ? playingFile.getKey().toString() : result, playingFile.getKey().toString());
					} catch (Exception e) {
						LoggerFactory.getLogger(ViewNowPlaying.class).error(e.toString(), e);
					}
				}
			});
			getAlbumTask.addOnErrorListener(onSimpleIoExceptionErrors);
			getAlbumTask.execute();
			
			SimpleTask<Void, Void, Float> getRatingsTask = new SimpleTask<Void, Void, Float>();
			getRatingsTask.addOnExecuteListener(new OnExecuteListener<Void, Void, Float>() {
				
				@Override
				public void onExecute(ISimpleTask<Void, Void, Float> owner, Void... params) throws Exception {
					owner.setResult((float) 0);
					if (playingFile.getProperty("Rating") != null && !playingFile.getProperty("Rating").isEmpty())
						owner.setResult(Float.valueOf(playingFile.getProperty("Rating")));
				}
			});
			getRatingsTask.addOnCompleteListener(new OnCompleteListener<Void, Void, Float>() {
				
				@Override
				public void onComplete(ISimpleTask<Void, Void, Float> owner, Float result) {
					if (owner.getState() == SimpleTaskState.ERROR && containsIoException(owner.getExceptions())) {
						resetViewOnReconnect(playingFile);
						return;
					}
					
					mSongRating.setRating(result);
					mSongRating.invalidate();
					
					mSongRating.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
						
						@Override
						public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
							if (!fromUser || !mControlNowPlaying.isShown()) return;
							playingFile.setProperty("Rating", String.valueOf(Math.round(rating)));
						}
					});
				}
			});
			getRatingsTask.addOnErrorListener(onSimpleIoExceptionErrors);
			getRatingsTask.execute();
			
			
			mSongProgressBar.setMax(playingFile.getDuration());
			
			mSongProgressBar.setProgress(playingFile.getCurrentPosition());
		} catch (IOException ioE) {
			resetViewOnReconnect(playingFile);
		}
		
		mPlay.setVisibility(!file.isPlaying() ? View.VISIBLE : View.INVISIBLE);
		mPause.setVisibility(file.isPlaying() ? View.VISIBLE : View.INVISIBLE);
	}
	
	private boolean containsIoException(List<Exception> exceptions) {
		for (Exception exception : exceptions)
			if (exception instanceof IOException) return true;
		
		return false;
	}
	
	private void resetViewOnReconnect(JrFile file) {
		final JrFile playingFile = file;
		PollConnectionTask.Instance.get().addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
				if (result == Boolean.TRUE) setView(playingFile);
			}
		});
		WaitForConnectionDialog.show(this);
	}
	
	private static class GetFileImage extends AsyncTask<String, Void, Bitmap> {
		private ImageView mNowPlayingImg;
		private ProgressBar mLoadingImg;
		
		private final int cacheSize = 5;			
		private static LruCache<String, Bitmap> imageCache;
		
		private static Bitmap emptyBitmap;

		public GetFileImage(ImageView nowPlayingImg, ProgressBar loadingImg) {
			super();
			mNowPlayingImg = nowPlayingImg;
			mLoadingImg = loadingImg;
			if (imageCache == null) imageCache = new LruCache<String, Bitmap>(cacheSize);
		}
		
		@Override
		protected void onPreExecute() {
			mNowPlayingImg.setVisibility(View.INVISIBLE);
			mLoadingImg.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected Bitmap doInBackground(String... params) {
			
			Bitmap returnBmp = null;
			String uId = params[0];
			String fileKey = params[1];
			
			if (imageCache.get(uId) != null) {
				return imageCache.get(uId);
			}
			
			try {
				JrConnection conn = new JrConnection(
											"File/GetImage", 
											"File=" + fileKey, 
											"Type=Full", 
											"Pad=1",
											"Format=png",
											"FillTransparency=ffffff");
				
				if (isCancelled()) return null;
				
				try {
					returnBmp = BitmapFactory.decodeStream(conn.getInputStream());
				} finally {
					conn.disconnect();
				}
			} catch (FileNotFoundException fe) {
				LoggerFactory.getLogger(ViewNowPlaying.class).error(fe.toString(), fe);
			} catch (Exception e) {
				LoggerFactory.getLogger(ViewNowPlaying.class).error(e.toString(), e);
			}
			
			if (returnBmp == null) {
				if (emptyBitmap == null) {
					emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
				}
				
				returnBmp = emptyBitmap;
			}
			
			imageCache.put(uId, returnBmp);				
			
			return returnBmp;
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			mNowPlayingImg.setImageBitmap(result);
			mNowPlayingImg.setScaleType(ScaleType.CENTER_CROP);
			mLoadingImg.setVisibility(View.INVISIBLE);
			mNowPlayingImg.setVisibility(View.VISIBLE);
		}
	}
	

	@Override
	public void onStreamingStart(StreamingMusicService service, JrFile file) {		
		setView(file);
		mPause.setVisibility(View.VISIBLE);
		mPlay.setVisibility(View.INVISIBLE);
		
		if (mTrackerThread != null && mTrackerThread.isAlive()) mTrackerThread.interrupt();

		mTrackerThread = new Thread(new ProgressTrackerThread(file, mHandler));
		mTrackerThread.setPriority(Thread.MIN_PRIORITY);
		mTrackerThread.setName("Tracker Thread");
		mTrackerThread.start();
	}
	
	@Override
	public void onStreamingStop(StreamingMusicService service, JrFile file) {
		if (mTrackerThread != null && mTrackerThread.isAlive()) mTrackerThread.interrupt();
		
		mPlay.setVisibility(View.VISIBLE);
		mPause.setVisibility(View.INVISIBLE);
	}
}
