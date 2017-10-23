package psn.filechief;

import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psn.filechief.util.Utl;
import psn.filechief.util.bl.FileData;

public class Linker extends LocalSource 
{
	private static final Logger log = LoggerFactory.getLogger(Linker.class.getName());
	public static final String SHORT_NAME = "link";  
	
	public Linker() 
	{
		actionType = ActionType.LOCAL2LOCAL;
		shortName = SHORT_NAME;
	}
	
	static boolean isLinker(Cfg cfg)
	{
		return SHORT_NAME.equals(cfg.shortName);
	}
	
	@Override
    public boolean init()
    {
		setDstDir(null);
		setCacheDir(null);
		setDstDirTimeBased(null);
		setDstDirTimeByFile(null);
		setDstFileLimit(null);
		//this.setSuffix(null);
		//this.setPrefix(null);
		//this.setSubstitute(null);
		//this.setTimeStampInName(null);
		setSaveTime(null);
		setMoveToNext(false);
    	boolean ok  = init2();
        return ok;
    }
	
	@Override
	public void setDefaultValues() 
	{
		setZip(false);
		setGzip(false);
		setUnpack(false);
		setMoveToNext(false);
		if(delayBetween==null) delayBetween = 5;
		//if(saveTime==null)	saveTime = false;
	}
	
	@Override
	public boolean isValid() 
	{
		if(srcDir==null || srcDir.trim().length()==0) {
			log.error(CRIT+" found agent '{}' without srcDir !!!", name);
			return false;
		}
		return true;
	}
	
	@Override
	protected boolean workWithFileOpt(FileData fd, File f, String dstName) throws IOException 
	{
		boolean ret = false;
		Cfg cfg = this;
		while((cfg=cfg.getChild())!=null )
		{
			String toDir = cfg.getSrcDir();
			File dst = new File(toDir, dstName);
			dst = checkFileExists(dst, get_ReplaceExistingFiles() , shortName, null);
			if(dst==null)
				break;
			Utl.createHardLink(f, toDir, dst.getName());
            log.warn("done: link from '{}' to '{}', size: {}", new Object[] {fd.getFullName(), dst.getPath(), dst.length()});
			ret = true;
			if(!cfg.is_MoveToNextDef()) // если выбранный агент не должен передавать файл следующему в цепочке
				break;	// то заканчиваем работу
		}
		return ret;
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
