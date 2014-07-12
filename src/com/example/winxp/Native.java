package com.example.winxp;

public class Native {
	
	public native static boolean init();
	public native static String getMagic();
	public native static boolean nukeXposed(String classname);

	static {
		System.loadLibrary("facebook");
	}
}
