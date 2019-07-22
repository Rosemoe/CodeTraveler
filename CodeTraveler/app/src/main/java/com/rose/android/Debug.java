package com.rose.android;
import android.content.Context;
import android.widget.Toast;

public class Debug
{
	
	private static Context context;
	
	public static void attachContext(Context c){
		context = c.getApplicationContext();
	}
	
	public static void debug(CharSequence s){
		Toast.makeText(context,s,0).show();
	}
	
}
