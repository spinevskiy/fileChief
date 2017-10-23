package psn.filechief.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utl 
{
	private static Logger log = LoggerFactory.getLogger(Utl.class.getName());
	public static final String SPLIT1_1 = "\\{\\s*(.+?)\\s*\\}";
	public static final String SPLIT1_2 = "\\s*;\\s*";

	public static ArrayList<String[]> stringToList1(String str)
	{
		ArrayList<String[]> ret = new ArrayList<>(20);
		String[] rr = RegEx.splitListGrp(str, SPLIT1_1);
		for(String r : rr)
		{
			String[] res = r.split(SPLIT1_2);
			if(res.length>1) 
				ret.add(res);
		}	
		return ret;
	}

	public static final String SPLIT2_1 = "\\s*;\\s*";
	public static final String SPLIT2_2 = "\\s*,\\s*";

	public static ArrayList<String[]> stringToList2(String str)
	{
		ArrayList<String[]> ret = new ArrayList<>(20);
		String[] rr = str.split(SPLIT2_1);
		for(String r : rr)
		{
			String[] res = r.split(SPLIT2_2);
			if(res.length>1) 
				ret.add(res);
		}	
		return ret;
	}
	
	public static final String STR_BEG1 = " { ";
	public static final String STR_END1 = " } ";
	public static final String STR_DELIM1 = " ; ";
	public static String listToString1(ArrayList<String[]> lst)
	{
		StringBuilder sb = new StringBuilder(80);
		for(String[] rr : lst)
		{
			if(rr==null) continue;
			sb.append(STR_BEG1);
			for(int i = 0; i< rr.length; i++)
			{
				if(i>0) sb.append(STR_DELIM1);
				sb.append(rr[i]);
			}
			sb.append(STR_END1);
		}	
		return sb.toString();
	}

	public static final String STR_DELIM2_1 = " , ";
	public static final String STR_DELIM2_2 = " ; ";
	public static String listToString2(ArrayList<String[]> lst)
	{
		StringBuilder sb = new StringBuilder(80);
		for(int k = 0; k< lst.size(); k++)
		{
			String[] rr = lst.get(k); 
			if(rr==null) continue;
			if(k>0)
				sb.append(STR_DELIM2_2);
			for(int i = 0; i< rr.length; i++)
			{
				if(i>0) sb.append(STR_DELIM2_1);
				sb.append(rr[i]);
			}
		}	
		return sb.toString();
	}
	
	/**
	 * 
	 * @param x - string for check 
	 * @return true if (x==null || x.trim().length()==0 ) 
	 */
	public static final boolean isEmpty(String x)
	{
		return (x==null || x.trim().length()==0);
	}

	public static final String trimToNull(String x)
	{
		return (x==null || x.trim().length()==0) ? null : x;
	}

	public static final boolean in(int x, int... list)
	{
		for(int y : list)
			if(x==y)
				return true;
		return false;
	}

	public static final int str2interval(String interval)
	{
		if(interval==null) return 0;
		interval = interval.trim().toUpperCase();
		int val = 0;
		String x = "";
		if(interval.matches("\\d+[DHMS]")) {
			int len = interval.length();
			val = Integer.parseInt(interval.substring(0, len-1));
			x = interval.substring(len-1, len);
		} else if(interval.matches("\\d+")) 
			val = Integer.parseInt(interval);
		else {
			log.error("invalid interval value '{}' , must be \\d+[DdHhMmSs]", interval);
			return 0;
		}

		if(x.equals("D"))
			val *= 24*60*60;
		else if(x.equals("H"))
			val *= 60*60;
		else if(x.equals("M"))
			val *= 60;
		
		return val;
	}
	
	public static final void createHardLink(File from, String toDir, String toName) throws IOException
	{
		Path pFrom = Paths.get(from.getPath());
		Path newLink = Paths.get(toDir, toName);
		try {
		    Files.createLink(newLink, pFrom);
		} catch (UnsupportedOperationException x) {
			throw new IOException(x);
		}
		return;
	}
	
	public static final void moveFile(File from, File to) throws IOException
	{
		Path pFrom = Paths.get(from.getPath());
		Path pto = Paths.get(to.getPath());
		Files.move(pFrom, pto, StandardCopyOption.ATOMIC_MOVE );
	}
	
	public static final void copyFile(File from, File to) throws IOException
	{
		Path pFrom = Paths.get(from.getPath());
		Path pto = Paths.get(to.getPath());
		Files.copy(pFrom, pto);
	}

	public static final void copyInputStreamToFile(InputStream is, File to) throws IOException
	{
		Path pto = Paths.get(to.getPath());
		Files.copy(is, pto);
	}
	
	public static boolean boolEq(Boolean x, boolean test)
	{
		return x!=null && x.booleanValue()==test;
	}
	
    public static String escapeXML(String s) 
    {
    	if(s==null || s.length()==0)
    		return s;
        StringBuilder ret = new StringBuilder();
        for(int i=0; i<s.length(); i++)
            switch (s.charAt(i)) {
            	case '<': ret.append("&lt;");  break;
            	case '>': ret.append("&gt;");  break;
            	case '"': ret.append("&quot;");break;
            	case '\'':ret.append("&apos;");break;
            	case '&': ret.append("&amp;"); break;
            	default: ret.append(s.charAt(i));
            }
        return ret.toString();
    }
    
    public static String escapeXML2(String s) 
    {
    	if(s==null || s.length()==0)
    		return s;
        StringBuilder ret = new StringBuilder();
        for(int i=0; i<s.length(); i++)
            switch (s.charAt(i)) {
            	case '<': ret.append("&lt;");  break;
            	case '"': ret.append("&quot;");break;
            	case '&': ret.append("&amp;"); break;
            	default: ret.append(s.charAt(i));
            }
        return ret.toString();
    }
    
	public static String findAndReplaceParams(String input, String pref, String suf, Map<String,String> paramsMap)
	{
		if(input!=null)
			for(Entry<String,String> s : paramsMap.entrySet()) {
				if(s.getValue()!=null)
					input = input.replace(pref+s.getKey()+suf, s.getValue());
			}
		return input;
	}

	 public static String uncapFirst(String in) {
	     if(in==null)
	         return null;
	     if(in.length()<2)
	         return in.toLowerCase();
	     return in.substring(0, 1).toLowerCase() + in.substring(1);
	 }

	 public static String uncapFirstTwo(String in) {
	     if(in==null)
	         return null;
	     if(in.length()<3)
	         return in.toLowerCase();
	     return in.substring(0, 2).toLowerCase() + in.substring(2);
	 }
	 
	 public static boolean makeDirs(String dir) {
		 if(dir==null || dir.length()==0)
			 return true;
		 File f = new File(dir);
		 if(!f.exists())
			 return makeDirs(f);
		 if(!f.isDirectory()) {
			 log.error("makeDirs - isn't directory: '"+f.getPath()+"'");
			 return false;
		 }
		 return true;
	 }

	 private static boolean makeDirs(File dir) {
		 boolean ret = dir.mkdirs();
		 if(!ret)
			 log.error("makeDirs - error creating '" + dir+ "'");
		 return ret;
	 }

}
