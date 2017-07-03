package de.johl.examples;

public class Method2 {
	public static void main(String[] args) {
		Foo foo1 = new Foo();
		Foo foo2 = new Foo();
		Bar bar = new Bar();
		
		foo1.modify();
		foo1.modify();
		foo2.modify();
		bar.modify();
	}
	
	private static class Foo
	{
		@SuppressWarnings("unused")
		public int i = 0;
		
		public void modify()
		{
			i++;
		}
	}
	
	private static class Bar
	{
		@SuppressWarnings("unused")
		public float f = 0;
		
		public void modify()
		{
			f += 1;
		}		
	}
}
