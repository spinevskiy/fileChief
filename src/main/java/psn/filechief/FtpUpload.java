package psn.filechief;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import psn.filechief.ftp.CNFtpTransport;
import psn.filechief.ftp.IFtpTransport;
import psn.filechief.util.bl.FileData;
import psn.filechief.util.bl.NotRenamedFile;

public class FtpUpload extends LocalSource 
{
	private static Logger log = LoggerFactory.getLogger(FtpUpload.class);
	public static final int DEFAULT_TIMEOUT = 60; 
	public static final String SHORT_NAME = "ftpup";

	private IFtpTransport ftp; 

	private FileFilter ftpFilter = new FileFilter();

    public FtpUpload() 
    {
    	dstDir = null;
    	actionType = ActionType.LOCAL2REMOTE;
    	shortName = SHORT_NAME;
    }
    
    public void setDstDir(String dstDir) {
    	this.dstDir = null;
    }
  
	public boolean isValid() 
	{
		if(srcDir==null || srcDir.trim().length()==0) {
			log.error(CRIT+" found agent '{}' without srcDir, skipped", name);
			return false;
		}
		if(ftpDstDir==null || ftpDstDir.trim().length()==0) {
			log.error(CRIT+" found agent '{}' without ftpDstDir, skipped", name);
			return false;
		}

		if(ftpCacheDir==null || ftpCacheDir.trim().length()==0) {
			log.error(CRIT+" found agent '{}' without ftpCacheDir, skipped", name);
			return false;
		}
		return true;
	}
	
	public void setDefaultValues() 
	{
		if(zip==null) zip = false;
		if(gzip==null) gzip = false;
		if(unpack==null) unpack = false;
		if(delayBetween==null) delayBetween = DEFAULT_DELAY;
		if(ftpServerPort==null) ftpServerPort = 21;
		if(ftpActiveMode==null)	ftpActiveMode = true;
		if(ftpRemoteVerification==null) ftpRemoteVerification = false;
		if(ftpTimeout==null) ftpTimeout = DEFAULT_TIMEOUT;
		if(ftpFileType==null) setFtpFileType(IFtpTransport.FTP_BINARY);
		if(reconnect==null)	reconnect = true;
		if(saveTime==null)	saveTime = true;
	}
	
	public String makeId() 
	{
		return super.makeIdString("");
	}
	
    public boolean init()
    {
    	boolean ok  = init2();
    	if( ftpServer!=null && ftpServer.length()>0 ) {
    		ftp = new CNFtpTransport();
    		ftp.setServer(ftpServer);
    		ftp.setPort(ftpServerPort);
    		ftp.setUser(ftpUser);
    		ftp.setPassword(ftpPassword);
    		ftp.setActiveMode(ftpActiveMode);
    		ftp.setFileType(ftpFileType);
    		ftp.setRemoteVerificationEnabled(ftpRemoteVerification);
    		ftp.setTimeout(ftpTimeout*1000L);
    		ftp.setServerTimeZone(ftpServerTimeZone);
    	}
        return ok;
    }

    private boolean isRemoteFileExists(String fullName) throws IOException
    {
    	try {
			return ftp.listFiles(fullName).length==0 ? false : true;
		} catch (IOException e) {
	    	throw new IOException("isRemoteFileExists - " + fullName , e);
		}
    }

    private boolean checkRemoteFileSize(String fullName, long size) throws IOException
    {
    	try {
    		FTPFile[] lst = ftp.listFiles(fullName); // запрашиваем информацию по файлу
    		if(lst.length!=1 || !lst[0].isFile()) // если не нашли, бросаем исключение
    			throw new IOException("checkRemoteFileSize - " + fullName + " : uploaded file not found");
    		if(lst[0].getSize()==size) {
    			if(log.isDebugEnabled())
    				log.debug("checkRemoteFileSize - after upload '{}' : expectedSize ( {} ) == actualSize", fullName, size);
    			return true;
    		}
    		log.error("checkRemoteFileSize - after upload '{}' : expectedSize ( {} ) != actualSize( {} )", new Object[] {fullName, size, lst[0].getSize()});
   			return false;
    	} catch (IOException e) {
    		throw new IOException("checkRemoteFileSize - " + fullName, e);
    	}
    }
    
    private boolean compareAndMakeRemoteDir(String[] oldDir, String[] newDir ) throws IOException
    {
    	if(oldDir==null || newDir==null)
    		return false;
    	StringBuilder sb = new StringBuilder();
    	int len = Math.min(oldDir.length, newDir.length);
    	int i;
    	for( i=0; i<len; i++) {
    		sb.append(newDir[i]);
    		sb.append(ftpSeparator);
    		if(!oldDir[i].equals(newDir[i])) {
    			ftp.makeRemoteDir(sb.toString());
    			i++;
    			break;
    		}	
    	}
    	for( ; i<newDir.length; i++) {
    		sb.append(newDir[i]).append(ftpSeparator);
    		ftp.makeRemoteDir(sb.toString());
    	}
    	
    	return true;
    }

    private String[] prevDstDir = null;
	private String makeFtpDstDir(File f, FileData fd) throws IOException // throws IOException
	{
		if(tbddf==null)
			return ftpDstDir;
		Date d = (saveTime && is_dstDirTimeByFile()) ? new Date(fd.getModTime()) : new Date();
		String curDstDir = tbddf.format(d);
		
		curDstDir = normalizeTail(curDstDir, ftpSeparator);
		
		String[] dstx = null;
		
		if(ftpSeparator==null || ftpSeparator.length()==0)
			ftp.makeRemoteDir(curDstDir);
		else {
			String regex = "\\"+ftpSeparator;
			dstx = curDstDir.split(regex);
			if(prevDstDir==null) // если это первый файл
			{
				String[] cachex = ftpCacheDir.split(regex);
				compareAndMakeRemoteDir(cachex, dstx);
			} else 
				compareAndMakeRemoteDir(prevDstDir, dstx);
			prevDstDir = dstx;
		}
		return curDstDir;
	}
	
	/**
	 * Переименовывает файл на ftp сервере.
	 * Если не удалось и зафиксировен таймаут - заносит файл в чёрный список на отправку.
	 * @param cacheFile старое имя. 
	 * @param dstFile новое имя.
	 * @return null при ошибке, иначе dstFile. 
	 * @throws IOException 
	 */
	private String renameRemoteFileExt(String cacheFile, String dstFile) throws IOException
	{
		AtomicBoolean timeoutFlag = new AtomicBoolean(false);
    	if(ftp.renameRemoteFile(cacheFile, dstFile, false, timeoutFlag)==true)
    		return dstFile;
    	if(is_smartRemoteRename() && ftp.getReplyCode()==350) // и включёно продвинутое переименование, и RNFR вернул 350
    		upBlackList.addToList(new NotRenamedFile(cacheFile, dstFile)); // то заносим файл в чёрный список на отправку.
    	else if( timeoutFlag.get()==false && ftp.checkConnection() ) // причина неудачи - не таймаут и есть соединение
    			ftp.delete(cacheFile,1); // пытаемся удалить файл из cacheDir на ftp сервере
    	return null;
	}
	
	/**
	 * 
	 * @param srcFile  
	 * @param dsfFileName 
	 * @param fd srcFile info 
	 * @return
	 * @throws IOException
	 */
	private boolean uploadAndSetTime(File srcFile, String dsfFileName, FileData fd ) throws IOException
	{
		boolean res = ftp.sendFile(srcFile, dsfFileName);
        // после выгрузки файла проверяем размер
        if(res && ftp.isBinary() && !checkRemoteFileSize(dsfFileName, srcFile.length())) { // если не соответствует
        	ftp.delete(dsfFileName, 1);  // пытаемся удалить на сервере (в ftpCacheDir)
        	res = false;  // и сбрасываем признак успеха
        }
    	if(res==false) 
    		throw new IOException(ftp.getErrMessage("Upload error"));
    	try {
    		if(saveTime) 
    			if(!ftp.setModificationTime(dsfFileName, fd.getModTime()))
    				if(log.isDebugEnabled())
    					log.debug("setModificationTime : false for '{}'", dsfFileName);
    	} catch(IOException em) {
    		log.error("ftp - setModificationTime :", em);
    	}
		return res;
	}

	private String moveToDstDir(String cacheFile, String curDstDir, String fname) throws IOException
	{
    	// файл залили в рабочий каталог, перед переносом в целевой надо проверить, нет ли там уже файла с таким же именем
    	int idx;
    	String tryName = curDstDir+fname;
    	String dstFile = tryName;
    	for( idx=1; isRemoteFileExists(dstFile); idx++)
    	{
    		if(idx==1)  // если файл с таким именем существует и это первая проверка
    		{	
    			if(get_ReplaceExistingFiles()) { // если разрешено замещать существующие файлы 
    				if(!ftp.delete(dstFile)) // пытаемся удалить 
    					return null;
    				else 
    					break;
    			}
    			log.error("CRITICAL - remote file already exists : " + dstFile);
    		}	
    		//dstFile = tryName + "." + idx; // формируем новое имя
    		dstFile = makeIdxName(tryName, idx);
    	}
    	if( idx > 1 )
    		log.warn("file {} : change name to '{}'", fname, dstFile);
    	return renameRemoteFileExt(cacheFile, dstFile);
	}
	
    private String uploadFile(File file, String newName, FileData fd) 
    {
        String fname = newName==null ? file.getName(): newName;	
    	String cacheFile = ftpCacheDir+fname;
    	String dstFile = null;
        try {
        	String curDstDir = makeFtpDstDir(file, fd);
        	NotRenamedFile nrf = is_smartRemoteRename() ? upBlackList.getValue(cacheFile) : null;
        	//Если файл в чёрном списке на выгрузку, значит не удалось его перименовать на сервере в целевой каталог(таймаут).
        	//Но если файла в рабочем каталоге нет, то считаем, что он был успешно переименован - просто ответ не дошёл.
       		if(nrf!=null && !isRemoteFileExists(cacheFile)) { 
       			dstFile = nrf.getDstFile();
       			upBlackList.removeFromList(nrf);
       		}
       		if(dstFile == null) { // если null - не было успешного срабатывания smartRemoteRename, обрабатываем обычным образом
       			log.info("begin upload: {} , size={}", fname, file.length());
       			uploadAndSetTime(file, fname, fd); // отправляем файл в cacheDir
       			log.info("ok upload file: {} , size={}", file.getPath(), file.length());
       			dstFile = moveToDstDir(cacheFile, curDstDir, fname);
       			if(dstFile!= null && nrf!=null) // успешно, файл из списка - удаляем его из списка 
       				upBlackList.removeFromList(nrf);
        	}
        } catch(IOException e) { 
       	 	log.error("uploadFile: {} , filesInQueue={}", e.getMessage(), statInfo.queueSize);
          }
        if(dstFile!=null)
        	log.info("ok rename {} to {}", cacheFile, dstFile);
        return dstFile;
    }

	public void afterListing() throws IOException 
	{
		if(!ftp.isConnected())
			ftp.connect();
	}

	public int applyDstFileLimit(int fileCount) throws IOException 
	{
		ftp.chDir(ftpDstDir); // перейдём в целевой каталог 
		int cnt = ftp.listFiles(null,ftpFilter).length; // получим кол-во файлов в нём
		if(cnt>= dstFileLimit) {
			log.info("dstFileLimit: {}, files in ftpDstDir: {}, waiting", dstFileLimit, cnt );
			pleaseWait = true;
			return 0;
		} 
		// вычисляем кол-во файлов, которое разрешено скачать
		return Math.min(fileCount, dstFileLimit - cnt);
	}

	public void beforeLoop() throws IOException 
	{
		try {
			ftp.chDir(ftpCacheDir);
		} catch(IOException e) { // если проблема с переходом в cache каталог, проверяем, а доступен ли целевой каталог
			if(!is_dstDirTimeBased())
				ftp.chDir(ftpDstDir); // если нет - вылетим по исключению
			ftp.chDir(ftpCacheDir, true); // если да, попробуем создать и перейти
		}
	}

	protected boolean workWithFileOpt(FileData fd, File f, String dstName) throws IOException
	{
		String ret = uploadFile(f, dstName, fd);
		long size = f.length(); 
		if(fd.dirUpdated()) // удаляем временный файл.
			deleteFile(f); 
		if(ret==null) // если ошибка отправки, то завершаем цикл, т.к на всякий случай лучше заново присоединиться.
			throw new LoopBreakException("Upload error"); 
		log.warn("done: from '{}' to '{}', size: {}", new Object[] {fd.getFullName(), ret, size});
		return true;
	}
	
	public void finallyAction(boolean flag) 
	{
		if(reconnect==true || flag)
			ftp.disconnect();
	}

	private class FileFilter implements FTPFileFilter  //для проверки кол-ва файлов в целевом каталоге. 
	{
		public boolean accept(FTPFile f) {
			return f.isFile() ? true : false;
		}
	}
	
}
