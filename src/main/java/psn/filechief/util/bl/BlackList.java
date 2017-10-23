package psn.filechief.util.bl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlackList<K,V> 
{
	private static Logger log = LoggerFactory.getLogger(BlackList.class.getName());
	
	private Map<K,V> blackList = null;
	
	private Map<K,V> lastCheckResult = null;
	
	protected final String listName;
	
	protected BlackList(String listName) {
		this.listName = listName;
	}
	
	public Map<K,V> getBlackMap() {
		return blackList;
	}
	
	protected int getSize()
	{
		if(blackList==null)
			return 0;
		return blackList.size();
	}
	
	protected void clearList()
	{
		if(blackList != null)
		{	
			if( getSize()>0 ) 
				log.warn("{} cleared , remove records:{}", listName, blackList.size());
			blackList.clear();
			blackList = null;
		}
		clearFound();
	}
	
	protected void addToList(K key, V val)
	{
		if(blackList==null) 
			blackList = new HashMap<>(200);
		blackList.put(key, val);
		//log.warn("added to blackList:'{}'", key);
	}

	/**
	 * Очищает перечень элементов, которые не прошли проверку по чёрному списку.
	 */
	public void clearFound() {
		if(lastCheckResult==null)
			return;
		lastCheckResult.clear();
		lastCheckResult = null;
	}

	private void addToFound(K key, V item) 
	{
		if(lastCheckResult==null)
			lastCheckResult = new HashMap<>(100);
		lastCheckResult.put(key, item);
	}

	private void delFromFound(K key) 
	{
		if(lastCheckResult==null || lastCheckResult.remove(key)==null) 
			return;
		if(lastCheckResult.size()==0)
			clearFound();
	}
	
	public int getFound() {
		if(lastCheckResult==null) 
			return 0;
		return lastCheckResult.size();
	}
	
	 public Collection<V> getFoundOnLastCheck() 
	 {
		if(lastCheckResult==null) { 
			List<V> tmp = Collections.emptyList();
			return tmp;
		}	
		return lastCheckResult.values();
	}
	
	 public ArrayList<V> getToTrimToLastCheckResult()
	 {
		 int sz = getSize();
		 if(sz==0 || sz==getFound())
			 return null;
		 ArrayList<V> toRemove = new ArrayList<>(20);
		 for(V v : blackList.values())
			 if(!lastCheckResult.containsValue(v))
				 toRemove.add(v);
		 return toRemove;
	 }
	
	public boolean inList(K key)
	{
		V b;
		if(blackList==null || (b=blackList.get(key))==null) 
			return false;
		addToFound(key, b);
		return true;
	}

	public V getValue(K key)
	{
		if(blackList==null) 
			return null;
		return blackList.get(key);
	}
	
	protected boolean removeFromList(K key, boolean needLog)
	{
		if(blackList==null || blackList.remove(key)==null)
			return false;
		delFromFound(key);
		if(needLog)
			log.warn("removed from {}:'{}'", listName, key);
		if(blackList.size()==0) {
			blackList = null;
			clearFound();
			log.warn("{} cleared", listName);
		}	
		return true;
	}
	
	class Unit 
	{
		
	}
	
}
