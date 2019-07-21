package com.rose.android.util.a;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.Spannable;
import java.util.List;
import java.util.ArrayList;

public class WatcherTransformer implements TextWatcher{
	
	private Editable target;
	private List<TextWatcherR> listeners;
	private int i,j;
	
	public WatcherTransformer(Editable text){
		target = text;
		target.setSpan(this,0,target.length(),Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		listeners = new ArrayList<TextWatcherR>();
	}

	public void add(TextWatcherR watcher){
		if(listeners.contains(watcher) || listeners == null){
			return;
		}
		listeners.add(watcher);
	}
	
	public void remove(TextWatcherR watcher){
		listeners.remove(watcher);
	}
	
	private void dispatchInsert(int i,CharSequence s){
		for(TextWatcherR w : listeners){
			w.onInsert(target,i,s);
		}
	}
	
	private void dispatchDelete(int i,CharSequence s){
		for(TextWatcherR w : listeners){
			w.onDelete(target,i,s);
		}
	}
	
	private void dispatchReplace(){
		for(TextWatcherR w : listeners){
			w.onReplace(target);
		}
	}
	
	@Override
	public void beforeTextChanged(CharSequence s, int index, int count, int j) {
		if(count != 0){
			if(j == 0){
				dispatchDelete(index,s.subSequence(index,index + count));
			}else{
				dispatchReplace();
			}
		}else{
			i=index;this.j=j;
		}
	}

	@Override
	public void onTextChanged(CharSequence s, int index, int len, int beforeCount){
		if(j != 0)
			dispatchInsert(i,s.subSequence(i,i+j));
	}

	@Override
	public void afterTextChanged(Editable s) {
		//no action
	}
	
}
