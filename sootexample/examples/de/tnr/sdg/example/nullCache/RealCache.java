package de.tnr.sdg.example.nullCache;

public class RealCache extends AbstractCache {

	@Override
	public void put(String filename) {
		size();				
	}

	@Override
	public void reset() {
		size();
	}

	@Override
	public int size() {
		return 0;
	}

}
