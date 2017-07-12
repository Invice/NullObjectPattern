package de.tnr.sdg.example.ifError;

import java.util.List;

public class Error {
	List<String> list;
	
	public static void main(String[] args) {
		Error myError = new Error();
		
		if(myError.list != null){
			System.out.println("false");
		}
	}
}
