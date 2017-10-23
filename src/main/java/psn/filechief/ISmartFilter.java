package psn.filechief;

import java.util.List;

import psn.filechief.util.bl.FileData;

public interface ISmartFilter 
{
	public void clearList();
	public List<FileData> getList();
}
