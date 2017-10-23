package psn.filechief;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CheckedOutputStream;

import javax.xml.bind.annotation.XmlAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psn.filechief.util.Utl;
import psn.filechief.util.bl.FileData;

public abstract class RemoteSource extends Cfg 
{
	private static Logger log = LoggerFactory.getLogger(RemoteSource.class);
	public static final int DEFAULT_DELAY = 30;
	
	@XmlAttribute
	private Boolean createLinks = false;
	
	protected boolean relRename;
	
	abstract public boolean deleteOnServer(String fileName, boolean firstAttempt) throws IOException;
	abstract public boolean renameOnServer(String from, String to, boolean firstAttempt) throws IOException;
	abstract protected boolean downloadFile(FileData src, File file, boolean useCrc, CheckedOutputStream crc);	
	
	public RemoteSource() {
		srcDir = null;
		actionType = ActionType.REMOTE2LOCAL;
		setMoveToNext(false);
		relRename = false;
	}
	
	public void setSrcDir(String srcDir) {
		this.srcDir = null;
	}
	
	public boolean isLocalSource() {
		return false;
	}
	
	public boolean isValid() 
	{
		if(ftpServer==null || ftpServer.length()==0) {
			log.error(CRIT+" found {} agent '{}' without ftpServer", this.getClass().getSimpleName(), name);
			return false;
		}
		if(ftpSrcDir==null || ftpSrcDir.length()==0) {
			log.error(CRIT+" found {} agent '{}' without ftpSrcDir", this.getClass().getSimpleName(), name);
			return false;
		}
		if(dstDir==null || dstDir.length()==0) {
			log.error(CRIT+" found {} agent '{}' without dstDir", this.getClass().getSimpleName(), name);
			return false;
		}
		
		if(is_dstDirTimeBased()) {
			log.error(CRIT+" {} agent '{}' must not use attribute 'dstDirTimeBased'", this.getClass().getSimpleName(), name);
			return false;
		}
		
		return true;
	}
	
	public static boolean deleteFile(File f) 
	{
		boolean ret = f.delete();
		if(ret)
			log.info("deleted {}", f.getPath() );
		return ret;
		
	}

	public boolean workWithFile(FileData fd) throws IOException 
	{
		File file = new File( cacheDir, fd.getName() ); // будем принимать в cacheDir
		boolean useCrc = false;
		CheckedOutputStream crc = null;
		boolean res = downloadFile(fd, file, useCrc, crc); // принимаем
		if(!res) {// retrieve error 
			deleteFile(file);
			// если ошибка получения, то завершаем цикл, т.к на всякий случай лучше заново присоединиться.
			throw new LoopBreakException("Download error");
		}
		// если приняли успешно
		//if(!useCrc || crc==null)
			log.warn("ok retrieve file: {} size: {}", file, file.length());
		//else 
		//	log.warn("ok retrieve file: {} size: {}  checksum: {}", new Object[] {file , file.length(), crc.getChecksum().getValue()});
		File f = unpackFile(file, null, true); // архив распаковываем в тот же каталог, если успешно, то удаляем архив.
		if(f==null) { // ошибка распаковки, надо удалить принятый файл из cacheDir
			deleteFile(file);
			return false;
		}	
		if(!f.getPath().equals(file.getPath())) { // если распаковали
			log.info("unpacked {} to {}, size: {}", new Object[] {fd.getName(), f.getPath(), f.length()});
			fd.setWorkFullName(f.getPath());
		}	
		//Теперь f указывает на принятый файл, либо на результат его распаковки.
		if(zip||gzip) {
			File zipFile = zipper.packSingleFile(f, cacheDir, zip);
			// в любом случае удаляем исходный файл из cacheDir: если запаковали успешно, он не нужен, если ошибка - тоже.
			deleteFile(f);
			if(zipFile==null) { // ошибка паковки
				return false;
				// возможно, тут надо поставить LoopBreakException ? 
				// throw new LoopBreakException("Download error");
			}
			log.info("packed {} to {}, size: {}", new Object[] {f.getName(), zipFile.getPath(), zipFile.length()});
			f = zipFile;
			fd.setWorkFullName(f.getPath());
		}
		// файл успешо принят и распакован/запакован (если нужно)
		//
		Date xd = is_timeByFile() ? new Date(fd.getModTime()) : new Date();
		String dstName = makeName(f.getName(), xd);
		if(is_CreateLinks()) 
		{
			AtomicReference<String> first  = new AtomicReference<String>();
			boolean ret = makeLinks(f, dstName, first);
			if(!ret) { // ошибка создания жёстких ссылок, надо удалить файл f из cacheDir
				deleteFile(f);
				return false;
			}	
			fd.setWorkFullName(first.get());
			return true;
		} else {
			File ret = copyOrMoveOpt(f, dstDir, true, dstName, get_ReplaceExistingFiles());
			if(ret==null) { // ошибка переноса в dstDir, надо удалить файл f из cacheDir
				deleteFile(f);
				return false;
			}
			log.warn("done: from '{}' to '{}', size: {}", new Object[] {curWorkDir + fd.get_AltName(), ret.getPath(), ret.length()});
			fd.setWorkFullName(ret.getPath());
			// rename OK
			return true;
		}
	}
	
    private boolean makeLinks(File from, String dstName, AtomicReference<String> first) throws IOException
    {
    	Cfg x = this;
    	boolean found = false;
    	boolean ret =  false;
    	while((x=x.getChild())!=null) {
    		if( x instanceof LocalSource && this.dstDir.equals(x.srcDir)) {
    			found = true;
    			break;
    		}
    	}
    	if(!found) 
    		return false;
    	int i = 0;
		do 
		{
			String toDir = x.getSrcDir();
			File dst = new File(toDir, dstName);
			dst = checkFileExists(dst, get_ReplaceExistingFiles(), "link", null);
			if(dst==null)
				return false;
			Utl.createHardLink(from, toDir, dst.getName());
            log.warn("done: link from '{}' to '{}', size: {}", new Object[] {from.getPath(), dst.getPath(), dst.length()});
            if(i++==0)
            	first.set(dst.getPath());
			ret = true;
			if(!x.is_MoveToNextDef()) // если выбранный агент не должен передавать файл следующему в цепочке
				break;	// то заканчиваем работу
			x=x.getChild();
		} while(x!=null && x instanceof LocalSource );
		if(ret==true) // если успешно создали линки, удаляем исходный файл.
			deleteFile(from);
		return ret;
    }

	
	//private Long prevLastFileTime = null;
	/*
	protected boolean checkFileDate(long date)
	{
		Long x1 = getStartFileDateLong();
		if(x1 != null) { //
			long t1 = x1;
			Long x2 = statInfo.getLastFileTime();
			if(x2!=null)
				t1 = Math.max(t1, x2.longValue());
			if(date < t1)
				return false;
		}	
		return true;
	}
	*/
	static final String DEL_OK = "ok: remote delete file {}";
	static final String REN_OK = "ok: remote rename file from {} to {}";
	public boolean afterAction(FileData f, boolean fromBlackList) throws IOException
	{
		String altName = f.get_AltName();
		String fName = f.getName();
		if(ftpDstDir==null || ftpDstDir.length()==0) // если не надо оставлять файл на сервере, то 
		{ // 
			if(!super.is_deleteAfter()) // запрет удаления -> включен batchMode, не удаляем.
				return true;
			if(deleteOnServer(altName, !fromBlackList)) { // пытаемся удалить файл на сервере
				if(!fromBlackList)
					log.info(DEL_OK, altName);
				else 
					log.warn(DEL_OK, altName);
				return true;  // ???????
			}
			return false;
		}
		
		// иначе переносим файл в другой каталог на сервере
		String from;
		if(relRename)
			from = altName;
		else
			from = curWorkDir + altName;
		String to = ftpDstDir+fName;
		if(!renameOnServer(from, to, !fromBlackList))
	       	return false;

		if(!fromBlackList)
			log.info(REN_OK, from, to);
		else
			log.warn(REN_OK, from, to);
   		return true;
	}
	
	public Boolean getCreateLinks() {
		return createLinks;
	}
	public void setCreateLinks(Boolean createLinks) {
		this.createLinks = createLinks;
	}
	public boolean is_CreateLinks() {
		return createLinks==null ? false : createLinks;
	}
	
	
}
