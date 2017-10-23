package psn.filechief.log4j;

import org.apache.log4j.spi.LoggingEvent;

public class Result 
{
	private int action;
	private LoggingEvent[] events;

	public Result(int a) {
		action = a; 
		events = null;
	}

	public Result(int a, LoggingEvent[] e) {
		action = a; 
		events = e;
	}

	public int getAction() {
		return action;
	}

	public LoggingEvent[] getEvents() {
		return events;
	}

	public void setAction(int action) {
		this.action = action;
	}

	public void setEvents(LoggingEvent[] events) {
		this.events = events;
	}
	
}
