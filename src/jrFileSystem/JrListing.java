package jrFileSystem;

public abstract class JrListing {
	private Integer mKey;
	private String mValue;
	
	public JrListing(int key, String value) {
		this.setKey(key);
		this.setValue(value);
	}
	
	public JrListing(String value) {
		this.setKey(null);
		this.setValue(value);
	}
	
	public JrListing() {
	}
	
	/**
	 * @return the mKey
	 */
	public Integer getKey() {
		return mKey;
	}

	/**
	 * @param mKey the mKey to set
	 */
	public void setKey(Integer mKey) {
		this.mKey = mKey;
	}

	public abstract String getUrl();

	/**
	 * @return the mValue
	 */
	public String getValue() {
		return mValue;
	}

	/**
	 * @param mValue the mValue to set
	 */
	public void setValue(String mValue) {
		this.mValue = mValue;
	}
}
