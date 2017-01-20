package com.lasthopesoftware.bluewater.shared;

/**
 * Created by david on 1/17/16.
 */
public class UrlKeyHolder<T> {
	private Integer hashCode;

	private final String url;
	private final T key;

	public UrlKeyHolder(String url, T key) {
		this.url = url;
		this.key = key;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final UrlKeyHolder<?> that = (UrlKeyHolder<?>) o;
		return !(url != null ? !url.equals(that.url) : that.url != null) && !(key != null ? !key.equals(that.key) : that.key != null);
	}

	@Override
	public int hashCode() {
		if (hashCode != null) return hashCode;

		int calculatedHashCode = url != null ? url.hashCode() : 0;
		calculatedHashCode = 31 * calculatedHashCode + (key != null ? key.hashCode() : 0);

		hashCode = calculatedHashCode;

		return calculatedHashCode;
	}
}
