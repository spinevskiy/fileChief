package psn.filechief.util;

import java.util.ArrayList;

public class ThreadList implements IThreadList 
{
	private static final ThreadList instance = new ThreadList();
	private ArrayList<Thread> list;
	
	private ThreadList() {
		list = new ArrayList<>(10);
	}
	
	public void addThread(Thread t) {
		list.add(t);
	}
	
	public static IThreadList getInstance() {
		return instance;
	}
	
	public ArrayList<Thread> getList() {
		return list;
	}

}
