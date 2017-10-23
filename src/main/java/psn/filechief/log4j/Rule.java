package psn.filechief.log4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import psn.filechief.util.RegEx;

public class Rule 
{
	public static final int YES = 1; 
	public static final int NO = 2; 
	public static final int COUNT = 3; 
	public static final int TIME = 4; 

	public static final int NEXT = 999; 
	public static final int LOG = 1; 
	public static final int NOLOG = 2; 

	public static final String S_PLUS = "+"; 
	public static final String S_MINUS = "-";
	public static final String S_THREAD = "t";
	public static final String S_EXTRA_KEY = "k";
	
	public static final String PAT_COUNT = "count:(\\d+)\\s+time:(\\d+)"; // count:(\\d+)\\s+time:(\\d+)(?:\\s+clear:(.+))? 
	public static final String PAT_TIME = "time:(\\d+)\\s+count:(\\d+)"; 
	
	private Level level;
	private String pattern;
	private Pattern pat;
	private String action;
	private int actionId;
	private Matcher matcher;
	private int minEventCount = -1;
	private int minEventTimeSpan = -1;
	private long timeSpan = -1;
	private boolean useExtraKey = false;
	private boolean useThreadNameAsKey = false;
	
	private EventStore eventStore = null;

	public Rule(String[] in, Level logLevel) 
	{
		level = logLevel;
		pattern = in[0];
		action = in[1];
		String[] cnt = null;
		if(action.equals(S_PLUS)) actionId = YES;
		else if(action.equals(S_MINUS)) actionId = NO;
		else if(action.matches(PAT_COUNT)) { 
			actionId = COUNT;
			cnt = RegEx.getGroupsS(action, PAT_COUNT);
			minEventCount = Integer.parseInt(cnt[0]);
			minEventTimeSpan = Integer.parseInt(cnt[1]);
			if(in.length>2 && in[2]!=null && in[2].length()>0 ) 
			{
				if(in[2].contains(S_EXTRA_KEY))
					useExtraKey = true;
				useExtraKey = true; //??????????
				if(in[2].contains(S_THREAD))
					useThreadNameAsKey = true;
			}	
			eventStore = new EventStore();
		}
		else if(action.matches(PAT_TIME)) {
			actionId = TIME;
			cnt = RegEx.getGroupsS(action, PAT_TIME);
			minEventCount = Integer.parseInt(cnt[1]);
			minEventTimeSpan = Integer.parseInt(cnt[0]);
			if(in.length>2 && in[2]!=null && in[2].length()>0 ) 
			{
				if(in[2].contains(S_EXTRA_KEY))
					useExtraKey = true;
				if(in[2].contains(S_THREAD))
					useThreadNameAsKey = true;
			}	
			eventStore = new EventStore();
		} //else 
		if(minEventTimeSpan!=-1)
			timeSpan = minEventTimeSpan*60*1000; 
		pat = Pattern.compile(pattern);
		matcher = pat.matcher("");
	}

	public int logThis(LoggingEvent event, String rmes, String extraKey, List<LoggingEvent> ret )
	{
		if(!event.getLevel().equals(level)) 
			return NEXT;
		matcher.reset(rmes);
		if(!matcher.find())
			return NEXT;
		
		switch(actionId) 
		{
			case YES :
			case NO  : return actionId;
			case COUNT :
			case TIME : StringBuilder xkey = new StringBuilder(pattern);
						if(useThreadNameAsKey)
							xkey.append(event.getThreadName());
						if(useExtraKey)
							xkey.append(extraKey);
 						boolean res = eventStore.check(this, xkey.toString(), event, ret);
						if(res && ret.size()>0) return YES;
		}
		return NO;
	}
	
	public boolean checkByTime(List<List<LoggingEvent>> ret)
	{
		if(actionId!=TIME || eventStore==null) 
			return false;
		//ret.clear(); // ?????
		return eventStore.checkByTime(ret, timeSpan, minEventCount);
	}
	
	public String getPattern() {
		return pattern;
	}
	public String getAction() {
		return action;
	}
	
	class EventStore 
	{
		public static final int initMapSize = 100;
		public static final int initListSize = 50;
		private Map<String,List<LoggingEvent>> map = new ConcurrentHashMap<String, List<LoggingEvent>>(initMapSize);
		
		public boolean checkByTime(List<List<LoggingEvent>> ret, long timeSpan, int minEventCount)
		{
			boolean added = false;
			// перебираем наборы событий, сгруппированные по ключам
			Set<String> keys = map.keySet(); // получаем ключи
			for(String key : keys)  // для каждого ключа
			{
				List<LoggingEvent> al = map.get(key); // ищем набор событий для данного ключа
				int sz = al.size();
				long d = System.currentTimeMillis();
				if(sz >= minEventCount && d-al.get(0).timeStamp>timeSpan) // если кол-во событий достигло границы и самое старое было ранее timeSpan мс назад  
				{
					ret.add(al); // значит надо вывести записи
					map.remove(key); // и сбросить накопленные события
					added = true;
				} else { // отбрасываем старые события
					ArrayList<LoggingEvent> lst = new ArrayList<LoggingEvent>(); 
					for(LoggingEvent ev : al)
						if(d-ev.timeStamp > timeSpan)
							lst.add(ev);
					al.removeAll(lst);
					
					/*
					Iterator<LoggingEvent> it = al.iterator();
					while(it.hasNext())
						if(d-it.next().timeStamp>timeSpan)
							it.remove();
							*/
					if(al.size()==0)
						map.remove(key);
				}
			}
			return added;
		}
		
		public boolean check(Rule rule, String key, LoggingEvent event, List<LoggingEvent> ret) 
		{
			//LoggingEvent[] ret = null;
			boolean res = false;
			if(!map.containsKey(key)) { // событий для данного ключа ешё нет - добавляем
				List<LoggingEvent> al = new CopyOnWriteArrayList<LoggingEvent>();
				al.add(event);
				map.put(key, al);
				return res; // ret;
			}
			
			List<LoggingEvent> al = map.get(key); // ищем набор событий для данного ключа
			al.add(event);
			int sz = al.size();
			if(rule.actionId == COUNT) // журналирование по достижению minEventCount событий в период не более timeSpan(мс). 
			{
				for(int i=0; i < sz-1; i++) // проверяем все события, начиная с старейшего, кроме последнего. 
				{
					LoggingEvent e = al.get(i);
					if(event.timeStamp-e.timeStamp < rule.timeSpan)  // если время старого событиия не слишком старо, считаем его первым в серии
					{
						if( sz >= rule.minEventCount) { // и кол-во событий достигло границы
							ret.addAll(al); // значит надо вывести записи
							map.remove(key); // и сбросить накопленные события
							res = true;
						}  
						return res; // ret;
					} else { // иначе такие старые события должны быть удалены
						al.remove(0);
						sz = al.size();
					}	
				}
				return res;
			}
		/*
			if(rule.actionId == TIME) // журналирование по накоплению за время timeSpan(мс) не менее minEventCount событий. 
			{
				for(int i=0; i < sz-1; i++) // проверяем все события, начиная с старейшего, кроме последнего. 
				{
					LoggingEvent e = al.get(i);
					long delta = event.timeStamp-e.timeStamp;
					if(delta < rule.timeSpan)  // если со времени самого старого событиия не прошло timeSpan мс - копим дольше.
						break;
					if(delta > rule.timeSpan*3/2) { // если время старого событиия слишком старо, отбрасываем событие
						al.remove(0);
						sz = al.size();
					} else { // проверяем количество событий
						if( sz >= rule.minEventCount) { // и кол-во событий достигло границы
							ret.addAll(al); // значит надо вывести записи
							map.remove(key); // и сбросить накопленные события
							res = true;
						}  
						return res;
					} 
				}
			}
	 */
			return res;
		}
	}

	public Level getLevel() {
		return level;
	}

	public int getActionId() {
		return actionId;
	}

	public int getMinEventCount() {
		return minEventCount;
	}

	public int getMinEventTimeSpan() {
		return minEventTimeSpan;
	}
	
}
