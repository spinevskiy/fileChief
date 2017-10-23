package psn.filechief;

import psn.filechief.util.bl.FileData;

public interface IFileDataFilter 
{

	boolean accept(FileData file);
}
