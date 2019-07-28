package com.rose.android;

import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import java.io.File;
import java.io.FileOutputStream;
import com.rose.android.widget.CodeEditor;
import android.widget.TextView;
public class MainActivity extends Activity 
{
	private CodeEditor ce;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		
        super.onCreate(savedInstanceState);
		
		Debug.attachContext(this);
		
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
								File nf = new File("/sdcard/log.txt");
								if(!nf.exists())
									nf.createNewFile();
								FileOutputStream fos = new FileOutputStream(nf);
								fos.write(sb.toString().getBytes());
								fos.flush();
								fos.close();
							}catch(Exception e){
								//Debug.debug(e.toString());
							}
							
							Debug.debug("Crash!");
							Looper.loop();
						}
					}.start();
				}

			
		});
		
		//CodeEditor e = new CodeEditor(this);
		//e.setLayoutParams(new FrameLayout.LayoutParams(-1,-1));
        setContentView(R.layout.main);
		ce = (CodeEditor) findViewById(R.id.editor);
    }
	
	public void insert(View view){
		switch(((TextView)view).getText().toString()){
			case "←":
				ce.moveSelectionLeft();
				break;
			case "→":
				ce.moveSelectionRight();
				break;
			case "↑":
				ce.moveSelectionUp();
				break;
			case "↓":
				ce.moveSelectionDown();
				break;
		}
	}
}
