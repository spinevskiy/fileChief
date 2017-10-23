package psn.filechief.log4j;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

public class Evaluator implements TriggeringEventEvaluator 
{
	public static final String SEND_NOW = "sendNow";
	public static final String SEND_YES = "sendYes";
	public static final String SEND_NO = "sendNo";
	public static final String AS_THREAD_NAME = "asThreadName";
	public static final String MULTI = "...";

	public boolean isTriggeringEvent(LoggingEvent event) 
	{
		if(event.getLevel().equals(Level.FATAL))
			return true;
		String sp = event.getProperty(SEND_NOW);
		if(sp==null || sp.equals(SEND_YES))
			return true;
	//	if(event.getLevel().isGreaterOrEqual(Priority.ERROR) && mes.contains("CRITICAL")) 
	//		return true;
	//	String mes = event.getRenderedMessage(); //.getMessage()
	//	if(event.getLevel().isGreaterOrEqual(Priority.ERROR) && mes.contains("CRITICAL")) 
	//		return true;
	//	if(event.getLevel().isGreaterOrEqual(Priority.WARN))
	//			if( mes.contains("Started, locked port") || mes.contains("Stop signal !")) 
	//				return true;
		return false;
	}

}
