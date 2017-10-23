package psn.filechief;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CheckedOutputStream;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psn.filechief.ftp.CNFtpTransport;
import psn.filechief.ftp.IFtpTransport;
import psn.filechief.util.Utl;
import psn.filechief.util.bl.FileData;

public class FtpDownload extends RemoteSource  
{
	private static Logger log = LoggerFactory.getLogger(FtpDownload.class.getName());
	public static final String SHORT_NAME = "ftpdown";
	
	private IFtpTransport ftp; 
	
	private Filter filter = new Filter(); 
	
	public FtpDownload()
	{
    	actionType = ActionType.REMOTE2LOCAL;
    	shortName = SHORT_NAME;
	}

	public String makeId() {
		return super.makeIdString("");
	}
	
	public void setDefaultValues() 
	{
		if(zip==null) zip = false;
		if(gzip==null) gzip = false;
		if(unpack==null) unpack = false;
		if(delayBetween==null) delayBetween = 30;
		if(ftpServerPort==null) ftpServerPort = 21;
		if(ftpActiveMode==null)	ftpActiveMode = true;
		if(ftpRemoteVerification==null) ftpRemoteVerification = false;
		if(ftpTimeout==null) ftpTimeout = FtpUpload.DEFAULT_TIMEOUT;
		if(ftpFileType==null) setFtpFileType(IFtpTransport.FTP_BINARY); 
		if(reconnect==null)	reconnect = true;
		if(saveTime==null)	saveTime = true;
		if(Utl.isEmpty(cacheDir))
			cacheDir = null;
		setMoveToNext(Boolean.FALSE);
	}

	public List<FileData> getFilesList() throws IOException 
	{
		Filter flt = getFilter();
		flt.clearList();
		try {
			if(srcSubDirList==null) {
				ftp.listFiles(null, flt); // получаем список файлов
			} else {
				for(String x : srcSubDirList) {
					flt.subDir = x;
					ftp.listFiles(x, flt); // получаем список файлов
				}
			}
		} catch(NullPointerException e) {
			log.error("getFileList:", e);
		}
		flt.subDir = null;
		return flt.getList();
	}
	
	protected boolean downloadFile(FileData src, File file, boolean useCrc, CheckedOutputStream crc)
	{
		boolean res = false;
		try {  
			log.info("begin retrieve {} to {}", src.getName(), file);
			String loadName = src.get_AltName(); 
			ftp.retrieveFile( loadName, file);
			if(is_checkFtpFileLength() && src.getSize()!=file.length() && ftp.isBinary()) 
				throw new IOException("CRITICAL: Invalid length of file '"+file.getName()+"', on server: "+src.getSize() + ", loaded: " + file.length());
			if(saveTime)
				file.setLastModified(src.getModTime());
			res = true;
		} catch(IOException e) { 
			log.error("retrieveFile: "+e.getMessage()+" , filesInQueue="+statInfo.queueSize, e);
		}
		return res;
	}
	

	public boolean init() 
	{
		boolean ret = init2();
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
		return ret;
	}
	
	public boolean deleteOnServer(String fileName, boolean firstAttempt) throws IOException 
	{
		int x = firstAttempt ? 1 : 0;
		return ftp.delete(fileName, x);
	}
	
	public boolean renameOnServer(String from, String to, boolean firstAttempt) throws IOException 
	{
		return ftp.renameRemoteFile(from, to, firstAttempt, null);
	}
	
	private class Filter implements FTPFileFilter  
	{
    	private List<FileData> list = new ArrayList<>(200);
    	//private boolean filtered = false;
    	String subDir = null;
    	
		
    	public void clearList()
    	{
    		//filtered = false;
    		subDir = null;
    		if(list!=null)
    			list.clear();
    		list = new ArrayList<>(200);
    	}
    	
    	public List<FileData> getList()
    	{
    		return list;
    	}
    	
    	@Override
		public boolean accept(FTPFile f) 
		{
    		//filtered = true;
			if( !f.isFile() || !isValidName(f.getName()))
					return false;
			if(checkFileDate(f.getTimestamp().getTimeInMillis()) == false)
				return false;
			FileData d = new FileData(f.getName(), f.getTimestamp().getTimeInMillis(), FileData.T_FILE, f.getSize());
			d.setFullName(null);
			if(subDir!=null)
				d.setAltName(subDir + getFtpSeparator() + d.getName());
			try {
				fts.updateTimestamp(d);
			} catch (ParseException e) {
				throw new IllegalArgumentException("ParseDate: agent="+getName()+" : "+e.getMessage()); 
			} 
			if(!inBlackList(d) )
				list.add(d);
			else if(getStartFileDateLong()!= null) 
			{
				FileData fd = getFromBlackList(d);
				if(fd.getModTime() < d.getModTime()) {
					removeFromBlackList(fd);
					addToBlackList(d);
					list.add(d);
				}
			}
			return false;
		}

		//public boolean isFiltered() {
		//	return filtered;
		//}
	}

	public Filter getFilter() {
		return filter;
	}
	
	public void beforeListing() throws IOException 
	{
		String workFtpSrcDir = getCurrentSrcDir(ftpSrcDir, ftpSeparator);
		if(!ftp.isConnected())	
			ftp.connect();
		try {
			ftp.chDir(workFtpSrcDir);
			curWorkDir = workFtpSrcDir;
		} catch (IOException e) {
			if(!isSrcDirTimeBased() || workFtpSrcDir==null || curWorkDir==null || curWorkDir.equals(workFtpSrcDir) ) throw e;
				ftp.chDir(curWorkDir);
		}
	}

	public void afterListing() throws IOException 
	{
	}

	public int applyDstFileLimit(int fileCount) throws IOException 
	{
		return getAllowedFilesNumber(fileCount);
	}

	public void beforeLoop() throws IOException {
	}

	public void finallyAction(boolean flag) 
	{
		if(reconnect==true || flag)
			ftp.disconnect();
	}

}
