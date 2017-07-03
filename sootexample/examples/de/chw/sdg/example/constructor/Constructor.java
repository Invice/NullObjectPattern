package de.chw.sdg.example.constructor;

public class Constructor {

	private final int param;
	private final float value;

	public Constructor() {
		super();
		this.param = 0;
		this.value = 0;
	}

	public Constructor(final int param) throws Exception {
		this(param, 0);
	}

	public Constructor(final int param, final float value) {
		this.param = param;
		this.value = value;
	}


	public int getParam() {
		return param;
	}

	public float getValue() {
		return value;
	}

	public static void main(final String[] args) {
		Constructor c0 = new Constructor();
		c0.getParam();
		Constructor c1 = new Constructor(3, 7.4f);
		c1.getValue();
		Constructor c2 = new Constructor(Integer.valueOf(args[0]), 7.4f);
		c2.getValue();
	}

}
