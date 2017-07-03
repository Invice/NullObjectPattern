package de.tnr.sdg.example.nullCache;


public class MainClass {	

	private AbstractCache cache = new NullCache();
	
	public MainClass(){
		
	}
	
	public void createCache(){
//		System.out.println("Initialising Cache.");
		this.cache = new RealCache();
	}
	
//	public void cacheFile(String filename){
//		System.out.println("Calling cacheFile("+filename+").");
//		cache.put(filename);
//	}
	
	public void clearCache(){
//		System.out.println("Calling clearCache().");
		cache.reset();
	}
	
	
	public static void main(String[] args) {
		MainClass myMain = new MainClass();
		
//		myMain.cacheFile("I'm a File!");
		myMain.clearCache();
		myMain.createCache();
//		myMain.cacheFile("Cooler File");
		myMain.clearCache();
	}
}
