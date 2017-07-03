package de.chw.sdg.db.transformer.util;

public class FilterUtil extends AbstractPropertyHolder {

	private static final String CONF_FILENAME = "conf/filter.properties";

	public static final FilterUtil INSTANCE = new FilterUtil();

	private FilterUtil() {
		super(CONF_FILENAME);
		// utility class
	}

	public String getRegex(final String key) {
		String value = getProperties().getProperty(key, key);
		return value;
	}
}
