package com.rose.android.util.a;
import android.text.Editable;
import android.text.Selection;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import com.rose.android.Debug;
import com.rose.android.widget.CodeEditor;

public class EditorInputConnection extends BaseInputConnection{

	private int nestedBE;
	private CodeEditor ce;

	public EditorInputConnection(CodeEditor ce){
		super(ce,true);
		this.ce = ce;
		nestedBE = 0;
	}

	@Override
	public Editable getEditable() {
		return ce.isEditable() ? ce.getEditableText() : null;
	}

	@Override
	public boolean sendKeyEvent(KeyEvent event) {
		if(event.getAction() == event.ACTION_DOWN){
			switch(event.getKeyCode()){
				case KeyEvent.KEYCODE_DEL:
					deleteSurroundingText(1,0);
					return true;
				case KeyEvent.KEYCODE_ENTER:
					commitText("\n",1);
					return true;
				case KeyEvent.KEYCODE_SHIFT_LEFT:
				case KeyEvent.KEYCODE_SHIFT_RIGHT:
					
					return true;
				default:
					Debug.debug(event.keyCodeToString(event.getKeyCode()));
			}
		}
		return super.sendKeyEvent(event);
	}

	@Override
	public boolean beginBatchEdit() {
		ce.getEditableText().beginBatchEdit();
		nestedBE++;
		return isBatchEdit();
	}

	public boolean isBatchEdit(){
		return nestedBE > 0;
	}

	@Override
	public boolean endBatchEdit() {
		ce.getEditableText().endBatchEdit();
		nestedBE--;
		if(nestedBE<0){
			nestedBE = 0;
		}else if(nestedBE == 0){
			ce.notifySelChange();
		}
		return isBatchEdit();
	}

	@Override
	public boolean setSelection(int start, int end) {
		EditorText text = ce.getEditableText();
		if(start == end && start == 0){
			Selection.setSelection(text,Selection.getSelectionStart(text)-1);
			ce.notifySelChange();
			return true;
		}else{
			boolean r = super.setSelection(start,end);
			if(r){
				ce.notifySelChange();
			}
			return r;
		}
	}

}
