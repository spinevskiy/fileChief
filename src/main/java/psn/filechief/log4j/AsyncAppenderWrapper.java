package psn.filechief.log4j;

//import java.util.Enumeration;
import org.apache.log4j.Appender;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Logger;

public class AsyncAppenderWrapper extends AsyncAppender 
{
	String loggerName; 
	

	public void setAppenders(String aNames)
	{
		String[] names = aNames.split(",");
		for(String name : names)
			setAppender(name);
	}
	
	public void setAppender(String aName)
	{
		aName = aName.trim();
		if(aName.length()==0)
			return;
		Logger root = Logger.getRootLogger();
		Appender a = root.getAppender(aName);
		if(a==null)
			return;
		root.removeAppender(a);
		addAppender(a);
		//System.out.println("Added appender: "+a.getName() +", attach status: "+this.isAttached(a));
	}
	
/*	
	public void setLogger(String name)
	{
		Logger l = Logger.getLogger(name);
		Enumeration<Appender> e = l.getAllAppenders();
		while(e.hasMoreElements())
		{
			Appender a = e.nextElement();
			this.addAppender(a);
			System.out.println("The newAppender "+a.getName() +" attach status "+this.isAttached(a));
		}
	}
*/	
	
	 
}
