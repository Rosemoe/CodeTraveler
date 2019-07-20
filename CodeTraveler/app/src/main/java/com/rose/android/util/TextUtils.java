package com.rose.android.util;

//Created By Rose on 2019/7/20

public class TextUtils{

	private TextUtils(){
		//Static Utils Class
	}

	public static int csIndexOf(CharSequence target,char c){
		return csIndexOf(target,0,c);
	}

	public static int csIndexOf(CharSequence target,int index,char c){
		if(index >= target.length() && target.length() != 0 && index != 0){
			throw new StringIndexOutOfBoundsException("index out of bounds");
		}
		for(int i = index;i < target.length();i++){
			if(target.charAt(i) == c){
				return i;
			}
		}
		return -1;
	}

	public static int csLastIndexOf(CharSequence target,char c){
		return csLastIndexOf(target,0,c);
	}

	public static int csLastIndexOf(CharSequence target,int index,char c){
		if(index >= target.length()){
			throw new StringIndexOutOfBoundsException("index out of bounds");
		}
		for(int i = index;i <= 0;i--){
			if(target.charAt(i) == c){
				return i;
			}
		}
		return -1;
	}

}
