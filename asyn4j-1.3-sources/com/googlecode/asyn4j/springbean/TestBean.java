package com.googlecode.asyn4j.springbean;

import java.util.ArrayList;
import java.util.List;

public class TestBean implements java.io.Serializable{

	public void test() {
		System.out.println("test excute");
	}

	public void myName(String name) {
		System.out.println(name+(1+100));
	}

	public void myName(String fn, String la) {
		System.out.println(fn + "  " + la);
	}

	public String myName(String fn, String la, int a) {
		return fn + "  " + la + "  age :" + a;
	}

	public void myName(ArrayList list) {
		if (list != null) {
			System.out.println(list.size());
		}

	}

}
