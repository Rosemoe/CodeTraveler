package com.rose.android;

import android.app.*;
import android.os.*;
import com.rose.android.widget.CodeEditor;
import android.widget.FrameLayout;
import android.widget.Toast;
public class MainActivity extends Activity 
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		CodeEditor e = new CodeEditor(this);
		e.setLayoutParams(new FrameLayout.LayoutParams(-1,-1));
        setContentView(e);
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){

				@Override
				public void uncaughtException(Thread thread, final Throwable cause) {
					//Looper.prepare();
					//Looper.loop();
				//	Toast.makeText(MainActivity.this,cause.toString(),0).show();
					new Thread(){
						@Override
						public void run(){
							Looper.prepare();
							StringBuilder sb = new StringBuilder();
							sb.append(cause.toString()).append("\n");
							for(Object o : cause.getStackTrace()){
								sb.append(o.toString()).append("\n");
							}
							
							Toast.makeText(MainActivity.this,sb,Toast.LENGTH_SHORT).show();
							Looper.loop();
							//android.os.Process.killProcess(android.os.Process.myPid());
						}
					}.start();
				}

			
		});
    }
}
