package com.rose.android.util.a;
import android.text.Editable;
import java.util.List;
import java.util.ArrayList;
import com.rose.android.Debug;

//Created By Rose on 2019/7/21

//This is a helper class of EditorText
//LineManager provides the index of lines
//It works by making caches and updating these
//cache data when the text changes
//So when you are trying to get a index of a
//line that is far from cached lines,
//it can spend you much time.
public class LineManager implements TextWatcherR ,ActionEndListener{

	//The line count of text
	private int lineCount = 1;
	
	//Our serve target
	private Editable serveTarget;
	
	//The cached lines
	private List<Pair> pairs;
	
	//The garbage caches
	//In order not to create new instance when text updates
	private List<Pair> garbages;
	
	//The last line of text
	//In order to find index backward and make it more quick
	private Pair endPoint;
	
	//The first line of text
	private Pair zeroPoint;
	
	//Max capcity of cache
	//We will clean the pairs wheb we add pair to it
	private int max_capcity;
	
	//If the nearest distance of the requesting line to
	//the cached line is more than it,we will
	//add the line to the cache
	private int add_switch;
	
	protected LineManager(Editable s){
		serveTarget = s;
		pairs = new ArrayList<Pair>();
		garbages = new ArrayList<Pair>();
		zeroPoint = new Pair();
		//Update when create.
		//beacause the onInsert() call from EditorText
		//will not cause this
		updateEndPoint();
		max_capcity = 50;
		add_switch = 30;
	}
	
	//Update the position of last line
	private void updateEndPoint(){
		if(endPoint == null){
			endPoint = new Pair();
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

	//add a pair to the cache
	private void addPair(int line, int index) {
		pairs.add(new Pair(line, index));
		//clean the pairs
		while (pairs.size() > max_capcity) {
			pairs.remove(0);
		}
	}

	//Get the cache which has the nearest distance by line index
	//Please use getNearsetPairWrapped()
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

		//No cache or too far
		//Just return the zeroPoint
		if(bestIndex != -1){
			Pair pair = pairs.get(bestIndex);
			pairs.remove(bestIndex);
			pairs.add(pair);
			return pair;
		}

		return zeroPoint;
	}

	//find index forward
	private int findForward(Pair pair,int count){
		int line = pair.first;
		int index = pair.second;
		for(int i = 0; i < count;i++,line++){
			index =indexOf(serveTarget,(line==0?0:index+1),'\n');
			if(index == -1){
				return index;
			}
		}
		return index;
	}

	//find index backward
	private int findBackward(Pair pair,int count){
		int line = pair.first;
		int index = pair.second;
		for(int i = 0;i < count;i++,line--){
			index = lastIndexOf(serveTarget,index-1,'\n');
		}
		return index;
	}

	//CharSequence indexOf without index check
	private int indexOf(CharSequence s,int start,char c){
		for(int i = start;i < s.length();i++){
			if(s.charAt(i)==c){
				return i;
			}
		}
		return -1;
	}

	//CharSequence lastIndexOf without index check
	private int lastIndexOf(CharSequence s,int start,char c){
		for(int i=start;i>0;i--) {
			if(s.charAt(i)==c){
				return i;
			}
		}
		return -1;
	}

	//Get line count
	public int getLineCount(){
		return lineCount;
	}

	//Get line start
	//Unless the first line,we just return
	//where the '\n' is.So you might need to
	//make a simple self-increase after the action
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
			if(absDistance >= add_switch){
				addPair(line,index);
			}
			return index;
		}
	}

	//Get line end
	//Do not need to use subSequence(start,end+1)
	public int getLineEnd(int line) {
		if(line < 0 || line >= lineCount){
			throw new IndexOutOfBoundsException("line index out of bounds");
		}
		if(line == getLineCount() - 1){
			return serveTarget.length();
		}
		return getLineStart(line + 1);
	}

	//Get the cache line which has the nearest 
	//char distance to the given line
	//Please use getNearestPairByIndexWrapped()
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

		//The zeroPoint is the nearest
		if(i != -1){
			pairs.remove(pair);
			pairs.add(pair);
		}

		return pair;
	}
	
	//Wrapped
	//It can advise the end point to you
	private Pair getNearestPairByIndexWrapped(int index){
		Pair pair = getNearestPairByIndex(index);
		int d = Math.abs(pair.second - index);
		int td = Math.abs(endPoint.second - index);
		return (d <= td) ? pair : endPoint;
	}

	//Wrapped
	//It can advise the end point to you
	private Pair getNearestPairWrapped(int line){
		Pair pair = getNearestPair(line);
		int d = Math.abs(pair.first - line);
		int td = Math.abs(endPoint.first - line);
		return (d <= td) ? pair : endPoint;
	}

	//Get the line of the given index
	//It means that (start<=index&&index<end)
	//But there is a little bug
	//Please see CodeEditor's code to get the solution
	public int getLineByIndex(int index){
		if(index == serveTarget.length()){
			return lineCount - 1;
		}
		if(index < 0 || index > serveTarget.length()){
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

	//find char offset backward
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

	//find char offset forward
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
	public void onInsert(Editable doc, int index, CharSequence textToInsert) {
		int dL;
		lineCount += ( dL=getNewLineTokenCount(textToInsert) );
		//update pairs
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
	public void onDelete(Editable doc, int index, CharSequence textDeleted) {
		int dL;
		lineCount -= ( dL = getNewLineTokenCount(textDeleted) );
		//update pairs
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
					//try to fix a bug...
					//If you think it is meanless you can delete them
					if(lastIndexOf(serveTarget,index-1,'\n')==-1){
						//this is zero point!
						//we can not remove it now due not to have the ConcurrentModificationException
						garbages.add(pair);
						continue;
					}
					
					int nI = findBackward(new Pair(pair.first,index),1);

					//reached start of document,it is zeroPoint
					//add it to garbage
					if(nI == -1 || pair.first<=0)
						garbages.add(pair);

					pair.second = nI;
				}catch(Exception e){
					//error
					System.out.println(e);
					//unable to modify it correctly
					//so we remove it from our list
					garbages.add(pair);
				}
			}
		}
		
		//remove all the garbages
		pairs.removeAll(garbages);
		garbages.clear();
	}

	@Override
	public void onReplace(Editable doc) {
		//do nothing.
		//the duty is onDelete and onInsert's
	}

	@Override
	public void onEnd() {
		//If we updated at other time(onInsert() and onDelete())
		//the text index will be wrong
		updateEndPoint();
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
	
	//Data saver
	private static class Pair{
		
		//line index
		public int first;
		
		//char offset(the first character after the index is the line start unless it is zero point)
		public int second;
		
		//create a zero point
		public Pair(){
			this(0,0);
		}
		
		//create a line with index
		public Pair(int line,int index){
			this.first = line;
			this.second = index;
		}
		
	}
	
}
