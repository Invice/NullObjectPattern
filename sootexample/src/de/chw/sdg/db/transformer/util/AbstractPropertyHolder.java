package de.chw.sdg.db.transformer.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class AbstractPropertyHolder {

	private final Properties properties;

	protected AbstractPropertyHolder(String filename) {
		properties = new Properties();
		try (InputStream inputStream = AbstractPropertyHolder.class.getResourceAsStream("/" + filename)) {
			properties.load(inputStream);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	protected Properties getProperties() {
		return properties;
	}
}
