package psn.filechief;

//import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
//import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psn.filechief.util.RegEx;
import psn.filechief.util.bl.FileData;

public class FileTimeStamps 
{
	private static Logger log = LoggerFactory.getLogger(FileTimeStamps.class.getName());
	private ArrayList<FileTS> fts = new ArrayList<>();
	private SimpleDateFormat frmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
	
	public FileTimeStamps(ArrayList<String[]> stamps, String fileMask, TimeZone tz)
	{
		if(stamps != null)
		for(String[] x : stamps)
			fts.add(new FileTS(x, fileMask, tz));
	}

	public void updateTimestamp(FileData f) throws ParseException
	{
		String name = f.getName();  
		for(FileTS ft : fts) {
			long x = ft.parse(name);
			if(x!=-1) {
				f.setModTime(x);
				break;
			}
		}
	}

	private class FileTS 
	{
		private RegEx regex;
		private SimpleDateFormat df;

		public FileTS(String[] stamp, String fileMask, TimeZone tz)
		{
			stamp[0] = stamp[0].trim(); 
			if(stamp[0].equals("*"))
				stamp[0] = fileMask;
			
			regex = new RegEx(stamp[0]);
			String format = stamp[1].trim();
			String lang = null;
			String country = null;
			if(stamp.length > 2)
				lang = stamp[2].trim();
			if(stamp.length > 3)
				country = stamp[3].trim();
			if(lang!=null) 
			{
				Locale loc;
		        if(country==null) loc = new Locale(lang);
		                else loc = new Locale(lang,country);
		        df = new SimpleDateFormat(format,loc);
			}	else df = new SimpleDateFormat(format);
			if(tz!=null) df.setTimeZone(tz);
		}
		
		public long parse(String in) throws ParseException 
		{
			String[] x = regex.getGroups(in);
			if(x.length==0) return -1;
			String y;
			if(x.length==1)
				y = x[0];
			else {
				StringBuilder sb = new StringBuilder(80);
				for(String z : x)
					sb.append(z);
				y = sb.toString();
			}
			Date d;
			try {
				d = df.parse(y);
			} catch (ParseException e) {
				throw new ParseException(e.getMessage()+" ; date format is '"+df.toPattern()+"'", 0);
			}
			if(log.isDebugEnabled()) 
				log.debug("date string '{}' parsed as '{}'", y, frmt.format(d));
			return d.getTime();
		}
		
	}// end FileTS
}
