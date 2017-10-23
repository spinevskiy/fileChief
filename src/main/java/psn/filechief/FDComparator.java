package psn.filechief;

import java.util.Comparator;

import psn.filechief.util.bl.FileData;

public class FDComparator implements Comparator<FileData> 
{
	public int compare(FileData a, FileData b) 
	{
		long ta = a.getModTime();
		long tb = b.getModTime();
		if(ta<tb) return -1;
		if(ta>tb) return 1;
		String na = a.getName();
		String nb = b.getName();
		if(na!=null && nb!=null)
			return na.compareTo(nb);
		return 0;
	}

}
