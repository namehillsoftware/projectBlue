package jrFileSystem;

public abstract class JrListing {
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
	
	public abstract String getUrl();
}
