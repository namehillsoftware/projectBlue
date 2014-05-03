package com.lasthopesoftware.bluewater.activities.adapters;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewFileDetails;
import com.lasthopesoftware.bluewater.activities.common.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.activities.listeners.OnSwipeListener;
import com.lasthopesoftware.bluewater.activities.listeners.OnSwipeListener.OnSwipeRightListener;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrPlaylistController;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItemFiles;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.JrFiles;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class FileListAdapter extends ArrayAdapter<JrFile> {
	
	private List<JrFile> mFiles;
	
	public FileListAdapter(Context context, int resource, List<JrFile> files) {
		super(context, resource, files);
		
		mFiles = files;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		final ViewFlipper parentView = new ViewFlipper(parent.getContext());
		parentView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		final  OnSwipeListener onSwipeListener = new OnSwipeListener(parentView.getContext());
		onSwipeListener.setOnSwipeRightListener(new OnSwipeRightListener() {
			
			@Override
			public boolean onSwipeRight(View view) {
				parentView.showPrevious();
				return true;
			}
		});
		parentView.setOnTouchListener(onSwipeListener);
		        
		final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final RelativeLayout rl = (RelativeLayout) inflater.inflate(R.layout.layout_standard_text, null);
		final TextView textView = (TextView) rl.findViewById(R.id.tvStandard);
		final JrFile file = getItem(position);
		
        textView.setMarqueeRepeatLimit(1);
        textView.setText(rl.getContext().getText(R.string.lbl_loading));
        GetFileValueTask.getFileValue(position, file, (ListView)parent, textView);
        
        final OnNowPlayingStartListener checkIfIsPlayingFileListener = new OnNowPlayingStartListener() {
			
			@Override
			public void onNowPlayingStart(JrPlaylistController controller, JrFilePlayer filePlayer) {
				textView.setTypeface(null, filePlayer.getFile().getKey() == file.getKey() ? Typeface.BOLD : Typeface.NORMAL);
			}
		};
		
		rl.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
			
			@Override
			public void onViewDetachedFromWindow(View v) {
				StreamingMusicService.removeOnStreamingStartListener(checkIfIsPlayingFileListener);
			}
			
			@Override
			public void onViewAttachedToWindow(View v) {
				if (StreamingMusicService.getPlaylistController() == null)
					StreamingMusicService.resumeSavedPlaylist(v.getContext());
				
				final JrPlaylistController playlistController = StreamingMusicService.getPlaylistController();
		        if (playlistController != null && playlistController.getCurrentFilePlayer() != null && playlistController.getCurrentFilePlayer().getFile().getKey() == file.getKey())
		        	textView.setTypeface(null, Typeface.BOLD);
		        
				StreamingMusicService.addOnStreamingStartListener(checkIfIsPlayingFileListener);
			}
		});
        
		parentView.addView(rl);
		
		final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_file_item_menu, null);
        fileMenu.setOnTouchListener(onSwipeListener);
        
        final ImageButton addButton = (ImageButton)fileMenu.findViewById(R.id.btnAddToPlaylist);
        addButton.setOnClickListener(new AddClickHandler(file));
        addButton.setOnTouchListener(onSwipeListener);
        
        final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlaySong);
        playButton.setOnClickListener(new PlayClickHandler(position, mFiles));
        playButton.setOnTouchListener(onSwipeListener);
        
        final ImageButton viewFileDetailsButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFileDetails);
        viewFileDetailsButton.setOnClickListener(new ViewFileDetailsClickHandler(file));
        viewFileDetailsButton.setOnTouchListener(onSwipeListener);
		
		parentView.addView(fileMenu);
        
		return parentView;
	}
	
	private static class PlayClickHandler implements OnClickListener {
		private List<JrFile> mFiles;
		private int mPosition;
		
		public PlayClickHandler(int position, List<JrFile> files) {
			mPosition = position;
			mFiles = files;
		}
		
		@Override
		public void onClick(View v) {
			StreamingMusicService.streamMusic(v.getContext(), mPosition, JrFiles.serializeFileStringList(mFiles));
		}
	}
	
	private static class AddClickHandler implements OnClickListener {
		private JrFile mFile;
		
		public AddClickHandler(JrFile file) {
			mFile = file;
		}
		
		@Override
		public void onClick(View v) {
			if (StreamingMusicService.getPlaylistController() == null) 
				StreamingMusicService.resumeSavedPlaylist(v.getContext());
			StreamingMusicService.getPlaylistController().addFile(mFile);
			String newFileString = JrSession.GetLibrary(v.getContext()).getSavedTracksString();
			if (!newFileString.endsWith(";")) newFileString += ";";
			newFileString += mFile.getKey() + ";";
			JrSession.GetLibrary(v.getContext()).setSavedTracksString(newFileString);
			
			Toast.makeText(v.getContext(), v.getContext().getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show();;
		}
	}
	
	private static class ViewFileDetailsClickHandler implements OnClickListener {
		private JrFile mFile;
		
		public ViewFileDetailsClickHandler(JrFile file) {
			mFile = file;
		}
		
		@Override
		public void onClick(View v) {
    		Intent intent = new Intent(v.getContext(), ViewFileDetails.class);
    		intent.putExtra(ViewFileDetails.FILE_KEY, mFile.getKey());
    		v.getContext().startActivity(intent);
		}
	}
	
	private static class GetFileValueTask extends AsyncTask<String, Void, String> {
		private int mPosition;
		private ListView mParentListView;
		private TextView mTextView;
		private JrFile mFile;
		
		public static GetFileValueTask getFileValue(int position, JrFile file, ListView parentListView, TextView textView) {
			return (GetFileValueTask) (new GetFileValueTask(position, file, parentListView, textView)).execute();
		}
		
		private GetFileValueTask(int position, JrFile file, ListView parentListView, TextView textView) {
			mPosition = position;
			mParentListView = parentListView;
			mFile = file;
			mTextView = textView;
		}

		@Override
		protected String doInBackground(String... params) {
			if ((mPosition < mParentListView.getFirstVisiblePosition() - 10) || (mPosition > mParentListView.getLastVisiblePosition() + 10)) return null;
			return mFile.getValue();
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (result == null) return;
			
			mTextView.setText(result);
		}
	}
}