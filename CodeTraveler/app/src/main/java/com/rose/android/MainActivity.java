package com.rose.android;

import android.app.*;
import android.os.*;
import com.rose.android.widget.CodeEditor;
import android.widget.FrameLayout;
public class MainActivity extends Activity 
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		CodeEditor e = new CodeEditor(this);
		e.setLayoutParams(new FrameLayout.LayoutParams(-1,-1));
        setContentView(e);
    }
}
