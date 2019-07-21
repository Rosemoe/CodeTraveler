package com.rose.android.util.a;
import android.text.Editable;

public interface TextWatcherR
{
	
	void onDelete(Editable s,int index,CharSequence textToDelete);
	
	void onInsert(Editable s,int index,CharSequence textToInsert);
	
	void onReplace(Editable s);
	
}
