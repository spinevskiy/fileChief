package psn.filechief.util;

public class LocalParamsStorage extends ParamsStorage 
{
	public static final String NAME_PATTERN = ":[A-Za-z][\\w]{1,40}";
	public static final String NAME = "LocalParams";
	public static final String SUPER = ":super";
	private boolean ready = false;
	
	public LocalParamsStorage()
	{
		super();
		logAddParam = false;
	}
	
	public void setLogAddParam(boolean val)
	{
		logAddParam = val;
	}
	
	public String getNamePattern() {
		return NAME_PATTERN;
	}
	
	public String getStorageName() {
		return NAME;
	}
	
	public void clear()
	{
		allParams.clear();
		log.info("local parameters cleared");
	}

	public void set(ParamsStorage store)
	{
		for(String key : store.allParams.keySet())
		{
			String value = store.allParams.get(key);
	    	if(SUPER.equals(key))
	    		throw new IllegalArgumentException("Local parameter '"+SUPER+"' is predefined.");
	    	addParam(new Param(key,value));
		}
	}
	
	public void clearAndSet(ParamsStorage store)
	{
		clear();
		set(store);
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady() {
		this.ready = true;
	}

}
