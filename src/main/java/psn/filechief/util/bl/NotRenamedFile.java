package psn.filechief.util.bl;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class NotRenamedFile implements IElement 
{
	private String cacheFile;
	private String dstFile;
	private boolean insFlag = true;

	public NotRenamedFile() { }
	
	public NotRenamedFile(String cacheFile, String dstFile)
	{
		this.cacheFile = cacheFile;
		this.dstFile = dstFile;
	}
	
	@JsonIgnore
	public String getKeyFor() {
		return cacheFile;
	}

	public boolean isInsFlag() {
		return insFlag;
	}

	public void setInsFlag(boolean flag) {
		insFlag = flag;
	}

	public String getCacheFile() {
		return cacheFile;
	}

	public String getDstFile() {
		return dstFile;
	}

	public void setCacheFile(String cacheFile) {
		this.cacheFile = cacheFile;
	}

	public void setDstFile(String dstFile) {
		this.dstFile = dstFile;
	}

}
