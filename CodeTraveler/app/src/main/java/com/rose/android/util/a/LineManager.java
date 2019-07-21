package com.rose.android.util.a;
import android.text.Editable;

public class LineManager implements TextWatcherR {

	private int lineCount = 1;
	private Editable str;
	
	protected LineManager(Editable s){
		str = s;
	}
	
	public int getLineCount(){
		return lineCount;
	}
	
	public int getLineStart(int line){
		return 0;
	}
	
	public int getLineEnd(int line){
		return str.length();
	}
	
	public int getLineByIndex(int cahIndex){
		return 0;
	}
	
	public static int getTargetCharacterCount(CharSequence s,int start,int end,char c){
		int nl = 0;
		for(int i = start;i < end;i++){
			if(s.charAt(i) == c){
				nl++;
			}
		}
		return nl;
	}
	
	@Override
	public void onDelete(Editable s, int index, CharSequence textToDelete) {
		lineCount -= getTargetCharacterCount(textToDelete,0,textToDelete.length(),'\n');
	}

	@Override
	public void onInsert(Editable s, int index, CharSequence textToInsert) {
		lineCount += getTargetCharacterCount(textToInsert,0,textToInsert.length(),'\n');
	}

	@Override
	public void onReplace(Editable s) {
		//do nothing
	}

	
}
