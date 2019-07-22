package com.rose.android;

import android.app.*;
import android.os.*;
import com.rose.android.widget.CodeEditor;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.FileOutputStream;
public class MainActivity extends Activity 
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		
		Debug.attachContext(this);
		
		CodeEditor e = new CodeEditor(this);
		e.setLayoutParams(new FrameLayout.LayoutParams(-1,-1));
        setContentView(e);
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){

				@Override
				public void uncaughtException(Thread thread, final Throwable cause) {
					new Thread(){
						@Override
						public void run(){
							Looper.prepare();
							StringBuilder sb = new StringBuilder();
							sb.append(cause.toString()).append("\n");
							for(Object o : cause.getStackTrace()){
								sb.append("    at ").append(o.toString()).append("\n");
							}
							
							try{
								FileOutputStream fos = new FileOutputStream("/sdcard/log.txt");
								fos.write(sb.toString().getBytes());
								fos.flush();
								fos.close();
							}catch(Exception e){
								
							}
							
							Debug.debug("Crash!");
							Looper.loop();
						}
					}.start();
				}

			
		});
    }
}
