package de.tnr.sdg.example.cache;

public class MainCache {

	private Cache cache;
	private Cache cache2 = new Cache();
	
	public MainCache(){
	}
	
	/*
	 * Test if operator != is recognized
	 */
	public void createCache(){
		//System.out.println("Initialising Cache.");
		if (cache == null) {
			this.cache = new Cache();
		} else {
		}
	}
	
	public void clearCache(){
		//System.out.println("Calling clearCache().");
		if (cache != null) {
			cache.reset();
		} else {					
		}
	}
	
	public void cacheFile(String filename){
		//System.out.println("Calling cacheFile("+filename+").");
		if (cache != null) {
			cache.put(filename);
		} else {
		}
	}
	
	/*
	 * Test if operand must be null
	 */
	public boolean checkDifference() {
		if (cache==cache2) {
			cache.reset();
			return true;
		} else {
			return false;
		}
	}
	
	public static void main(String[] args) {
		MainCache myCache = new MainCache();
		
		myCache.cacheFile("I'm a File!");
		myCache.clearCache();
		myCache.createCache();
		myCache.cacheFile("Cooler File");
		myCache.clearCache();
	}
}