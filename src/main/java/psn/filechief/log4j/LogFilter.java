package psn.filechief.log4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class LogFilter extends Filter 
{
	private String denyPattern = null;
	private Matcher denyMatcher = null;
	private String allowPattern = null;
	private Matcher allowMatcher = null;
	private Level allowLevel = Level.WARN;

	@Override
	public int decide(LoggingEvent event) 
	{
		if(!event.getLevel().isGreaterOrEqual(allowLevel) ) 
			return Filter.DENY;
		
		String mes = event.getRenderedMessage();
		if(denyMatcher!=null) {
			denyMatcher.reset(mes);
			if(denyMatcher.find())
				return Filter.DENY;
		}	
		if(allowMatcher==null)
			return Filter.NEUTRAL;
		
		allowMatcher.reset(mes);
		if(allowMatcher.find())
			return Filter.ACCEPT;
		return Filter.DENY;
	}

	public String getDenyPattern() {
		return denyPattern;
	}

	public void setDenyPattern(String pattern) 
	{
		this.denyPattern = pattern;
		denyMatcher = Pattern.compile(pattern).matcher("");
	}

	public String getAllowPattern() {
		return allowPattern;
	}

	public void setAllowPattern(String pattern) 
	{
		this.allowPattern = pattern;
		allowMatcher = Pattern.compile(pattern).matcher("");
	}
	
	public void setMinLogLevel(String level)
	{
		Level x = Level.toLevel(level);
		if(x.equals(Level.DEBUG))
			x = Level.WARN;
		allowLevel = x;
	}
}
