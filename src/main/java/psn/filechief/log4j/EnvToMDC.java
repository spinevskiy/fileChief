package psn.filechief.log4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.MDC;

import psn.filechief.util.Utl;

/**
 * Грузит некоторые переменные окружения в MDC Log4J.
 * @author snpinevs
 *
 */
public class EnvToMDC 
{
	public static final String L4J_MDC[] = {"ZONE", "SERVER", "MAIL_HOST", "MAIL_FROM", "MAIL_TO" }; 
	public static final String PREFIX = "MDC_" ;	
	public static final EnvToMDC INSTANCE = new EnvToMDC();	
	private Map<String,String> logenv = Collections.synchronizedMap(new HashMap<String,String>());
	private String[] info = new String[]{};

	private EnvToMDC()
	{
		ArrayList<String> onLoad = new ArrayList<String>();
		for(String s : L4J_MDC) {
			String val = System.getenv(PREFIX + s.toUpperCase());
			if(val!=null && val.trim().length()>0) {
				MDC.put(s, val);
				logenv.put(s, val);
				//LogLog.warn("add to MDC: '"+s+"'='"+val+"'");
				onLoad.add("added to MDC: '"+s+"'='"+val+"'");
			}
		}
		info = onLoad.toArray(info);
	}

	public static EnvToMDC getInstance() {
		return INSTANCE;
	}
	
	public String replaceEnvParams(String input)
	{
		return Utl.findAndReplaceParams(input, "%X{", "}", logenv);
	}

	public String[] getInfo() {
		return info;
	}
	

}
