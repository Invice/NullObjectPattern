package de.tnr.sdg.example.nullCache;

public class NullCache extends AbstractCache {

	@Override
	public void put(String filename) {
	}

	@Override
	public void reset() {
	}

	@Override
	public int size() {
		return 0;
	}

}
