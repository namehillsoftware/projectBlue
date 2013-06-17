package jrFileSystem;

import java.util.ArrayList;
import jrAccess.JrPlaylistResponse;
import jrAccess.JrSession;

public class JrPlaylists extends JrListing implements IJrItem<JrPlaylist> {
	private ArrayList<JrPlaylist> mSubItems;
	
	public JrPlaylists(int key) {
		setKey(key);
		setValue("Playlist");
	}
	
	@Override
	public ArrayList<JrPlaylist> getSubItems() {
		if (mSubItems != null) return mSubItems;
		
		mSubItems = new ArrayList<JrPlaylist>();
		if (JrSession.accessDao == null) return mSubItems;
		try {
			mSubItems = (new JrPlaylistResponse()).execute( "Playlists/List" ).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mSubItems;
	}
	
	@Override
	public ArrayList<JrFile> getFiles() {
		return null;
	}

	@Override
	public String getSubItemUrl() {
		return JrSession.accessDao.getJrUrl("Playlists/List");
	}

	@Override
	public ArrayList<JrFile> getFiles(int option) {
		// TODO Auto-generated method stub
		return null;
	}
}
