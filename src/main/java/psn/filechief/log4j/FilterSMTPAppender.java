package psn.filechief.log4j;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.net.SMTPAppender;
import org.apache.log4j.spi.LoggingEvent;

import psn.filechief.util.RegEx;
import psn.filechief.util.ThreadList;

public class FilterSMTPAppender extends SMTPAppender 
{
	public static final String SPLIT1 = "\\{\\s*(.+?)\\s*\\}";
	public static final String SPLIT2 = "\\s*;\\s*";
	
	public static final String singleMessage = "SingleMessage";
	public static final String singleMessageTrue = "true";
	
	private String keyPattern = null;
	private Pattern pattern = null;
	private Matcher matcher = null;
	private LinkedBlockingQueue<LoggingEvent> queue = null;
	private Thread worker = null; 
	private Thread worker2 = null; 
	
	private ArrayList<Rule> warnRules = null;
	private ArrayList<Rule> errorRules = null;
	
//	private String[] smtpHosts2 = new String[2];
	
	private volatile String savedSubject =  null;
	
	private String subjInfoPattern = null;
//	private String subjInfoPattern2 = null;

	@Override
	public void activateOptions() 
	{
		EnvToMDC.getInstance();
		super.activateOptions();
	}
	
	@Override
	public void setSMTPHost(String smtpHost) 
	{
		smtpHost = EnvToMDC.getInstance().replaceEnvParams(smtpHost);
		//smtpHosts[0] = smtpHost;
		super.setSMTPHost(smtpHost);
		init();
	}

	public void setSMTPHost2(String smtpHost) 
	{
		//smtpHosts[1] = smtpHost;
		init();
	}
	

	public void setKeyPattern(String pat)
	{
		setPattern(pat);
	}
	
	public void setPattern(String pat)
	{
		if(pat==null) return;
		pat = pat.trim();
		if(pat.length()==0) return;
		keyPattern = pat;
		pattern = Pattern.compile(keyPattern);
		matcher = pattern.matcher("");
	}
	
	@Override
	public void setTo(String to)
	{
		to = EnvToMDC.getInstance().replaceEnvParams(to);
		super.setTo(to);
	}

	@Override
	public void setFrom(String from)
	{
		from = EnvToMDC.getInstance().replaceEnvParams(from);
		super.setFrom(from);
	}
	
	@Override
	public void setSubject(String subj)
	{
		subj = EnvToMDC.getInstance().replaceEnvParams(subj);
		if(subj!=null && ( subj.contains("%t") || subj.contains("%i") ))
			savedSubject = subj;
		super.setSubject(subj);
	}
	
	private void applySubject(LoggingEvent event)
	{
		String useAsThreadName = event.getProperty(Evaluator.AS_THREAD_NAME); 
		if(savedSubject!=null)
		{
			String sPat = subjInfoPattern;
			String tn = event.getThreadName();
			if(useAsThreadName!=null) {
				tn = useAsThreadName;
				sPat = "";
				
			}
			String s = savedSubject.replaceFirst("%t",tn);
			
			if(sPat!=null) 
			{
				String z = "";
				if(sPat.length()>0)
				{
					String[] x = RegEx.getGroupsS(event.getRenderedMessage(), sPat);
					if(x.length>0) z = x[0];
				}
				s = s.replaceFirst("%i",z);
			}	
			try {
				super.msg.setSubject(MimeUtility.encodeText(s, "UTF-8", null));
	        } catch (UnsupportedEncodingException ex) {
	        	LogLog.error("Unable to encode SMTP subject", ex);
	        }catch (MessagingException e) {
	            LogLog.error("Could not activate SMTPAppender options.", e);
	        }
		}
	}
	
	public void setWarnRules(String rulesString)
	{
		init();
		warnRules = setXRules(rulesString, Level.WARN);
	}

	public void setErrorRules(String rulesString)
	{
		init();
		errorRules = setXRules(rulesString, Level.ERROR);
	}
	
	private ArrayList<Rule> setXRules(String rulesString, Level level)
	{
		ArrayList<Rule> rules = new ArrayList<>(20);
		String[] rr = RegEx.splitListGrp(rulesString, SPLIT1);
		for(String r : rr)
		{
			String[] res = r.split(SPLIT2);
			if(res.length>1) {
				Rule ru = new Rule(res,level);
				rules.add(ru);
			}
		}	
		return rules;
	}
	
	private ArrayList<Rule> findTimeRules()
	{
		ArrayList<Rule> ret = new ArrayList<>(20);
		if(errorRules!=null)
			for(Rule r: errorRules)
				if(r.getActionId()==Rule.TIME)
					ret.add(r);
		if(warnRules!=null)
			for(Rule r: warnRules)
				if(r.getActionId()==Rule.TIME)
					ret.add(r);
		return ret;
	}
	
	private int checkRules(LoggingEvent event, String rmes, ArrayList<LoggingEvent> ret) 
	{
		String extraKey = "";
		if(matcher!=null) {
			matcher.reset(rmes);
			if( matcher.find() && matcher.groupCount()>=1) 
				extraKey = matcher.group(1);
		}
		ret.clear();
		if(event.getLevel()==Level.WARN) {
			if(warnRules!=null)
				for(Rule r : warnRules) {
					int res = r.logThis(event, rmes, extraKey, ret);
					if(res!=Rule.NEXT)
						return res;
				}
			return Rule.NO;
		}
		
		if(event.getLevel()==Level.ERROR) {
			if(errorRules!=null)
				for(Rule r : errorRules) {
					int res = r.logThis(event, rmes, extraKey, ret);
					if(res!=Rule.NEXT)
						return res;
				}
		}
		
		return Rule.YES;
	}
		
	private void checkQueue() 
	{
		LoggingEvent ev;
		try {
			while(true) { 
				ev = queue.take(); // спим в ожидании
				// если в очереди на отправку появилось новое событие - отправляем.
					applySubject(ev);
					super.append(ev);  
			}
		} catch (InterruptedException ex) { 
			Thread.currentThread().interrupt(); 
		}	
	}

	private void putToQueue(LoggingEvent ev)  
	{
		try {
			queue.put(ev);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private long prevCheck = 0;
	private void checkTimeRulesX() 
	{
		long d = System.currentTimeMillis();
		List<List<LoggingEvent>> ret = new ArrayList<List<LoggingEvent>>(); 
		if(d-prevCheck < 20000) 
			return;
		prevCheck = d;
		List<Rule> lst = findTimeRules();
		LoggingEvent e;
		for(Rule r : lst) 
		{
			ret.clear();
			r.checkByTime(ret);
			// сбрасываем на отправку группу событий, предварительно пометив все, 
			// кроме последнего, что немедленная отправка не нужна (чтобы они попали в одно письмо)
			for(List<LoggingEvent> events : ret) 
			{
				int sz = events.size();
				Set<String> tns = new HashSet<String>();
				for(int i=0; i<sz; i++ ) 
				{
					e = events.get(i);
					tns.add(e.getThreadName());
					if(i+1<sz) 
						e.setProperty(Evaluator.SEND_NOW, Evaluator.SEND_NO);
					else {
						if(tns.size()>1) 
						{
							String tname = Evaluator.MULTI;
							if(tns.size()<5) 
							{									
								StringBuilder sb = new StringBuilder(); 
								for(String s :tns)
									sb.append(" ").append(s).append(";");
								tname = sb.toString();
							}
							e.setProperty(Evaluator.AS_THREAD_NAME, tname);
						}
					}
					putToQueue(e); //super.append(e);
				}
			}
		}
	}		
	
	private void checkTimeRules()
	{
		while(true) 
		{
			try {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break; 
				}
				checkTimeRulesX();
			} catch (Exception e) {
				LogLog.error("checkTimeRules: ", e);
			}
		}
	}
	
	private boolean inited = false; 
	private synchronized void init() 
	{
		if(!inited)
		{
			queue = new LinkedBlockingQueue<LoggingEvent>();
			worker = new Thread() { public void run(){ checkQueue(); }; };
			worker.setName(worker.getName()+"-SmtpApp");
			//worker.setDaemon(true);
			worker.start();
			ThreadList.getInstance().addThread(worker);

			worker2 = new Thread() { public void run(){ checkTimeRules(); }; };
			worker2.setName(worker2.getName()+"-SmtpAppCheckTimeRules");
			worker2.setDaemon(true);
			worker2.start();
			ThreadList.getInstance().addThread(worker2);
			inited = true;
			}
	}

	/**
	 * Получает информацию о новом событии журнала.
	 */
	@Override	
	public void append(LoggingEvent event) 
	{
		ArrayList<LoggingEvent> ret = new ArrayList<>(50);
		StringBuilder sb = new StringBuilder(1024);
		sb.append("[").append(event.getThreadName()).append("] ").append(event.getRenderedMessage());
		String[] ti = event.getThrowableStrRep();
		if(ti!=null)
			for(String s : ti)
				sb.append(s).append("\n");
		
		String mes = sb.toString();
		boolean enable = false;
		int lv = event.getLevel().toInt();
		if(lv==Priority.FATAL_INT) enable = true;
		else if(lv==Priority.WARN_INT || lv==Priority.ERROR_INT) {
			int res = checkRules(event, mes, ret);
			if(res==Rule.YES) enable = true;
		}
		if(!enable)
			return;

		if(ret.size()==0) { // если это одиночное событие - отправляем
			event.setProperty(singleMessage, singleMessageTrue);
			putToQueue(event); //super.append(event);
			return;
		}
		// если же это группа событий - сбрасываем на отправку, предварительно пометив все, 
		// кроме последнего, что немедленная отправка не нужна (чтобы они попали в одно письмо)
		LoggingEvent e;
		int sz = ret.size();
		for(int i=0; i<sz; i++ ) 
		{
			e = ret.get(i);
			if(i+1<sz)
				e.setProperty(Evaluator.SEND_NOW, Evaluator.SEND_NO);
			putToQueue(e); //super.append(e);
		}
	}
	
	public String getKeyPattern() {
		return keyPattern;
	}

	public void setSubjInfoPattern(String pattern) 
	{
		if(pattern==null) return;
		pattern = pattern.trim();
		if(pattern.length()==0)	return;
		this.subjInfoPattern = pattern;
	}

	public void setSubjInfoPattern2(String pattern) 
	{
		if(pattern==null) return;
		pattern = pattern.trim();
		if(pattern.length()==0)	return;
		//this.subjInfoPattern2 = pattern;
	}
	
	
}
