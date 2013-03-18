package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.JrFileXmlResponse;
import jrAccess.JrSession;

public class JrPlaylist extends JrListing implements IJrItem {
	private ArrayList<JrFile> mFiles;
	
	@Override
	public ArrayList<JrItem> getSubItems() {
		return null;
	}
	
	@Override
	public ArrayList<JrFile> getFiles() {
		if (mFiles != null) return mFiles;
		
		mFiles = new ArrayList<JrFile>();
		try {
			List<JrFile> tempFiles = (new JrFileXmlResponse()).execute("Playlist/Files", "ID=" + String.valueOf(this.mKey)).get(); 
			mFiles.addAll(tempFiles);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mFiles;
	}

	@Override
	public String getUrl() {
		return JrSession.accessDao.getJrUrl("Playlist/Files", "Playlist=" + String.valueOf(this.mKey));
	}
}
