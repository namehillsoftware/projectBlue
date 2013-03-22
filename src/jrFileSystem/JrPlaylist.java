package jrFileSystem;

import android.annotation.SuppressLint;

import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import jrAccess.JrFileXmlResponse;
import jrAccess.JrSession;

@SuppressLint("UseSparseArrays")
public class JrPlaylist extends JrListing implements IJrItem<JrPlaylist> {
	private ArrayList<JrFile> mFiles;
	private HashMap<Integer, JrPlaylist> mSubItems;
	private String mPath;
	private String mGroup;
	
	public static int GET_SHUFFLED = 1;
	
	public JrPlaylist() {
		super();		
	}
	
	public JrPlaylist(int key) {
		super();
		setKey(key);
	}

	@Override
	public ArrayList<JrPlaylist> getSubItems() {
		if (mSubItems == null) mSubItems = new HashMap<Integer, JrPlaylist>();
		ArrayList<JrPlaylist> returnList = new ArrayList<JrPlaylist>(mSubItems.size());
		returnList.addAll(mSubItems.values());
		return returnList;
	}
	
	public void addPlaylist(JrPlaylist playlist) {
		if (mSubItems == null) mSubItems = new HashMap<Integer, JrPlaylist>();
		mSubItems.put(playlist.getKey(), playlist);
	}
	
	@Override
	public ArrayList<JrFile> getFiles() {
		if (mFiles != null) return mFiles;
		
		mFiles = new ArrayList<JrFile>();
		try {
			List<JrFile> tempFiles = (new JrFileXmlResponse()).execute("Playlist/Files", "Playlist=" + String.valueOf(this.getKey())).get(); 
			mFiles.addAll(tempFiles);
			for (JrFile file : mFiles) file.setSiblings(mFiles);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mFiles;
	}
	
	public ArrayList<JrFile> getFiles(int option) {
		ArrayList<JrFile> returnFiles = new ArrayList<JrFile>();
		returnFiles.addAll(getFiles());
		if (option == GET_SHUFFLED) Collections.shuffle(returnFiles, new Random(new Date().getTime()));
		
		return returnFiles;
	}

	/**
	 * @return the mPath
	 */
	public String getPath() {
		return mPath;
	}

	/**
	 * @param mPath the mPath to set
	 */
	public void setPath(String mPath) {
		this.mPath = mPath;
	}

	/**
	 * @return the mGroup
	 */
	public String getGroup() {
		return mGroup;
	}

	/**
	 * @param mGroup the mGroup to set
	 */
	public void setGroup(String mGroup) {
		this.mGroup = mGroup;
	}

	@Override
	public String getUrl() {
		return JrSession.accessDao.getJrUrl("Playlist/Files", "Playlist=" + String.valueOf(this.getKey()));
	}
}
