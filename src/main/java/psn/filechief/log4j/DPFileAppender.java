package psn.filechief.log4j;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.spi.LoggingEvent;

/**
 * DPFileAppender - Date Pattern FileAppender 
 * 
 * @author snpinevs
 *
 */
public class DPFileAppender extends org.apache.log4j.FileAppender 
{
	public static final String UNDEF = "'undefined'";
	public static final String DEF_PATTERN = ".yyyyMMdd_HHmmss_SSS";
	public static final String TMP_SUFFIX = ".tmp";
	
	private String datePattern = DEF_PATTERN;
	private String filePrefix = "";
	private String suffix = "";
	private SimpleDateFormat sdf = new SimpleDateFormat();
	private LogFilter filter = null;
	private int sequence = 0;
	private int sequenceLength = 0;
	private String seqFormat = "";
	
	private void initFilter()
	{
	    if(filter==null) {
	        filter = new LogFilter();
	        addFilter(filter);
	    }
	}
	
	@Override
	public void append(LoggingEvent event)
	{
		StringBuilder sb = new StringBuilder(filePrefix);
		sb.append(sdf.format(new Date()));
		if(sequenceLength>0)
			sb.append("_").append(String.format(seqFormat, sequence++));
		if(suffix.length()>0)
			sb.append(suffix);
		String fName = sb.toString();
		sb.append(TMP_SUFFIX);	
		String tmpName = sb.toString();
		super.setFile(tmpName);// задаём имя временного файла
		super.activateOptions(); // открываем OutputStream
		super.append(event); // пишем событие в файл
		super.reset(); // закрываем файл
		File tmp = new File(tmpName);
		tmp.renameTo(new File(fName));
	  }

	@Override
	public void activateOptions() 
	{
		EnvToMDC.getInstance();
		if(sequenceLength>9)
			sequenceLength = 9;
		if(sequenceLength>0)
			seqFormat = "%0"+sequenceLength+"d";
		sdf.applyPattern(datePattern);
		String x = super.getFile();
		if(x==null || x.length()==0)
			x = UNDEF;
		filePrefix = x; 
		super.setBufferedIO(false);
		super.setAppend(false);
	}

	public void setAllowPattern(String pattern)
	{
	    initFilter();
	    filter.setAllowPattern(pattern);
	}

	public void setDenyPattern(String pattern) 
	{
		initFilter();
		filter.setDenyPattern(pattern);
	}
	
	public void setMinLogLevel(String level)
	{
		initFilter();
		filter.setMinLogLevel(level);
	}

	public String getMinLogLevel()
	{
		return "";
	}
	
	public String getDatePattern() {
		return datePattern;
	}

	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public int getSequenceLength() {
		return sequenceLength;
	}

	public void setSequenceLength(int sequenceLength) {
		this.sequenceLength = sequenceLength;
	}
	
}
