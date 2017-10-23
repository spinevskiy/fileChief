package psn.filechief.util;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public class Param 
{
	@XmlAttribute(required=true)
	private String name;
	@XmlAttribute(required=true)
	private String value;
	
	public Param() {
		
	}

	public Param(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}
	
	public static Param parseString(String x) {
		x = x.trim();
		int z = x.indexOf("=");
		if(z==-1)
			throw new IllegalArgumentException("Invalid value of parameterString: '"+x+"'");
		String a = x.substring(0, z).trim(); 
		if(a.length()==0)
			throw new IllegalArgumentException("Invalid value of parameterString: '"+x+"'");
		String b = x.substring(z+1).trim(); 
		return new Param(a, b);
	}

}
