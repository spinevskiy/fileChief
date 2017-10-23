package psn.filechief.util;

import java.util.concurrent.atomic.AtomicBoolean;

public class SmartThread extends Thread 
{
	protected AtomicBoolean flag;
	
	public void setStopFlag(AtomicBoolean flag) {
		this.flag = flag;
	}
	
	public SmartThread(AtomicBoolean flag) {
		this.flag = flag;
	}

}
