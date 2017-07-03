package de.tnr.sdg.example.cache;

public class MainCache {

	private Cache cache;
	
	public MainCache(){
	}
	
	public void createCache(){
		//System.out.println("Initialising Cache.");
		this.cache = new Cache();
	}
	
	public void clearCache(){
		//System.out.println("Calling clearCache().");
		if (cache != null) {
			cache.reset();
		} else {					
		}
	}
	
//	public void cacheFile(String filename){
//		//System.out.println("Calling cacheFile("+filename+").");
//		if (cache != null) {
//			cache.put(filename);
//		} else {
//		}
//	}
//	
	public static void main(String[] args) {
		MainCache myCache = new MainCache();
		
//		myCache.cacheFile("I'm a File!");
		myCache.clearCache();
		myCache.createCache();
//		myCache.cacheFile("Cooler File");
		myCache.clearCache();
	}
}