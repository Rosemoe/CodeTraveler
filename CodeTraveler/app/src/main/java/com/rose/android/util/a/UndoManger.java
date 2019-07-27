package com.rose.android.util.a;

import java.util.ArrayList;
import android.text.Editable;

//Created By Rose on 2019/7/21

//Helper class of EditorText
//It saves the undo/redo information for user
public class UndoManger implements TextWatcherR {

	//Default stack size
	public final static int DEFAULT_MAX_SIZE = 100;

	//Next action flag
	private boolean replaceFlag = false;
	
	//We are undoing/redoing
	//and we must ignore the changes
	private boolean ignoreChange=false;
	
	//whether we are in batch edit
	private boolean batchEdit;
	
	//the stack
	private ArrayList<Action> stack;
	
	//current position.in stack
	private int curr;
	
	//cached deleted text
	private CharSequence cachedText;

	//max stack size
	private int max_size;
	
	//UndoManager enabled
	private boolean enabled;

	public UndoManger(){
		this(DEFAULT_MAX_SIZE);
	}

	public UndoManger(int max_size){
		stack = new ArrayList<Action>();
		this.max_size = max_size;
		this.curr = -1;
		this.batchEdit = false;
		this.cachedText = null;
		this.ignoreChange = false;
		this.replaceFlag = false;
		this.enabled = true;
	}

	public void setEnabled(boolean enabled){
		if(enabled && enabled != this.enabled){
			clearStack();
		}else if(!enabled){
			this.enabled = enabled;
		}
	}

	public boolean isEnabled(){
		return enabled;
	}

	@Override
	public void onInsert(Editable doc, int index, CharSequence textToInsert) {
		if (ignoreChange||!enabled) {
			return;
		}
		if (replaceFlag) {
			stack.add(new ReplaceAction(index, cachedText, textToInsert));
			replaceFlag = false;
		} else {
			add(new InsertAction(index, textToInsert));
		}
	}

	@Override
	public void onDelete(Editable doc, int index, CharSequence textDeleted) {
		if (ignoreChange||!enabled) {
			return;
		}
		if (replaceFlag) {
			cachedText = textDeleted;
		} else {
			add(new DeleteAction(index, textDeleted));
		}
	}

	@Override
	public void onReplace(Editable doc) {
		if (ignoreChange||!enabled) {
			return;
		}
		replaceFlag = true;
	}

	public void setBatchEdit(boolean batchEdit){
		this.batchEdit = batchEdit;
	}

	public boolean isBatchEdit(){
		return this.batchEdit;
	}
	
	private void cleanStackBeforeAdd() {
		//Remove the undo/redo info after index
		//or it will be wrong to add action to the end
		while (canRedo()) {
			stack.remove(stack.size() - 1);
		}
	}

	private void cleanStackAfterAdd(){
		//We should clean the stack after change
		//So that we will not more than the given max size
		while(stack.size() > max_size && curr > 0){
			stack.remove(0);
			curr--;
		}
	}

	//make UndoManager empty
	public void clearStack() {
		curr = -1;
		stack.clear();
		ignoreChange = false;
		replaceFlag = false;
	}

	private void add(Action action){
		cleanStackBeforeAdd();

		//in batch edit we should consider all the actions as one action
		if(batchEdit){
			if(curr==-1||!(stack.get(curr) instanceof MultiAction)){
				stack.add(new MultiAction());
				curr++;
				((MultiAction)stack.get(curr)).addAction(action);
			}else{
				MultiAction parent = (MultiAction)stack.get(curr);
				if(parent.getLastAction()!=null&&parent.getLastAction().canMerge(action)){
					parent.getLastAction().merge(action);
				}else{
					parent.addAction(action);
				}
			}
			return;
		}

		if(curr!=-1&&stack.get(curr).canMerge(action)){
			stack.get(curr).merge(action);
		}else{
			stack.add(action);
			curr++;
		}

		cleanStackAfterAdd();
	}

	public boolean canUndo() {
		return curr > -1;
	}

	public boolean canRedo() {
		return curr < stack.size() - 1;
	}

	public void undo(Editable doc) {
		if (canUndo()) {
			ignoreChange = true;
			stack.get
			(curr).undo(doc);
			ignoreChange = false;
			curr--;
		}
	}

	public void redo(Editable doc) {
		if (canRedo()) {
			ignoreChange = true;
			stack.get(curr + 1).redo(doc);
			ignoreChange = false;
			curr++;
		}
	}

	private interface Action {

		void undo(Editable doc);

		void redo(Editable doc);
		
		//As the users will type texts usually one by one
		//We should merge the same actions so that it will not
		//make too many actions
		boolean canMerge(Action action);

		void merge(Action action);

	}

	private static class DeleteAction implements Action {

		public int index;
		public CharSequence text;

		public DeleteAction(int i, CharSequence j) {
			index = i;
			text = j;
		}

		@Override
		public void undo(Editable doc) {
			doc.insert(index, text);
		}

		@Override
		public void redo(Editable doc) {
			doc.delete(index, text.length());
		}

		@Override
		public boolean canMerge(Action action) {
			if (action instanceof DeleteAction) {
				DeleteAction target = (DeleteAction) action;
				if (target.index == this.index - text.length()) {
					return true;
				}
				return false;
			} else {
				return false;
			}
		}

		@Override
		public void merge(Action action) {
			if (canMerge(action)) {
				DeleteAction target = (DeleteAction) action;
				this.index = target.index;
				this.text = target.text.toString() + this.text.toString();
			} else {
				throw new IllegalArgumentException("target action not supported");
			}
		}

	}

	private static class InsertAction implements Action {

		public int index;
		public CharSequence text;

		public InsertAction(int i, CharSequence j) {
			index = i;
			text = j;
		}

		@Override
		public void redo(Editable doc) {
			doc.insert(index, text);
		}

		@Override
		public void undo(Editable doc) {
			doc.delete(index, text.length());
		}

		@Override
		public boolean canMerge(Action action) {
			if (action instanceof InsertAction) {
				InsertAction target = (InsertAction) action;
				if(target.index == this.index + text.length()){
					return true;
				}else{
					return false;
				}
			} else {
				return false;
			}
		}

		@Override
		public void merge(Action action) {
			if(canMerge(action)){
				this.text = this.text.toString() + ((InsertAction)action).text.toString();
			}else{
				throw new IllegalArgumentException("target action not supported");
			}
		}

	}

	private static class ReplaceAction implements Action {

		public int index;
		public CharSequence ori;
		public CharSequence nw;

		public ReplaceAction(int i, CharSequence j, CharSequence k) {
			index = i;
			ori = j;
			nw = k;
		}

		@Override
		public void undo(Editable doc) {
			doc.delete(index, nw.length());
			doc.insert(index, ori);
		}

		@Override
		public void redo(Editable doc) {
			doc.delete(index, ori.length());
			doc.insert(index, nw);
		}

		@Override
		public boolean canMerge(UndoManger.Action action) {
			return false;
		}

		@Override
		public void merge(UndoManger.Action action) {
			throw new IllegalArgumentException("action not supported");
		}

	}

	private static class MultiAction implements Action {

		private ArrayList<Action> actions = new ArrayList<Action>();

		public void addAction(Action action) {
			if (action != null) {
				actions.add(action);
			}
		}

		public Action getLastAction(){
			if(actions.size()==0){
				return null;
			}else{
				return actions.get(actions.size()-1);
			}
		}

		@Override
		public void redo(Editable doc) {
			for(int i=actions.size()-1;i>-1;i--){
				actions.get(i).redo(doc);
			}
		}

		@Override
		public void undo(Editable doc) {
			for(int i=actions.size()-1;i>-1;i--){
				actions.get(i).undo(doc);
			}
		}

		@Override
		public boolean canMerge(UndoManger.Action action) {
			return false;
		}

		@Override
		public void merge(UndoManger.Action action) {
			throw new IllegalArgumentException("action not supported");
		}

	}

}
