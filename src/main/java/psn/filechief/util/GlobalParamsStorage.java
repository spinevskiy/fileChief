package psn.filechief.util;

public class GlobalParamsStorage extends ParamsStorage 
{
	public static final String NAME_PATTERN = "[A-Za-z][\\w]{1,40}";
	public static final String NAME = "GlobalParams";

	public String getNamePattern() {
		return NAME_PATTERN;
	}

	public String getStorageName() {
		return NAME;
	}

}
