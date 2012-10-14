package jrFileSystem;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jrAccess.JrByteResponse;
import jrAccess.JrFileXmlResponse;
import jrAccess.JrStringResponse;
import jrAccess.JrStdXmlResponse;
import jrAccess.JrSession;

public class JrItem extends JrListing {
	public List<JrListing> mSubItems;
	
	public JrItem(int key, String value) {
		super(key, value);
		// TODO Auto-generated constructor stub
	}
	
//	public jrPage(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setCategories();
//	}
	
	public JrItem() {
		super();		
	}
	
	public List<JrListing> getSubItems() {
		
		if (mSubItems == null) {
			mSubItems = new ArrayList<JrListing>();
			if (JrSession.accessDao == null) return mSubItems;
			try {
				List<JrItem> tempSubItems = JrFileUtils.transformListing(JrItem.class, (new JrStdXmlResponse()).execute(new String[] { "Browse/Children", "ID=" + String.valueOf(this.mKey), "Skip=1" }).get().getItems());
				mSubItems.addAll(tempSubItems);
				
				if (mSubItems.isEmpty()) {
					List<JrFile> tempFiles = (new JrFileXmlResponse()).execute(new String[] { "Browse/Files", "ID=" + String.valueOf(this.mKey)/*, "Action=Serialize"*/}).get(); 
					mSubItems.addAll(tempFiles);
				}
				
				mSubItems = (List<JrListing>) (new JrFileUtils.SortJrListAsync().execute(new List[] { mSubItems }).get());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mSubItems;
	}
	
	private List<JrListing> parseFileList(String semiColonFileString) {
		List<JrListing> returnFiles = new ArrayList<JrListing>();
		String[] fileArray = semiColonFileString.split(";");
		
		for (int i = 0; i < fileArray.length; i++)
			returnFiles.add(new JrFile(Integer.parseInt(fileArray[i])));
		
		return returnFiles;
	}
}
