package psn.filechief.util;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ParamsStorage 
{
	protected static final Logger log = LoggerFactory.getLogger(ParamsStorage.class);
	
	private static String parPrefix = "{";
	private static String parSuffix = "}";
	private final String parNamePattern;
	protected boolean logAddParam = true;

	protected HashMap<String,String> allParams = new HashMap<>();
	
	public abstract String getNamePattern(); 

	public abstract String getStorageName(); 
	
	public ParamsStorage() 
	{
		parNamePattern = getNamePattern();
	}
	
	public ParamsStorage(String parNamePattern)
	{
		this.parNamePattern = parNamePattern;
	}
	
	public void addParam(Param p)
	{
		if(p==null) 
			throw new IllegalArgumentException(getStorageName()+" addParam : null");
		String key = p.getName();
		if(key==null)
			throw new IllegalArgumentException(getStorageName()+" addParam : unnamed parameter");

		if(!key.matches(parNamePattern)) {
			throw new IllegalArgumentException(getStorageName()+" addParam : invalid name = '"+key+"' , name pattern is '"+parNamePattern+"'");
		}			
		// Значение может содержать ссылки на другие параметры, их надо обработать.
		String value = findAndReplace(p.getValue());
		if(value==null) 
			throw new IllegalArgumentException(getStorageName()+ " addParam : parameter '{"+key+"}' has not value");
		allParams.put(key, value);
		if( logAddParam ) {
			String x = "password".contains(key.toLowerCase()) ? "*****" : value;
			log.info(getStorageName()+" add parameter name='{}' , value='{}'", key, x);
		}	
	}
	
	public String getValue(String parName)
	{
		return allParams.get(parName);
	}
	
	public boolean paramExists(String parName)
	{
		return allParams.containsKey(parName);
	}
	
	public String getPatternRegEx(String prefix, String suffix)
	{
		return getPatternRegEx(getNamePattern(), prefix, suffix);
	}

	public static final String getPatternRegEx(String parRegEx, String prefix, String suffix)
	{
		String pre = parPrefix;
		String suf = parSuffix;
		if(prefix!=null && prefix.length()>0 && suffix!=null && suffix.length()>0 )
		{
			pre = prefix;
			suf = suffix;
		}
		return "\\Q"+pre+"\\E"+parRegEx+"\\Q"+suf+"\\E";
	}
	
	public static final String getPattern(String parName, String prefix, String suffix)
	{
		String pre = parPrefix;
		String suf = parSuffix;
		if(prefix!=null && prefix.length()>0 && suffix!=null && suffix.length()>0 )
		{
			pre = prefix;
			suf = suffix;
		}
		return pre+parName+suf;
	}
	
	public String replaceParams(String parName,String src, String prefix, String suffix)
	{
		String val = getValue(parName);
		if(val==null) return src;
		String p = getPattern(parName, prefix, suffix);
		String ret = src.replace( p, val );
		if(src.contains(p))
			log.info(getStorageName()+ " replaceParams: from=\""+src+"\" to=\""+ret+"\"" );
		return ret;
	}
/*	
	public String replaceParams2(String parName,String src, String prefix, String suffix)
	{
		String val = getValue(parName);
		if(val==null) return src;
		log.info("replaceAll: parName="+parName+"  prefix="+prefix+"  suffix="+suffix+"  val="+val );
		return src.replaceAll( getPattern(parName, prefix, suffix), val );
	}
*/	
	
	public String findAndReplace(String input)
	{
		return findAndReplace(input, parPrefix, parSuffix);
	}
	
	public String findAndReplace(String input, String prefix, String suffix)
	{
		for(String key : allParams.keySet()) 
			input = replaceParams(key, input, prefix, suffix);
		return input;
	}
}
