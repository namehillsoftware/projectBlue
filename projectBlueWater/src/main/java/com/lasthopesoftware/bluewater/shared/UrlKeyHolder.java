package com.lasthopesoftware.bluewater.shared;

/**
 * Created by david on 1/17/16.
 */
public class UrlKeyHolder<T> {
	private final String url;
	private final T key;

	public UrlKeyHolder(String url, T key) {
		this.url = url;
		this.key = key;
	}

	@Override
	public boolean equals(Object o) {
		UrlKeyHolder<T> otherUrlKeyHolder;
		try {
			otherUrlKeyHolder = (UrlKeyHolder<T>) o;
		} catch (ClassCastException e) {
			return false;
		}

		return otherUrlKeyHolder != null && key.equals(otherUrlKeyHolder.key) && url.equals(otherUrlKeyHolder.url);
	}
}
