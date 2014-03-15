package com.lasthopesoftware.bluewater.activities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
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
import com.lasthopesoftware.bluewater.data.service.access.connection.JrConnection;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrPlaylistController;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class ViewNowPlaying extends Activity implements OnNowPlayingChangeListener, OnNowPlayingStopListener, ISimpleTask.OnStartListener<String, Void, Boolean> {
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
	
	private JrFilePlayer mFilePlayer = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Library library = JrSession.GetLibrary(this);
		
		mContentView = new FrameLayout(this);
		setContentView(mContentView);
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
		PollConnectionTask.Instance.get().addOnStartListener(this);
		
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
		
		if (StreamingMusicService.getPlaylistController() == null) {
			if (library.getNowPlayingId() <= 0) return;
			StreamingMusicService.InitializePlaylist(this, library.getNowPlayingId(), library.getNowPlayingProgress(), library.getSavedTracksString());
			return;
		}
		
		mFilePlayer = StreamingMusicService.getPlaylistController().getCurrentFilePlayer();
				
		if (mTrackerThread != null && mTrackerThread.isAlive()) mTrackerThread.interrupt();

		mTrackerThread = new Thread(new ProgressTrackerThread(mFilePlayer, mHandler));
		mTrackerThread.setPriority(Thread.MIN_PRIORITY);
		mTrackerThread.setName("Tracker Thread");
		mTrackerThread.start();
		
		setView(mFilePlayer);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_now_playing, menu);
		setRepeatingIcon(menu.findItem(R.id.menu_repeat_playlist));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_connection_settings:
			startActivity(new Intent(this, SelectServer.class));
			return true;
		case R.id.menu_repeat_playlist:
			StreamingMusicService.setIsRepeating(this, !JrSession.GetLibrary(this).isRepeating());
			setRepeatingIcon(item);
			return true;
		case R.id.menu_view_now_playing_files:
			startActivity(new Intent(this, ViewNowPlayingFiles.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void setRepeatingIcon(MenuItem item) {
		item.setIcon(JrSession.GetLibrary(this).isRepeating() ? R.drawable.av_repeat_dark : R.drawable.av_no_repeat_dark);
	}
	
	private static class TogglePlayPauseListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (!mControlNowPlaying.isShown()) return;
			
			if (StreamingMusicService.getPlaylistController() != null) {
				if (StreamingMusicService.getPlaylistController().isPlaying()) {
					StreamingMusicService.Pause(v.getContext());
					return;
				}
				
				if (StreamingMusicService.getPlaylistController().isPrepared()) {
					StreamingMusicService.Play(v.getContext());
					return;
				}
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
		PollConnectionTask.Instance.get().removeOnStartListener(this);
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
	private void setView(JrFilePlayer filePlayer) {
		final JrFile playingFile = filePlayer.getFile();
		final JrFilePlayer _filePlayer = filePlayer;
				
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
						resetViewOnReconnect(_filePlayer);
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
						resetViewOnReconnect(_filePlayer);
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
						resetViewOnReconnect(_filePlayer);
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
						resetViewOnReconnect(_filePlayer);
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
			
			
			mSongProgressBar.setMax(filePlayer.getDuration());
			
			mSongProgressBar.setProgress(filePlayer.getCurrentPosition());
		} catch (IOException ioE) {
			resetViewOnReconnect(_filePlayer);
		}
		
		mPlay.setVisibility(!filePlayer.isPlaying() ? View.VISIBLE : View.INVISIBLE);
		mPause.setVisibility(filePlayer.isPlaying() ? View.VISIBLE : View.INVISIBLE);
	}
	
	private boolean containsIoException(List<Exception> exceptions) {
		for (Exception exception : exceptions)
			if (exception instanceof IOException) return true;
		
		return false;
	}
	
	private void resetViewOnReconnect(JrFilePlayer filePlayer) {
		final JrFilePlayer _filePlayer = filePlayer;
		PollConnectionTask.Instance.get().addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
				if (result == Boolean.TRUE) setView(_filePlayer);
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
				LoggerFactory.getLogger(ViewNowPlaying.class).warn("Image not found!");
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
	public void onNowPlayingChange(JrPlaylistController controller, JrFilePlayer filePlayer) {		
		setView(filePlayer);
		
		if (mTrackerThread != null && mTrackerThread.isAlive()) mTrackerThread.interrupt();

		mTrackerThread = new Thread(new ProgressTrackerThread(filePlayer, mHandler));
		mTrackerThread.setPriority(Thread.MIN_PRIORITY);
		mTrackerThread.setName("Tracker Thread");
		mTrackerThread.start();
	}
	
	@Override
	public void onNowPlayingStop(JrPlaylistController controller, JrFilePlayer file) {
		if (mTrackerThread != null && mTrackerThread.isAlive()) mTrackerThread.interrupt();
		
		mPlay.setVisibility(View.VISIBLE);
		mPause.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onStart(ISimpleTask<String, Void, Boolean> owner) {
		WaitForConnectionDialog.show(this);
	}
}
