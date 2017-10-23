package psn.filechief.util;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RefUtl {
	public static final String SET = "set";
	public static final String GET = "get";
	public static final int NOT_SUPPORTED = 100000;
	private static final Class<?>[] supported = new Class[]{String.class, 
			int.class, Integer.class, 
			boolean.class, Boolean.class,
			long.class, Long.class,
			short.class, Short.class,
			byte.class, Byte.class};

	private Object object;
	private boolean ignoreUnknown;
	private final Map<String, Method> setters;
	private final Map<String, Method> getters;
	
	public RefUtl(Object o) {
		this(o,false);
	}

	public RefUtl(Object o, boolean ignoreUnknown) {
		object = o;
		this.ignoreUnknown = ignoreUnknown;
		setters = findSetters();
		getters = findGetters();
	}
	
	private int getIndex(Class<?> x) {
		for(int i=0; i<supported.length; i++)
			if(supported[i].equals(x))
				return i;
		return NOT_SUPPORTED;
	}
	
	private Map<String, Method> findSetters() {
		HashMap<String, Method> methods = new HashMap<>();
		for(Method m : object.getClass().getMethods()) {
			String name = m.getName();
			Class<?>[] types = m.getParameterTypes();
			if(!name.startsWith(SET) || types.length!=1)
				continue;
			String propName = uncapFirst(name.substring(SET.length()));
			int second = getIndex(types[0]);
			if(second==NOT_SUPPORTED)
				continue;
			Method exists = methods.get(propName);
			if(exists!=null) {
				int first = getIndex(exists.getParameterTypes()[0]);
				if(first<second)
					continue;
			}
			methods.put(propName, m);
		}
		return Collections.unmodifiableMap(methods);
	}

	private Map<String, Method> findGetters() {
		HashMap<String, Method> methods = new HashMap<>();
		for(Method m : object.getClass().getMethods()) {
			String name = m.getName();
			Class<?>[] types = m.getParameterTypes();
			if(!name.startsWith(GET) || types.length!=0 || m.getReturnType().equals(void.class))
				continue;
			String propName = uncapFirst(name.substring(GET.length()));
			methods.put(propName, m);
		}
		return Collections.unmodifiableMap(methods);
	}
	
	public boolean set(String property, String value) throws IllegalArgumentException, ReflectiveOperationException {
		Method m = setters.get(property);
		if(m==null) {
			if(ignoreUnknown)
				return false;
			throw new IllegalArgumentException("not found setter for property '"+property+"'");
		}	
		invokeSetter(m, value);
		return true;
	}

	public Object get(String property) throws IllegalArgumentException, ReflectiveOperationException {
		Method m = getters.get(property);
		if(m==null) {
			if(ignoreUnknown)
				return null;
			throw new IllegalArgumentException("not found getter for property '"+property+"'");
		}	
		return m.invoke(object);
	}
	
	public boolean hasGetter(String property) {
		return getters.containsKey(property);
	}

	public boolean hasSetter(String property) {
		return setters.containsKey(property);
	}
	
	private void invokeSetter(Method method, String value) throws ReflectiveOperationException, IllegalArgumentException
	{
		Class<?> type = method.getParameterTypes()[0];
		Object po = null;
		try {
			if(type.equals(String.class))
				po = value;
			else if(type.equals(int.class) || type.equals(Integer.class)) {
				po = Integer.parseInt(value);
			} else if(type.equals(boolean.class) || type.equals(Boolean.class)) {
				po = Boolean.parseBoolean(value);
			} else if(type.equals(long.class) || type.equals(Long.class)) {
				po = Long.parseLong(value);
			} else if(type.equals(byte.class) || type.equals(Byte.class)) {
				po = Byte.parseByte(value);
			} else throw new IllegalArgumentException("setter: method='"+method+"', value='"+value+"' - unsupported type '"+type+"'");
			method.invoke(object, new Object[] {po});
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException("string to numeric, method="+method+", value="+value+" : ", e);
		}
	}
	
	private String uncapFirst(String in) {
		if(in==null)
			return null;
		if(in.length()<2)
			return in.toLowerCase();
		return in.substring(0, 1).toLowerCase() + in.substring(1);
	}

}
