package psn.filechief.util.bl;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class FileData implements IElement
{
	public static final int T_UNKNOWN = 0;
	public static final int T_FILE = 1;
	public static final int T_DIR = 2;
	
	private String name;
	
	private String fullName;

	private String workFullName;
	
	private long modTime;
	
	private int fileType;
	
	private long size;
	
	private boolean timeUpdated = false;

	private String altName = null;

	private boolean insFlag = true; 
	
	public FileData() 
	{
	}
	
	public FileData(String name, long t, int type, long size) 
	{
		this.name = name;
//		fullName = fName;
		modTime = t;
		fileType = type;
		this.size = size;
	}
	
	public static final FileData getEmpty()
	{
		return new FileData("",0, FileData.T_FILE, 0);
	}
	
	public void setName(String fileName) { this.name = fileName; }
	public String getName() { return name; }
	
	public void setModTime(long fileTime) {
		timeUpdated = true;
		this.modTime = fileTime;
	}
	public long getModTime() { return modTime; }

	public int getFileType() { return fileType;	}
	
	public void setFileType(int fileType) { this.fileType = fileType; }
	@JsonIgnore
	public boolean isFile() {
		return fileType == T_FILE;
	}

	@JsonIgnore
	public boolean isUnknown() {
		return fileType == T_UNKNOWN;
	}
	@JsonIgnore
	public boolean isDirectory() {
		return fileType == T_DIR;
	}

	public long getSize() { return size; }
	public void setSize(long sz) { size = sz; }
	
	public String getFullName() { return fullName; } 
	public void setFullName(String fullName) { this.fullName = fullName; }

	public String getWorkFullName() { return workFullName; }
	public void setWorkFullName(String workFullName) { this.workFullName = workFullName; }

	public boolean isTimeUpdated() { return timeUpdated; }
	public void setTimeUpdated(boolean timeUpdated) { this.timeUpdated = timeUpdated; }

	public boolean dirUpdated() {
		return !fullName.equals(workFullName);
	}
	
	@JsonIgnore
	public String get_AltName() {
		if(altName==null) 
			return getName();
		return altName;
	}
	public String getAltName() {
		return altName;
	}
	public void setAltName(String altName) {
		this.altName = altName;
	}

	@JsonIgnore
	public String getKeyFor() {
		return get_AltName();
	}

	public boolean isInsFlag() {
		return insFlag;
	}

	public void setInsFlag(boolean flag) {
		insFlag = flag;
	}
	
}
