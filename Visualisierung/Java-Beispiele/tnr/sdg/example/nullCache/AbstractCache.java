package de.tnr.sdg.example.nullCache;

public abstract class AbstractCache {
	
	public abstract void put(String filename);
	public abstract void reset();
	public abstract int size();
}
