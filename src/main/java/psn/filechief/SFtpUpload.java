package psn.filechief;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import psn.filechief.sftp.ISftpTransport;
import psn.filechief.sftp.JschSftpTransport;
import psn.filechief.util.bl.FileData;
import psn.filechief.util.bl.NotRenamedFile;

@XmlAccessorType(XmlAccessType.NONE)
public class SFtpUpload  extends LocalSource 
{
	private static final Logger log = LoggerFactory.getLogger(SFtpUpload.class);
	
	private ISftpTransport sftp = null;
	
	@XmlAttribute
	private String proxyType = null;
	@XmlAttribute
	private String proxyHost = null;
	@XmlAttribute
	private Integer proxyPort = null;
	@XmlAttribute
	private String proxyUser = null;
	@XmlAttribute
	private String proxyPassword = null;
	
    public SFtpUpload() 
    {
    	dstDir = null;
    	actionType = ActionType.LOCAL2REMOTE;
    	shortName = "sftpup";
    }
    
    public void setDstDir(String dstDir) {
    	this.dstDir = null;
    }
   
	public ISftpTransport getSftp() 
	{
		return new JschSftpTransport();		
	}
    
	public boolean init() 
	{
		boolean ret = init2();
    	if( ftpServer!=null && ftpServer.length()>0 ) 
    		sftp = getSftp();
		return ret;
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
		if(ftpServerPort==null) ftpServerPort = 22;
		if(ftpTimeout==null) ftpTimeout = FtpUpload.DEFAULT_TIMEOUT;
		if(reconnect==null)	reconnect = true;
		if(saveTime==null)	saveTime = true;
	}
	
    private boolean makeRemoteDir(String dirname) throws IOException
    {
    	boolean ret = sftp.makeDirectory(dirname);
    	if(ret)
    		log.warn("created remote dir '{}'", dirname);
    	return ret;
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
    			makeRemoteDir(sb.toString());
    			i++;
    			break;
    		}	
    	}
    	for( ; i<newDir.length; i++) {
    		sb.append(newDir[i]);
    		sb.append(ftpSeparator);
    		makeRemoteDir(sb.toString());
    	}
    	return true;
    }

	
    private String[] prevDstDir = null;
	private String makeSFtpDstDir(File f, FileData fd) throws IOException // throws IOException
	{
		if(tbddf==null)
			return ftpDstDir;
		Date d = (saveTime && is_dstDirTimeByFile()) ? new Date(fd.getModTime()) : new Date();
		String curDstDir = tbddf.format(d);
		
		curDstDir = normalizeTail(curDstDir, ftpSeparator);
		
		String[] dstx = null;
		
		if(ftpSeparator==null || ftpSeparator.length()==0)
			makeRemoteDir(curDstDir);
		else {
			dstx = curDstDir.split("\\"+ftpSeparator);
			if(prevDstDir==null) // если это первый файл
			{
				String[] cachex = ftpCacheDir.split("\\"+ftpSeparator);
				compareAndMakeRemoteDir(cachex, dstx);
			} else 
				compareAndMakeRemoteDir(prevDstDir, dstx);
			prevDstDir = dstx;
		}
		return curDstDir;
	}
	
    private boolean isRemoteFileExists(String fullName) throws IOException
    {
    	try {
    		//log.info("isRemoteFileExists, name="+fullName);
			return sftp.listFiles(fullName,null).size()==0 ? false : true;
		} catch (IOException e) {
	    	throw new IOException("isRemoteFileExists - " + fullName + " : " + e.getMessage());
		}
    }
	
	private boolean uploadAndSetTime(String src, String fname, FileData fd) throws IOException
	{
		boolean res = true;
    	sftp.sendFile(src, fname);
    		if(saveTime) 
    			if(!sftp.setModificationTime(fname, fd.getModTime()))
    				if(log.isDebugEnabled())
    					log.debug("setModificationTime : false for '{}'", fname);
		return res;
	}

	
	public boolean deleteRemoteFile(String fname)
	{
		return sftp.delete(fname, false);
	}
	
	 public boolean renameRemoteFile(String from, String to, boolean highErrorLevel, AtomicBoolean timeoutFlag) // throws IOException
	 {
		 String mes = "";
		 boolean ok = false;
		 long t = System.currentTimeMillis();
		 try { 
			 ok = sftp.rename(from,to, false);
		 } catch(Exception e) { 	
			 mes = e.getMessage(); 
		 }
	      	boolean timeout = false;      	
	  		String extraMes = "";
	      	if(!ok) {
	      		if(mes==null || mes.trim().length()==0)
	      			mes = NOT_RENAMED;
	      		if( timeoutFlag!=null) {
	      			timeout = System.currentTimeMillis()-t >= ftpTimeout*1000;
	      			timeoutFlag.set(timeout);
	      		}	
	      		if(timeout)
	      			log.error("failed remote rename file from {} to {}, detected timeout{} . {}", new Object[] {from, to, extraMes, mes});
	      		else if(highErrorLevel)
	      				log.error("CRITICAL : failed remote rename file from {} to {}. {}", new Object[] {from, to, mes});
	      			else
	      				log.warn("failed remote rename file from {} to {}. {}", new Object[] {from, to, mes});
	      	}	
	    	return ok;
	    }
	    
	
	/**
	 * Переименовывает файл на ftp сервере.
	 * Если не удалось и зафиксировен таймаут - заносит файл в чёрный список на отправку.
	 * @param cacheFile старое имя. 
	 * @param dstFile новое имя.
	 * @return null при ошибке, иначе dstFile. 
	 */
	private String renameRemoteFileExt(String cacheFile, String dstFile)
	{
		AtomicBoolean timeoutFlag = new AtomicBoolean(false);
    	if(renameRemoteFile(cacheFile, dstFile, false, timeoutFlag)==true)
    		return dstFile;
    	if(is_smartRemoteRename()) // и включёно продвинутое переименование
    		upBlackList.addToList(new NotRenamedFile(cacheFile, dstFile)); // то заносим файл в чёрный список на отправку.
    	else if( timeoutFlag.get()==false && sftp.isConnected() ) // причина неудачи - не таймаут и есть соединение  
    		deleteRemoteFile(cacheFile); // пытаемся удалить файл из cacheDir на ftp сервере
    	return null;
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
    				if(!deleteRemoteFile(dstFile)) // пытаемся удалить 
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
    	String origName = file.getPath();
        String fname = newName==null ? file.getName(): newName;	
    	String cacheFile = ftpCacheDir+fname;
    	String dstFile = null;
        try {
        	String curDstDir = makeSFtpDstDir(file, fd);
        	NotRenamedFile nrf = is_smartRemoteRename() ? upBlackList.getValue(cacheFile) : null;
        	//Если файл в чёрном списке на выгрузку, значит не удалось его переименовать на сервере в целевой каталог(таймаут).
        	//Но если файла в рабочем каталоге нет, то считаем, что он был успешно переименован - просто ответ не дошёл.
       		if(nrf!=null && !isRemoteFileExists(cacheFile)) { 
       			dstFile = nrf.getDstFile();
       			upBlackList.removeFromList(nrf);
       		}
       		if(dstFile == null) { // если null - не было успешного срабатывания smartRemoteRename, обрабатываем обычным образом  
            	log.info("begin upload: {} , size={}", origName, file.length());
            	uploadAndSetTime(origName, fname, fd); // отправляем файл в cacheDir
            	log.info("ok upload file: {} , size={}", origName, file.length());
        		dstFile = moveToDstDir(cacheFile, curDstDir, fname);
        		if(dstFile!= null && nrf!=null) // успешно, файл из списка - удаляем его из списка 
        			upBlackList.removeFromList(nrf);
        	}
        } catch(IOException e) { 
       	 	log.error("uploadFile: {} , filesInQueue={}", e.getMessage(), statInfo.queueSize);
          }
        finally {
        }
        if(dstFile==null)
        	return null;
        log.info("ok rename {} to {}", cacheFile, dstFile);
        return dstFile;
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

	public String makeId() {
		return super.makeIdString("");
	}

	protected void sftpConnect() throws IOException
	{
		if(!sftp.isConnected()) {
	        sftp.setTimeout(ftpTimeout*1000);
	        sftp.connect(ftpServer, ftpServerPort, ftpUser, ftpPassword);
		}
	}

	public void afterListing() throws IOException 
	{
		sftpConnect();
	}

	public int applyDstFileLimit(int fileCount) throws IOException 
	{
		changeSftpWorkDir(ftpDstDir); // перейдём в целевой каталог 
		List<FileData> lst = sftp.listFiles(null, new IFileDataFilter() {
			@Override
			public boolean accept(FileData f) { return f.isFile(); } ; 
		});
		int cnt = lst.size(); 		// получим кол-во файлов в нём
		if(cnt>= dstFileLimit) {
			log.info("dstFileLimit: {}, files in ftpDstDir: {}, waiting", dstFileLimit, cnt );
			pleaseWait = true;
			return 0;
		} 
		// вычисляем кол-во файлов, которое разрешено скачать
		return Math.min(fileCount, dstFileLimit - cnt);
	}

	private boolean sftpChDir(String dir)
	{
		try {
			sftp.chDir(dir);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	private void changeSftpWorkDir(String workDir) throws IOException  
    {
    	changeSftpWorkDir(workDir, false);    	
    }

	protected String makeErrMessage(String prefix)
	{
    	StringBuilder sb = new StringBuilder(prefix);
    	//sb.append(", sftpUser='").append(ftpUser).append("' code=");
    	//for(String rep : ftp.getReplyStrings())
    	//	sb.append(rep).append(" ");
		return sb.toString();
	}
	
	private void changeSftpWorkDir(String workDir, boolean autoCreate) throws IOException  
    {
    	if( sftpChDir(workDir) == true) 
    		return; 
   		if(autoCreate) 
   		{
   			if( !sftp.makeDirectory(workDir) ) 
   				throw new IOException(makeErrMessage("can't create remote directory '"+workDir+"'"));
   			log.warn("created remote directory: '{}'", workDir);
   			if( sftpChDir(workDir) ) 
   				return;
   		}
    	throw new IOException(makeErrMessage("can't change remote directory to '"+workDir+"'"));
    }
	
	public void beforeLoop() throws IOException {
		try {
			changeSftpWorkDir(ftpCacheDir);
		} catch(IOException e) { // если проблема с переходом в cache каталог, проверяем, а доступен ли целевой каталог
			if(!is_dstDirTimeBased())
				changeSftpWorkDir(ftpDstDir); // если нет - вылетим по исключению
			changeSftpWorkDir(ftpCacheDir, true); // если да, попробуем создать и перейти
		}
	}

	public void finallyAction(boolean flag) 
	{
		if(reconnect==true || flag)
			sftp.disconnect();
	}

	public String getProxyType() {
		return proxyType;
	}

	public void setProxyType(String proxyType) {
		this.proxyType = proxyType;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public Integer getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getProxyUser() {
		return proxyUser;
	}

	public void setProxyUser(String proxyUser) {
		this.proxyUser = proxyUser;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}
	
}
