package com.rose.android.util.a;
import android.text.Editable;
import android.text.Selection;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import com.rose.android.Debug;
import com.rose.android.widget.CodeEditor;

//Created By Rose on 2019/7/26

//Helper class of CodeEditor
//This is the connection bwtween editor and input method
//We should implement the method getEditable() of it
//And other work will most be completed by its parnet
//BaseInputConnection
public class EditorInputConnection extends BaseInputConnection{

	//the batch edit nested count
	private int nestedBE;
	
	//serve target view
	private CodeEditor ce;

	public EditorInputConnection(CodeEditor ce){
		//We are a full editor and do not want to enable
		//dummy mode(I do not know how it works and something
		//unexpected will happen when you press key ENTER)
		//So we pass a 'true' to the parent
		super(ce,true);
		this.ce = ce;
		nestedBE = 0;
	}

	@Override
	public Editable getEditable() {
		//Return our editable to parent
		//So that parent can help us do some input work
		return ce.isEditable() && ce.isEnabled() ? ce.getEditableText() : null;
	}

	@Override
	public boolean sendKeyEvent(KeyEvent event) {
		//The key event from input method
		//We only handle the event when the key down
		//So that we will not do the same action twice
		if(event.getAction() == event.ACTION_DOWN){
			switch(event.getKeyCode()){
				case KeyEvent.KEYCODE_DEL:
					//Delete Key
					deleteSurroundingText(1,0);
					return true;
				case KeyEvent.KEYCODE_ENTER:
					//Insert a '\n'
					commitText("\n",1);
					return true;
				case KeyEvent.KEYCODE_SHIFT_LEFT:
				case KeyEvent.KEYCODE_SHIFT_RIGHT:
					//TODO
					//The input method want to select text
					//We should also handle the event which contains "DPAD" in its name
					return true;
				default:
					//Unknown key event
					//If debug is enabled,we will make a tip
					Debug.debug(event.keyCodeToString(event.getKeyCode()));
			}
		}
		return super.sendKeyEvent(event);
	}

	@Override
	public boolean beginBatchEdit() {
		//Called by input method to begin
		//Parent class has a empty impl
		ce.getEditableText().beginBatchEdit();
		nestedBE++;
		return isBatchEdit();
	}

	//Getter for editor
	public boolean isBatchEdit(){
		return nestedBE > 0;
	}

	@Override
	public boolean endBatchEdit() {
		//Called by input method to end
		//Parent class has empty impl
		ce.getEditableText().endBatchEdit();
		nestedBE--;
		//According to the document,we might
		//receive some meanless endBatchEdit() calls
		//when the input method is exiting
		//do not let it under zero
		if(nestedBE<0){
			nestedBE = 0;
		}else if(nestedBE == 0){
			//batch edit finished
			//notify the input method at once
			ce.notifySelChange();
		}
		return isBatchEdit();
	}

	@Override
	public boolean setSelection(int start, int end) {
		EditorText text = ce.getEditableText();
		//This mostly happens when the user input (),{},or[]
		//Because we did not send the correct positions of selections
		//the input method will send wrong setSelection() calls
		//We will try to solve it future
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
