package psn.filechief.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.apache.oro.text.perl.MalformedPerl5PatternException;
import org.apache.oro.text.perl.Perl5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubstituteWithTime 
{
	private static Logger log = LoggerFactory.getLogger(SubstituteWithTime.class.getName());
	private Perl5Util p5u = null;
	private String regExp = null;
	private boolean substTimeStamp = false;
	private SimpleDateFormat sdf = null;

	public SubstituteWithTime(String regExp, boolean substTimeStamp, TimeZone tz)
	{
		try {
			p5u = new Perl5Util();
			p5u.substitute(regExp, "");
			this.regExp = regExp;
			this.substTimeStamp = substTimeStamp;
	        if(substTimeStamp) {
                sdf = new SimpleDateFormat();
                if(tz!=null)
                	sdf.setTimeZone(tz);
	        } 
		} catch(MalformedPerl5PatternException e) {
			log.error("SubstituteWithTime. RegExp: '{}' - {}.", regExp, e.getMessage());
			p5u = null;
		}
	}
	
	public boolean isValid()
	{
		return p5u==null ? false : true; 
	}
	
	public String substitute(String src, Date date)
	{
		String dst = src;
		if(p5u!=null) {
	        dst = p5u.substitute(regExp, src);
	        if(substTimeStamp && !dst.equals(src)) 
	        {
	        	sdf.applyPattern(dst);
	        	dst = sdf.format(date);
	        } 
		}
		return dst;
	}
	
}
