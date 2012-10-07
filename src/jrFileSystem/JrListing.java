package jrFileSystem;

public class JrListing {
	public Integer mKey;
	public String mValue;
	
	public JrListing(int key, String value) {
		this.mKey = key;
		this.mValue = value;
	}
	
	public JrListing(String value) {
		this.mKey = null;
		this.mValue = value;
	}
	
	public JrListing() {
	}
	
	
}
