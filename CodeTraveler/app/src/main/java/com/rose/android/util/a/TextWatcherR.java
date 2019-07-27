package com.rose.android.util.a;
import android.text.Editable;

//Created by Rose on 2019/7/25

//A interface of WatcherTransformer
public interface TextWatcherR
{
	//It is to notify you that
	//the text {s} has deleted text
	//{textToDelete} at {index}
	void onDelete(Editable s,int index,CharSequence textToDelete);
	
	//It is to notify you that
	//the text {s} has inserted text
	//{textToInsert} at {index}
	void onInsert(Editable s,int index,CharSequence textToInsert);
	
	//It is to note that
	//the following two calls
	//are part of replace
	//and they will have the same {index}
	void onReplace(Editable s);
	
}
