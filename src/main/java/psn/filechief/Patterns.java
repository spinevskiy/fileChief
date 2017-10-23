package psn.filechief;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Patterns 
{
	private static final Logger log = LoggerFactory.getLogger(Patterns.class.getName());	

    public void addChild(Cfg cfg) 
    {
    	if(cfg==null) {
    		log.warn("add pattern: null");
    		return;
    	}
    	StringBuilder sb = new StringBuilder();
    	sb.append("add pattern ").append(cfg.getName());
    	if(cfg.getChild()!=null) {
    		sb.append("[ ");
    		for(Cfg c : cfg.getChildren())
    			sb.append(c.getName()).append(" ");
    		sb.append("]");
    		
    	}
    	log.info(sb.toString());
    	PatternStore.getInstance().addPattern(cfg);
    }
	
}
