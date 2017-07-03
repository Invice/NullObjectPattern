package de.chw.sdg.db.transformer.util;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;

class LabelUtil extends AbstractPropertyHolder {

	private static final String CONF_FILENAME = "conf/labels.properties";

	public static final LabelUtil INSTANCE = new LabelUtil();

	private LabelUtil() {
		super(CONF_FILENAME);
	}

	public Label getLabel(String key) {
		String value = getProperties().getProperty(key, key);
		return DynamicLabel.label(value);
	}

}
