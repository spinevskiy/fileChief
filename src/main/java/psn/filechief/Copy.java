package psn.filechief;

import java.io.File;
import java.io.IOException;
import java.util.Date;
//import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psn.filechief.util.Utl;
import psn.filechief.util.bl.FileData;

public class Copy extends LocalSource 
{
	private static Logger log = LoggerFactory.getLogger(Copy.class.getName());
	public static final String SHORT_NAME = "cp";

	public Copy() 
	{
		actionType = ActionType.LOCAL2LOCAL;
		shortName = SHORT_NAME;
	}
	
    public boolean init()
    {
    	boolean ok  = init2();
        return ok;
    }

	public boolean isValid() 
	{
		if(srcDir==null || srcDir.trim().length()==0) {
			log.error(CRIT+" found agent '{}' without srcDir !!!", name);
			return false;
		}
		if(dstDir==null || dstDir.trim().length()==0) {
			log.error(CRIT+" found agent '{}' without dstDir !!!", name);
			return false;
		}
		return true;
	}

	public String makeId() {
		return super.makeIdString("");	
	}
	
	public void setDefaultValues() 
	{
		if(zip==null) zip = false;
		if(gzip==null) gzip = false;
		if(unpack==null) unpack = false;
		if(delayBetween==null) delayBetween = DEFAULT_DELAY;
		if(saveTime==null && !is_moveToNextBlocked())
			saveTime = true;
	}

	public void afterListing() throws IOException {
	}

	public int applyDstFileLimit(int fileCount) throws IOException {
		return getAllowedFilesNumber(fileCount);
	}

	public void beforeLoop() throws IOException {
	}

	public void finallyAction(boolean flag) {
	}
	
	private String makeDstDir(File f) throws IOException
	{
		if(tbddf==null)
			return dstDir;
		Date d = is_dstDirTimeByFile() ? new Date(f.lastModified()) : new Date();
		String curDstDir = tbddf.format(d);
		if(! Utl.makeDirs(curDstDir))
			throw new IOException("makeDstDir: error mkdirs for: '"+curDstDir+"'");
		return curDstDir;
	}

	/**
	 * @param f file to move - original file from srcDir or file from cacheDir
	 * @param fd FileData object for source file
	 * @param dstName new name of the file
	 * @return true if file moved to dstDir
	 */
	protected boolean workWithFileOpt(FileData fd, File f, String dstName) throws IOException 
	{
		if(saveTime!=null) {
			if(!saveTime)
				f.setLastModified(System.currentTimeMillis());
			else if(fd.isTimeUpdated() )
				f.setLastModified(fd.getModTime());
		}	
		String curDstDir = makeDstDir(f);
		File ret = copyOrMoveOpt(f, curDstDir, true, dstName, get_ReplaceExistingFiles());
		if(ret==null)
			return false;
		log.warn("done: from '{}' to '{}', size: {}", new Object[] {fd.getFullName(), ret.getPath(), ret.length()});
		return true;
	}
	
}
