package com.rose.android.util;

import java.util.ArrayList;
import android.text.Editable;
import android.text.InputFilter;
import java.lang.reflect.Array;

//Created By Rose on 2019/7/17
//DEPRECATED!
//Many bugs in this file.

//EditorText begin
//Ability:
//  A simple but useful variable String.
//  It allows you to midify the text content by its methods,
//  so that you do not need to create a lot of String objects;
//
//  It also allows you to make Undo/Redo actions,
//  so that you can recover your document easily.
//  Note that we have a limit for the Undo/Redo stack.
//  If you want to have a bigger size for the stack
//  Please:
//     yourDocument.setUndoStackMaxSize(sizeYouPrefer);
//  
//  What is nicer is that you can get line start or end position
//  conveniently by calling getLineStart(int line) and getLineEnd(int line)
//  It works with cache so that it can work more quickly
//  It is more suit for EditorView in Android device
//  And it is also the raw target of it
//  Also,you can call getLineCount() to get how many lines there is
//
//  :(
public class EditorText implements CharSequence,Appendable,Cloneable,Editable {
	public final static int DEFAULT_CAPCITY = 100;

	//Start:Instance Fileds------------------------

	private int capcity;
	private int length;
	private char[] chars;

	private UndoStack undoStack;
	private LineIndexProvider lineIndexs;
	private ArrayList<DocumentChangeListener> listeners;

	private int batchEdit;
	private boolean editFlag;

	//End:Instance Field-------------------------
	//Start:Constructor--------------------------

	//create a null instance
	public EditorText() {
		this(DEFAULT_CAPCITY);
	}

	//create a new instance with the given capcity
	public EditorText(int capcity) {
		if (capcity <= 0) {
			throw new IllegalArgumentException("capcity under or equal zero");
		}
		this.capcity = capcity;
		this.length = 0;
		chars = new char[capcity];
		commonInit();
	}

	//create with the text
	public EditorText(CharSequence source) {
		this(source.length() > 0 ? source.length() : DEFAULT_CAPCITY);
		//not a real user edition action,we cancel it
		undoStack.ignoreChange = true;
		append(source);
		undoStack.ignoreChange = false;
	}

	//create with characters
	public EditorText(char[] source) {
		this(source, 0, source.length);
	}

	//create instance with characters [offset,offset+length)
	public EditorText(char[] source, int offset, int length) {
		this(source, offset, length, DEFAULT_CAPCITY);
	}

	//create instance with characters [offser,offset+length),and set the capcity
	public EditorText(char[] source, int offset, int length, int capcity) {
		if (capcity <= 0) {
			throw new IllegalArgumentException("capcity under or equal zero");
		}
		if (length < 0) {
			throw new IllegalArgumentException("invalid length");
		}
		this.capcity = capcity;
		int size = length > capcity ? length : capcity;
		chars = new char[size];
		for (int i = offset,j=0;i < offset + length;j++,i++) {
			chars[j] = source[i];
		}
		this.length = length;
		commonInit();
	}

	private void commonInit() {
		listeners = new ArrayList<DocumentChangeListener>();
		undoStack = new UndoStack();
		lineIndexs = new LineIndexProvider(this);
		batchEdit = 0;
		editFlag = false;
	}

	//End:Constructor-----------------------------
	//Start:Checkers------------------------------

	//check whether the given index is in our region
	//if not,we will throw a exception
	private void checkIndex(int index) {
		if (index < 0 || index >= length()) {
			throw new StringIndexOutOfBoundsException("index out of bounds:index=" + index + " length=" + length);
		}
	}

	//some illegal calls will be denied
	//such call some methods to modify document when the document is working
	private void checkState(){
		if(!isAvailable()){
			throw new IllegalStateException("this document is working.it is a illegal call to modify it");
		}
	}

	public boolean isAvailable(){
		return !editFlag;
	}

	//End:Checkers------------------------------
	//Start:Replace-----------------------------

	//replace region [index,end) to {text}
	public EditorText replace(int start, int end, CharSequence text) {
		checkState();
		dispatchBeforeReplace();
		delete(start, end - start);
		insert(start, text);
		return this;
	}

	//st=textStart,ed=textEnd
	public EditorText replace(int start, int end, CharSequence text, int st, int ed) {
		return replace(start, end, text.subSequence(st, ed));
	}

	//End:Replace------------------------------
	//Start:Delete-----------------------------

	//delete text with region [index,index+length)
	public EditorText delete(int index, int length) {
		if(length == 0){
			editFlag = true;
			dispatchBeforeDelete(0,"");
			editFlag = false;
			return this;
		}
		checkIndex(index);
		checkIndex(index + length - 1);
		checkState();
		if (length < 0) {
			throw new IllegalArgumentException("length under zero");
		}
		editFlag = true;
		dispatchBeforeDelete(index, this.subSequence(index, index + length));
		for (int i = index;i < this.length - length;i++) {
			chars[i] = chars[i + length];
		}
		this.length -= length;
		editFlag = false;
		return this;
	}

	//End:Delete--------------------------------
	//Start:Insert------------------------------

	//insert at {index} with {text} [start,end)
	public EditorText insert(int index, CharSequence text, int start, int end) {
		return insert(index, text.subSequence(start, end));
	}

	//insert at {index} with {text}
	public EditorText insert(int index, CharSequence text) {
		checkState();
		editFlag = true;
		if(index != 0)
			checkIndex(index-1);
		text = ((text == null) ? "null" : text);
		int len = text.length();
		enlargeSizeIfNeed(len);
		dispatchBeforeInsert(index, text);
		for (int i = length - 1;i >= index;i--) {
			chars[i + len] = chars[i];
		}
		for (int i = index;i < index + len;i++) {
			chars[i] = text.charAt(len - (index + len - i));
		}
		length += len;
		editFlag = false;
		return this;
	}

	//End:Insert----------------------------
	//Start:Append--------------------------

	public EditorText append(char[] chars) {
		return append(new String(chars));
	}

	public EditorText append(char[] chars, int st, int len) {
		return append(new String(chars, st, st + len));
	}

	public EditorText append(char c) {
		return append(Character.toString(c));
	}

	public EditorText append(CharSequence text, int st, int ed) {
		return append(text.subSequence(st, ed));
	}

	public EditorText append(CharSequence text) {
		return insert(length, text);
	}

	//End:Append-----------------------------------
	//Start:Undo/Redo------------------------------

	//They will be complete by class UndoStack

	public boolean canUndo() {
		return undoStack.canUndo();
	}

	public boolean canRedo() {
		return undoStack.canRedo();
	}

	public boolean undo() {
		return undoStack.undo(this);
	}

	public boolean redo() {
		return undoStack.redo(this);
	}

	public void setUndoEnabled(boolean enabled){
		undoStack.setEnabled(enabled);
	}

	public boolean isUndoEnabled(){
		return undoStack.isEnabled();
	}

	//This is not a standard method.
	//We actually do not have the interface.for users.
	//But you can also call it if you want.
	public void setUndoStackMaxSize(int size){
		if(size <= 0){
			throw new IllegalArgumentException();
		}
		undoStack.max_size = size;
	}


	//End:Undo/Redo------------------------------
	//Start:Enlarge------------------------------

	//make a new array that may larger than current array
	//so that we can insert (or replace) freely and correctly
	private void enlargeSizeIfNeed(int len) {
		if (chars.length - length < len) {
			len = len - (chars.length - length);
			int newSize = chars.length + (len > capcity ? len : capcity);
			char[] newArray = new char[newSize];
			for (int i = 0;i < length;i++) {
				newArray[i] = chars[i];
			}
			chars = newArray;
		}
	}

	//End:Enlarge------------------------------
	//Start:Listener---------------------------

	//add the given DocumentChangeListener to listeners
	//if there is the listener in listeners,we will not add it to the listeners again
	public void addDocumentChangeListener(DocumentChangeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("can not add null to listeners");
		}
		if (listeners.indexOf(listener) == -1) {
			listeners.add(listener);
		}
	}

	//remive the given DocumentChangeListener 
	public void removeDocumentChangeListener(DocumentChangeListener listener) {
		listeners.remove(listener);
	}

	//remove all the DocumentChangeListener added
	public void removeAllDocumentListeners() {
		listeners.clear();
	}

	//dispatch event to listeners {onInsert}
	private void dispatchBeforeInsert(int index, CharSequence text) {
		undoStack.onInsert(this, index, text);
		lineIndexs.onInsert(this, index, text);
		for (DocumentChangeListener lis : listeners) {
			lis.onInsert(this, index, text);
		}
	}

	//dispatch event to listeners {onDelete}
	private void dispatchBeforeDelete(int index, CharSequence text) {
		undoStack.onDelete(this, index, text);
		lineIndexs.onDelete(this, index, text);
		for (DocumentChangeListener lis : listeners) {
			lis.onDelete(this, index, text);
		}
	}

	//dispatch event to listeners {onReplace}
	private void dispatchBeforeReplace() {
		undoStack.onReplace(this);
		lineIndexs.onReplace(this);
		for (DocumentChangeListener lis : listeners) {
			lis.onReplace(this);
		}
	}

	//End:Listener-----------------------------
	//Start:Internal Calls---------------------

	//These methods will be called by EditorView
	//They are about InputConnection

	public void beginBatchEdit(){
		batchEdit++;
		undoStack.setBatchEdit(true);
	}

	public void endBatchEdit(){
		batchEdit--;
		if(batchEdit < 0){
			batchEdit = 0;
		}
		undoStack.setBatchEdit(batchEdit > 0);
	}

	public boolean isInBatchEdit(){
		return (batchEdit>0);
	}

	//End:Internal Calls-----------------------
	//Start:Line Info--------------------------

	//These methods are due to provider easy getter for views

	public int getLineCount(){
		return lineIndexs.getLineCount();
	}

	public int getLineStart(int line){
		return lineIndexs.getLineStart(line);
	}

	public int getLineEnd(int line){
		return lineIndexs.getLineEnd(line);
	}

	public int getLineByIndex(int index){
		return -1;
	}

	//End:Line Info----------------------------
	//Start:Override---------------------------

	//get the length of text
	@Override
	public int length() {
		return this.length;
	}

	//get the character at {index}
	@Override
	public char charAt(int index) {
		checkIndex(index);
		return chars[index];
	}

	//get the subSequence of this Document from {begin} to {end}
	@Override
	public CharSequence subSequence(int begin, int end) {
		checkIndex(begin);
		if(end != 0)
			checkIndex(end - 1);
		if(begin > end){
			throw new IllegalArgumentException("begin > end!");
		}
		return new EditorText(chars, begin, end - begin, capcity);
	}

	@Override
	public String toString() {
		//create String by character array
		return new String(chars, 0, length);
	}

	@Override
	public EditorText clone() {
		//clone object ourselves
		return (EditorText)this.subSequence(0, length());
	}

	@Override
	public void getChars(int start, int end, char[] dest, int offset) {
		checkIndex(start);
		checkIndex(end - 1);
		for(int i = start,j=offset;i < end;i++,j++){
			dest[j] = chars[i];
		}
	}

	@Override
	public void clear() {
		dispatchBeforeDelete(0,subSequence(0,length()));
		length = 0;
	}

	//the following methods we do not support
	//we might support them one day

	@Override
	public InputFilter[] getFilters(){
		return new InputFilter[0];
	}

	@Override
	public int getSpanStart(Object span){
		return 0;
	}

	@Override
	public int getSpanEnd(Object span){
		return 0;
	}

	@Override
	public void removeSpan(Object span){
		//do nothing
	}

	@Override
	public void clearSpans(){
		//do nothing
	}

	@Override
	public void setSpan(Object span,int start,int end,int mode){
		//do nothing
	}

	@Override
	public <T extends Object> T[] getSpans(int start, int end, Class<T> type) {
		return (T[])Array.newInstance(type,0);
	}

	@Override
	public int getSpanFlags(Object span) {
		return 0;
	}

	@Override
	public void setFilters(InputFilter[] p1) {
		//do nothing
	}

	@Override
	public int nextSpanTransition(int p1, int p2, Class p3) {
		return 0;
	}

	//End:Override---------------------------------
	//Start:Interface------------------------------

	// a simple document change listener for {Document}
	public interface DocumentChangeListener {

		//Note: never change document by using object{doc} when these methods are called
		//it is only readable!

		//it will be called when we insert {textToInsert} to {index} in {doc}
		void onInsert(EditorText doc, int index, CharSequence textToInsert);

		//it will be called when we delete from {index} and we deleted {textDeleted}
		void onDelete(EditorText doc, int index, CharSequence textDeleted);

		//when we try to replace text,
		//the onReplace will be called,
		//and then onDelete,
		//and finally onInsert.
		//we replace in this way to simplify our code
		//:)
		//it will be called before we call methods {Document#delete} and {Document#insert}
		void onReplace(EditorText doc);

	}

	//End:Interface---------------------------------------
	//Start:Internal Classes------------------------------


	//UndoStack begin
	//Responsibility:To collect undo/redo information and make undo/redo actions
	public static class UndoStack implements DocumentChangeListener {

		//Start:UndoStack:Fields-----------------------------

		private final static int DEFAULT_MAX_SIZE = 100;

		//is in replacement
		private boolean replaceFlag = false;

		//if we should cancel changes
		private boolean ignoreChange=false;

		//if we should merge actions all types
		private boolean batchEdit;

		//int value for batch edit

		//stack for ...
		private ArrayList<Action> stack;

		//current undo action to do index
		private int curr;

		//text submited by {onDelete} when in a replacement
		private CharSequence cachedText;

		//max save count
		private int max_size;

		//if the undo/redo enabled
		private boolean enabled;

		//End:UndoStack:Fields------------------------------
		//Constructors here---------------------------------

		public UndoStack(){
			this(DEFAULT_MAX_SIZE);
		}

		public UndoStack(int max_size){
			stack = new ArrayList<Action>();
			this.max_size = max_size;
			this.curr = -1;
			this.batchEdit = false;
			this.cachedText = null;
			this.ignoreChange = false;
			this.replaceFlag = false;
			this.enabled = true;
		}

		//enabled or disenabled here-------------------------

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

		//Start:UndoStack:Listener-----------------------------

		@Override
		public void onInsert(EditorText doc, int index, CharSequence textToInsert) {
			if (ignoreChange||!enabled) {
				//ignore changes
				return;
			}
			if (replaceFlag) {
				//add ReplaceAction to stack
				stack.add(new ReplaceAction(index, cachedText, textToInsert));
				//replace exit
				replaceFlag = false;
			} else {
				//add InsertAction to stack
				add(new InsertAction(index, textToInsert));
			}
		}

		@Override
		public void onDelete(EditorText doc, int index, CharSequence textDeleted) {
			if (ignoreChange||!enabled) {
				//ignore changes
				return;
			}
			if (replaceFlag) {
				//take down textdeleted
				cachedText = textDeleted;
			} else {
				//add DeleteAction to stack
				add(new DeleteAction(index, textDeleted));
			}
		}

		@Override
		public void onReplace(EditorText doc) {
			if (ignoreChange||!enabled) {
				//ignore changes
				return;
			}
			//set replace flag true
			replaceFlag = true;
		}

		//End:UndoStack:Listener------------------------------------
		//Start:UndoStack:Stack Actions-----------------------------

		public void setBatchEdit(boolean batchEdit){
			this.batchEdit = batchEdit;
		}

		public boolean isBatchEdit(){
			return this.batchEdit;
		}

		//clean the stack from tail
		//so that the action can be added properly
		private void cleanStackBeforeAdd() {
			//when the user have already called the {redo()}
			//and now the user is trying to change the text which is undone,
			//we should clean the action after the current index
			while (canRedo()) {
				stack.remove(stack.size() - 1);
			}
		}

		//clean the stack from head
		//so that we will not save too much actions
		private void cleanStackAfterAdd(){
			while(stack.size() > max_size && curr > 0){
				stack.remove(0);
				curr--;
			}
		}

		//clear(reset) the UndoStack instance
		public void clearStack() {
			curr = -1;
			stack.clear();
			ignoreChange = false;
			replaceFlag = false;
		}

		//internal add a Action to stack
		private void add(Action action){
			cleanStackBeforeAdd();

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

		//End:UndoStack:Stack Actions------------------------------------------
		//Start:UndoStack:Real interface for users-----------------------------

		//whether the text can be undone
		public boolean canUndo() {
			return curr > -1;
		}

		//wheter the text can be redone
		public boolean canRedo() {
			return curr < stack.size() - 1;
		}

		//undo the text if available
		//return if the action is successfully done
		public boolean undo(EditorText doc) {
			if (canUndo()) {
				ignoreChange = true;
				stack.get
				(curr).undo(doc);
				ignoreChange = false;
				curr--;
				return true;
			}
			return false;
		}

		//redo the text if available
		//return if the action is successfully done
		public boolean redo(EditorText doc) {
			if (canRedo()) {
				ignoreChange = true;
				stack.get(curr + 1).redo(doc);
				ignoreChange = false;
				curr++;
				return true;
			}
			return false;
		}

		//End:UndoStack:Real interface for users-----------------------------
		//Start:UndoStack:Internal interface---------------------------------

		//universal interface for undo/redo action
		private interface Action {

			//undo action on {doc}
			void undo(EditorText doc);

			//redo action on {doc}
			void redo(EditorText doc);

			//whether can merge with {action}
			boolean canMerge(Action action);

			//merge with {action} if we can
			//otherwise,we throw a {IllegalArgumentException}
			void merge(Action action);

		}

		//End:UndoStack:Internal interface-----------------------------
		//Start:UndoStack:Interface Impl-------------------------------

		//the following classes are the implementations of Action

		//DeleteAction begin
		private static class DeleteAction implements Action {

			public int index;
			public CharSequence text;

			public DeleteAction(int i, CharSequence j) {
				index = i;
				text = j;
			}

			@Override
			public void undo(EditorText doc) {
				doc.insert(index, text);
			}

			@Override
			public void redo(EditorText doc) {
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

		}//DeleteAction end

		//InsertAction begin
		private static class InsertAction implements Action {

			public int index;
			public CharSequence text;

			public InsertAction(int i, CharSequence j) {
				index = i;
				text = j;
			}

			@Override
			public void redo(EditorText doc) {
				doc.insert(index, text);
			}

			@Override
			public void undo(EditorText doc) {
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

		}//InsertAction end

		//ReplaceAction begin
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
			public void undo(EditorText doc) {
				doc.delete(index, nw.length());
				doc.insert(index, ori);
			}

			@Override
			public void redo(EditorText doc) {
				doc.delete(index, ori.length());
				doc.insert(index, nw);
			}

			//Replace do not support merge

			@Override
			public boolean canMerge(EditorText.UndoStack.Action action) {
				return false;
			}

			@Override
			public void merge(EditorText.UndoStack.Action action) {
				throw new IllegalArgumentException("action not supported");
			}

		}//ReplaceAction end

		//MultiAction begin
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
			public void redo(EditorText doc) {
				for(int i=actions.size()-1;i>-1;i--){
					actions.get(i).redo(doc);
				}
			}

			@Override
			public void undo(EditorText doc) {
				for(int i=actions.size()-1;i>-1;i--){
					actions.get(i).undo(doc);
				}
			}

			//MultiAction do not support merge

			@Override
			public boolean canMerge(EditorText.UndoStack.Action action) {
				return false;
			}

			@Override
			public void merge(EditorText.UndoStack.Action action) {
				throw new IllegalArgumentException("action not supported");
			}

		}//MultiAction end

		//End:UndoStack:Interface Impl-----------------------------

	}//UndoStack eend

	//LineIndexProvider begin
	//Responsibility:manage line information such as line start , line end and line count.
	private static class LineIndexProvider implements DocumentChangeListener {

		public final static int DEFAULT_CAPCITY = 10;
		public final static int CHANGE_LIMIT = 10;
		public final static int ADD_LINE = 100;

		private Pair zeroPoint;
		private Pair endPoint;
		private ArrayList<Pair> pairs;
		private int lineCount;
		private int max_capcity;
		private EditorText serveTarget;

		public LineIndexProvider(EditorText doc){
			serveTarget = doc;
			zeroPoint = new Pair(0,0);
			pairs = new ArrayList<Pair>();
			max_capcity = DEFAULT_CAPCITY;
			lineCount = 1;
			if(doc.length() != 0)
				this.onInsert(doc,0,doc);
		}

		private void updateEndPoint(){
			if(endPoint == null){
				endPoint = new Pair(0,0);
			}
			endPoint.first = lineCount - 1;
			int i = lastIndexOf(serveTarget,serveTarget.length()-1,'\n');
			if(i == -1){
				//there is only one line
				endPoint.first = endPoint.second = 0;
			}else{
				endPoint.second = i;
			}
		}

		private void addPair(int line, int index) {
			if(true){
				return;
			}
			pairs.add(new Pair(line, index));
			while (pairs.size() > max_capcity) {
				pairs.remove(0);
			}
		}

		private Pair getNearestPair(int line){
			int bestIndex = -1;
			int bestDistance = line;
			for(int i = pairs.size()-1;i > -1;i--){
				Pair pair = pairs.get(i);
				int newDistance = Math.abs(pair.first - line);
				if(newDistance < bestDistance){
					bestDistance = newDistance;
					bestIndex = i;
				}
			}

			if(bestIndex != -1){
				Pair pair = pairs.get(bestIndex);
				pairs.remove(bestIndex);
				pairs.add(pair);
				return pair;
			}

			return zeroPoint;
		}

		private int findForward(Pair pair,int count){
			int line = pair.first;
			int index = pair.second;
			CharSequence s = serveTarget;
			for(int i = 0; i < count;i++,line++){
				index =indexOf(s,(line==0?0:index+1),'\n');
				if(index == -1){
					throw new IndexOutOfBoundsException("too large count");
				}
			}
			return index;
		}

		private int findBackward(Pair pair,int count){
			int line = pair.first;
			int index = pair.second;
			CharSequence s = serveTarget;
			for(int i = 0;i < count;i++,line--){
				index = lastIndexOf(s,index-1,'\n');
			}
			return index;
		}

		private int indexOf(CharSequence s,int start,char c){
			for(int i = start;i < s.length();i++) if(s.charAt(i)==c) return i;
			return -1;
		}

		private int lastIndexOf(CharSequence s,int start,char c){
			for(int i=start;i>0;i--) if(s.charAt(i)==c) return i;
			return -1;
		}

		public int getLineCount(){
			return lineCount;
		}

		public int getLineStart(int line) {
			if (line == 0) {
				return 0;
			}
			if(line < 0 || line >= lineCount){
				throw new IndexOutOfBoundsException("line index out of bounds");
			}
			Pair pair = getNearestPairWrapped(line);
			int distance = line - pair.first;
			if(distance == 0){
				return pair.second;
			}else{
				int absDistance = Math.abs(distance);
				int index = distance < 0 ? findBackward(pair,absDistance) : findForward(pair,absDistance);
				if(absDistance >= 1){
					addPair(line,index);
				}
				return index;
			}
		}

		public int getLineEnd(int line) {
			if(line < 0 || line >= lineCount){
				throw new IndexOutOfBoundsException("line index out of bounds");
			}
			if(line == getLineCount() - 1){
				return serveTarget.length();
			}
			return getLineStart(line + 1);
		}

		private Pair getNearestPairByIndex(int index){
			int i = -1;
			int d = index;
			for(int j = 0;j < pairs.size();j++){
				Pair pair = pairs.get(j);
				int nd = Math.abs(index - pair.second);
				if(nd < d){
					i = j;
					d = nd;
				}
			}
			Pair pair = (i == -1) ? zeroPoint : pairs.get(i);

			if(i != -1){
				pairs.remove(pair);
				pairs.add(pair);
			}

			return pair;
		}

		private Pair getNearestPairByIndexWrapped(int index){
			Pair pair = getNearestPairByIndex(index);
			int d = Math.abs(pair.second - index);
			int td = Math.abs(endPoint.second - index);
			return (d <= td) ? pair : endPoint;
		}

		private Pair getNearestPairWrapped(int line){
			Pair pair = getNearestPair(line);
			int d = Math.abs(pair.first - line);
			int td = Math.abs(endPoint.first - line);
			return (d <= td) ? pair : endPoint;
		}

		public int getLineByIndex(int index){
			if(index < 0 || index >= serveTarget.length()){
				throw new StringIndexOutOfBoundsException("index out of bounds");
			}
			Pair pair = getNearestPairByIndexWrapped(index);
			int dis = pair.second - index;
			if(dis == 0){
				return pair.first;
			}else{
				int line = (dis<0)?findUtil_Forward(pair,index):findUtil_Backward(pair,index);
				return line;
			}
		}
		
		private int findUtil_Backward(Pair pair,int index){
			int line = pair.first;
			int i = pair.second;
			Pair cache = new Pair(0,0);
			while(i > index){
				cache.first = line;
				cache.second = i;
				i = findBackward(cache,1);
				if(i == -1){
					return 0;
				}
				line--;
			}
			return line;
		}
		
		private int findUtil_Forward(Pair pair,int index){
			int line = pair.first;
			int i = pair.second;
			Pair cache = new Pair(0,0);
			while(i < index){
				cache.first = line;
				cache.second = i;
				i = findForward(cache,1);
				if(i == -1){
					return lineCount - 1;
				}
				line++;
			}
			return line -1;
		}

		@Override
		public void onInsert(EditorText doc, int index, CharSequence textToInsert) {
			int dL;
			lineCount += ( dL=getNewLineTokenCount(textToInsert) );
			updateEndPoint();
			for(Pair pair : pairs){
				if(index > pair.second){
					//no effect
				}else{
					pair.first += dL;
					pair.second += textToInsert.length();
				}
			}
		}

		@Override
		public void onDelete(EditorText doc, int index, CharSequence textDeleted) {
			int dL;
			lineCount -= ( dL = getNewLineTokenCount(textDeleted) );
			updateEndPoint();
			for(Pair pair : pairs){
				if(index > pair.second){
					//no effect
				}else if(index+textDeleted.length() < pair.second){
					pair.first -= dL;
					pair.second -= textDeleted.length();
				}else{
					int xDl = getNewLineTokenCount(doc.subSequence(index,pair.second+1));
					pair.first -= xDl;
					try{
						if(lastIndexOf(serveTarget,index-1,'\n')==-1){
							pairs.remove(pair);
							continue;
						}
						int nI = findBackward(new Pair(pair.first,index),1);

						//reached start of document,it is zeroPosint
						if(nI == -1 || pair.first<=0)
							pairs.remove(pair);

						//System.out.println("Pair:first="+pair.first+" second="+pair.second);

						pair.second = nI;
					}catch(Exception e){
						//error
						System.out.println(e);
						pairs.remove(pair);
					}
				}
			}
		}

		@Override
		public void onReplace(EditorText doc) {
			//do nothing.
			//the duty is onDelete and onInsert's
		}

		//Get how many '\n' there are in target CharSequence object
		private int getNewLineTokenCount(CharSequence s){
			if(s==null){
				return 0;
			}
			int c = 0;
			for(int i = 0;i < s.length();i++){
				if(s.charAt(i)=='\n'){
					c++;
				}
			}
			return c;
		}

		//Our data saver
		private static class Pair {

			//Simple Constructor
			public Pair(int f, int s) {
				first = f;second = s;
			}

			//line number
			public int first;

			//line start index
			public int second;

		}

	}//LineIndexProvider end

	//End:Internal Classes------------------------------

}//EditorText end
