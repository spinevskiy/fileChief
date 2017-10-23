package psn.filechief;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.annotation.XmlAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psn.filechief.util.Utl;
import psn.filechief.util.bl.FileData;

public class Demux extends LocalSource 
{
	private static Logger log = LoggerFactory.getLogger(Demux.class.getName());
	public static final String SPLIT = "\\s+;\\s+";
	public static final String SHORT_NAME = "demux";  

	@XmlAttribute
	private String dstDirList = null;
	/**
	 * целевые каталоги
	 */
	private String[] dstDirs = null;
	/**
	 * номер каталога, в который переместили последний файл.
	 */
	private int index = -1;
	
	public Demux() 
	{
		actionType = ActionType.LOCAL2LOCAL;
		shortName = SHORT_NAME;
	}	

	public String getDstDirList() {
		return dstDirList;
	}

	public void setDstDirList(String dstDirList) {
		this.dstDirList = dstDirList;
	}
	
	private int getNextIndex(int idx) {
		return (++idx>=0 && idx<dstDirs.length) ? idx : 0;
	}
	
	private String getNextDir() throws LoopBreakException
	{
		int ind = getNextIndex(index);
		if(dstFileLimit==null || dstFileLimit <= 0) {
			index = ind;
			return dstDirs[index];
		}	
		int first = ind;  
		do { // перебираем все каталоги, начиная со следующего за dstDirs[index]. 
			int cnt = getFileCount(dstDirs[ind]);
			if( cnt < dstFileLimit) {
				index = ind;
				return dstDirs[index];
			}
			if(log.isDebugEnabled())
				log.debug("dstFileLimit: {}, in dstDir:'{}' files: {}, waiting", new Object[] {dstFileLimit, dstDirs[ind], cnt });
			ind = getNextIndex(ind);
		} while(ind!=first); 
		throw new LoopBreakException("dstFileLimit - no valid dir"); // если не нашли непереполненных каталогов - прерываем цикл обработки.
	}
	
	@Override
	protected boolean workWithFileOpt(FileData fd, File f, String dstName) throws IOException 
	{
		String toDir = getNextDir();
		File dst = copyOrMoveOpt(f,toDir,true, dstName, get_ReplaceExistingFiles());
		if(dst==null)
			return false;
        log.warn("done: move from '{}' to '{}', size: {}", new Object[] {fd.getFullName(), dst.getPath(), dst.length()});
		return true;
	}

	@Override
	public boolean init() {
		setDstDir(null);
		setCacheDir(null);
		setDstDirTimeBased(null);
		setDstDirTimeByFile(null);
		setSaveTime(null);
		setMoveToNext(false);
		if(dstDirList!=null)
			dstDirs = dstDirList.split(SPLIT);
    	boolean ok  = init2();
        return ok;
	}

	@Override
	public boolean isValid() {
		if(Utl.isEmpty(srcDir)) {
			log.error(CRIT+" found agent '{}' without srcDir !!!", name);
			return false;
		}
		if(Utl.isEmpty(dstDirList)) {
			log.error(CRIT+" found demux agent '{}' without dstDirList !!!", name);
			return false;
		}
		return true;
	}

	@Override
	public void setDefaultValues() {
		setZip(false);
		setGzip(false);
		setUnpack(false);
		setMoveToNext(false);
		if(delayBetween==null) delayBetween = 5;
	}

	@Override
	public String makeId() {
		return super.makeIdString("");
	}

	@Override
	public void afterListing() throws IOException {
	}

	@Override
	public int applyDstFileLimit(int fileCount) throws IOException {
		return fileCount;
	}

	@Override
	public void beforeLoop() throws IOException {
	}

	@Override
	public void finallyAction(boolean flag) {
	}

}
