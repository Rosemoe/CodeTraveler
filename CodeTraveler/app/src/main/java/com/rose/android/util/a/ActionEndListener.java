package com.rose.android.util.a;

//Created By Rose on 2019/7/25

//Simple interface of WatcherTransformer
public interface ActionEndListener
{
	//When the afterTextChanged() in 
	//TextWatcher is called by its host
	//This method will be called by WatcherTransformer
	void onEnd();
}
