package de.chw.sdg.db.transformer.util;

class PropertyKeyUtil extends AbstractPropertyHolder {

	private static final String CONF_FILENAME = "conf/keys.properties";

	public static final PropertyKeyUtil INSTANCE = new PropertyKeyUtil();

	private PropertyKeyUtil() {
		super(CONF_FILENAME);
	}

	public String getPropertyKey(String key) {
		return getProperties().getProperty(key, key);
	}
}
