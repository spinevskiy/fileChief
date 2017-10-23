package psn.filechief;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import psn.filechief.util.RefUtl;

public class PatternStore 
{
	private static final Logger log = LoggerFactory.getLogger(PatternStore.class.getName());
	private static final PatternStore instance = new PatternStore();
	
	private Map<String,Cfg> patterns = new HashMap<String,Cfg>();
	
	private PatternStore() { }
	
	public static PatternStore getInstance() 
	{
		return instance;
	}
	
	public void addPattern(Cfg cfg) 
	{
		if(cfg==null) return;
		String name = cfg.getName().trim();
		if(name==null || name.length()==0) {
			throw new IllegalArgumentException("try add unnamed pattern");
		}
		if( patterns.containsKey(name) ) {
			throw new IllegalArgumentException("pattern with name '"+name+"' already added");
		}
	
		// применяем шаблоны(ы)
		Cfg child = cfg;
		do {
			applyPatterns( child );
		} while( (child=child.getChild()) !=null); 
		
		patterns.put(name, cfg);
	}
	

	/**
	 * Копирует значения заполненных полей (т.е. не null) из шаблона в незаполненные поля целевого агента.
	 * @param cfg
	 * @param pattern
	 * @throws IllegalArgumentException при ошибке установки значений полей.
	 */
	public static void setFieldsFromPattern(Cfg cfg, Cfg pattern)
	{
		RefUtl src = new RefUtl(pattern); 
		RefUtl dst = new RefUtl(cfg); 
		
		for(String prop : FileChief.PROP_NAMES) 
		{
			try {
				Object x = null;
				if(!dst.hasGetter(prop))
					continue;
				x = dst.get(prop);
				if(x!=null || !src.hasGetter(prop)) 
					continue;
				x = src.get(prop);
				if(x==null || "name".equals(prop) && cfg.isFirst()) // имя агента-родителя не может быть изменено
					continue;
				dst.set(prop, x.toString());
			} catch(Exception e) {
				throw new IllegalArgumentException( "Hmm... , on set property '"+prop+"' : ",e );
			}
		}
	}
	
	private boolean applyAdd(Cfg config, Cfg pattern)
	{
		Cfg cfg = config; 
		Cfg pat = pattern;
		if(cfg==null || pat==null) return false;
		
		Cfg prev = null;
		do {
			if(cfg==null) // если следующий агент отсутствует, а у шаблона еще есть - добавляем и переносим свойства из шаблона. 
			{
				String cn = pat.getClass().getCanonicalName();
				try {
					cfg = (Cfg) Class.forName(cn).newInstance();
					prev.addChild(cfg);
					log.debug("applyAdd: added new agent from pattern, class = '{}', name = '{}'", cn, cfg.getName());
				} catch (Throwable e1) {
					throw new IllegalArgumentException("applyAdd: "+cn+" - "+e1.getMessage());
				}
			}	
			setFieldsFromPattern(cfg, pat);
			prev = cfg;
			cfg = cfg.getChild();
			pat = pat.getChild();
		} while(pat!=null);
		
		return true;
	}

	private boolean applyBefore(Cfg config, Cfg pattern)
	{
		Cfg cfg = config; 
		Cfg pat = pattern;
		if(cfg==null || pat==null) return false;
		setFieldsFromPattern(config, pattern); // переносим свойства из шаблона в целевого агента.
		
		Cfg saveChild = cfg.getChild(); // сохраняем инф. о потомке.
		cfg.setChild(null);
		while( (pat = pat.getChild()) != null ) // добавляем потомков из шаблона.
		{
			String cn = pat.getClass().getCanonicalName();
			try {
				Cfg ncfg = (Cfg) Class.forName(cn).newInstance();
				cfg.addChild(ncfg);
				setFieldsFromPattern(ncfg, pat);
				cfg = ncfg;
				log.debug("applyBefore: added new agent from pattern, class = '{}', name = '{}'",cn, cfg.getName());
			} catch (Throwable e1) {
				throw new IllegalArgumentException("applyBefore: "+cn+" - "+e1.getMessage());
			}
		}
		if(saveChild!=null)
			cfg.addChild(saveChild);
		return true;
	}

	private boolean applyAfter(Cfg config, Cfg pattern, int index)
	{
		Cfg cfg = config; 
		Cfg pat = pattern;
		if(cfg==null || pat==null) return false;
		setFieldsFromPattern(config, pattern); // переносим свойства из шаблона в целевого агента.
		
		Cfg saveChild = null;
		for(int i=0;  ; i++) 
		{
			//while( cfg.getChild() != null ) // ищем последнего в цепочке
			if(index>=0 && i==index ) 
			{
				if( cfg.getChild()!= null ) {
					saveChild = cfg.getChild(); // сохраняем инф. о потомке.
		        	cfg.setChild(null);
				}	
				break;
			}
			if( cfg.getChild()!= null ) cfg = cfg.getChild();
				else break; 
		}	
		
		while( (pat = pat.getChild()) != null ) // добавляем потомков из шаблона.
		{
			String cn = pat.getClass().getCanonicalName();
			try {
				Cfg ncfg = (Cfg) Class.forName(cn).newInstance();
				cfg.addChild(ncfg);
				setFieldsFromPattern(ncfg, pat);
				cfg = ncfg;
				log.debug("applyAfter: added new agent from pattern, class = '{}', name = '{}'",cn, cfg.getName());
			} catch (Throwable e1) {
				throw new IllegalArgumentException("applyAfter "+cn+" - "+e1.getMessage());
			}
		}

		if(saveChild!=null)
			cfg.addChild(saveChild);
		
		return true;
	}
	
	
	private boolean applySet(Cfg config, Cfg pattern)
	{
		Cfg cfg = config; 
		Cfg pat = pattern;
		while(cfg!=null && pat!=null) // переносим свойства из шаблона в сушествующих агентов. 
		{
			setFieldsFromPattern(cfg, pat);
			cfg = cfg.getChild();
			pat = pat.getChild();
		};
		
		return true;
	}

	public boolean applyPatterns(Cfg cfg)
	{
		if(cfg==null || cfg.getPattern()==null) return false;
		if(cfg._patterns.length==1 && ( cfg._applyMethods==null || cfg._applyMethods.length==0 ) )
			cfg._applyMethods = new ApplyMethod[] {ApplyMethod.SET};
		
		int cnt = cfg._patterns.length;
		if(cfg._applyMethods.length != cnt)
			throw new IllegalArgumentException("agent '"+cfg.getName()+"' - applyAllPatterns() - inconsistency between 'pattern' and 'applyMethod'");
		
		boolean ret = true; 
		for(int i = 0; i< cnt; i++)
			ret = ret && applyPattern(cfg, cfg._patterns[i], cfg._applyMethods[i]);
		return ret;
	}
	
	
	/**
	 * Для данного агента ищем шаблон (если задан), и переносим значения атрибутов (имена которых есть в PROP_NAMES) из шаблона в агент.
	 * Перенос происходит только если знасение целевого атрибута = null.  
	 * @param cfg - агента
	 * @return true - выполнено без ощибок, иначе false.
	 * @throws IllegalArgumentException если попалось неизвестное имя шаблона или ошибка при установке значений полей.
	 */
	private boolean applyPattern(Cfg cfg, String patName, ApplyMethod m)
	{
		if(cfg==null) return false;
		if(patName==null) return true;
		
			Cfg pat = patterns.get(patName);
			if(pat==null) 
				throw new IllegalArgumentException("pattern with name '"+patName+"' not found !" );
			if(m==ApplyMethod.ADD || m==ApplyMethod.APPEND || m==ApplyMethod.AFTER || m==ApplyMethod.BEFORE) {
				if(pat.getChild()==null)
					throw new IllegalArgumentException("ApplyMethod : '" + m.name() + "' incompatible with simple(not composit) pattern !" );
				if(m!=ApplyMethod.ADD && cfg.getChild()==null)
					throw new IllegalArgumentException("Pattern with applyMethod : '" + m.name() + "' not allowed for node without child node !" );
				if(m==ApplyMethod.ADD && cfg.getChild()!=null)
					throw new IllegalArgumentException("Pattern with applyMethod : '" + m.name() + "' not allowed for node with child node !" );
			}
			
			switch(m) {
				case SET 	:	applySet(cfg, pat); break;
				case ADD 	:	applyAdd(cfg, pat); break;  
				case APPEND :	applyAdd(cfg, pat); break;  
				case BEFORE :	applyBefore(cfg, pat); break;
				case AFTER :	applyAfter(cfg, pat, m.index); break;
				default	:	throw new IllegalArgumentException("not defined action for ApplyMethod : '" + m.name() + "' !" );
			}
		return true;
	}
	
}
