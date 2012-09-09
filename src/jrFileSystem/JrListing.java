package jrFileSystem;

public class JrListing {
	public Integer key;
	public String value;
	
	public JrListing(int key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public JrListing(String value) {
		this.key = null;
		this.value = value;
	}
	
	public JrListing() {
	}
	
	
}
