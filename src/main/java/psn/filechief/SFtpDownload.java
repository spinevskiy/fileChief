package psn.filechief;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CheckedOutputStream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psn.filechief.sftp.ISftpTransport;
import psn.filechief.sftp.JschSftpTransport;
import psn.filechief.util.Utl;
import psn.filechief.util.bl.FileData;

@XmlAccessorType(XmlAccessType.NONE)
public class SFtpDownload extends RemoteSource 
{
	private static Logger log = LoggerFactory.getLogger(SFtpDownload.class.getName());
	public static final String SHORT_NAME = "sftpdown";
	
	private ISftpTransport sftp = null;
	
	private Filter filter = new Filter();
	
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

	public SFtpDownload() 
	{
		super();
    	actionType = ActionType.REMOTE2LOCAL;
    	shortName = SHORT_NAME;
    	relRename = true;
	}
	
	public boolean init() 
	{
		boolean ret = init2();
    	if( ftpServer!=null && ftpServer.length()>0 ) 
    	{ 
    		sftp = getSftp();
    		sftp.setProxyType(proxyType);
    		sftp.setProxyHost(proxyHost);
    		sftp.setProxyUserPasswd(proxyUser, proxyPassword);
    		if(proxyPort!=null)
    			sftp.setProxyPort(proxyPort);
    	}
		return ret;
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
		if(Utl.isEmpty(cacheDir))
			cacheDir = null;
		setMoveToNext(Boolean.FALSE);
	}

	public List<FileData> getFilesList() throws IOException 
	{
		Filter flt = getFilter();
		flt.clearList();
		if(srcSubDirList==null) {
			sftp.listFiles(null, flt); // получаем список файлов
			return flt.getList();
		}
		for(String x : srcSubDirList) {
			flt.subDir = x;
			sftp.listFiles(x, flt); // получаем список файлов
		}
		return flt.getList();
	}
	
	public ISftpTransport getSftp() 
	{
		return new JschSftpTransport();		
	}

	protected boolean downloadFile(FileData src, File file, boolean useCrc, CheckedOutputStream crc)
	{
		String name = src.getName();
		try {
			log.info("begin retrieve {} to {}", name, file);
			sftp.retrieveFile( src.get_AltName() , file );
			if(src.getSize() > file.length() &&  is_checkFtpFileLength()) 
				throw new IOException("CRITICAL: Invalid length of file '"+file.getName()+"', on server: "+src.getSize() + ", loaded: " + file.length());

			if(saveTime)
				file.setLastModified(src.getModTime());
		} catch(IOException e) { 
			log.error("retrieveFile: {}, filesInQueue={}", e.getMessage(), statInfo.queueSize);
			return false;
		}
		return true;
	}
	
	public String makeId() {
		return super.makeIdString("");
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
	
	private class Filter implements IFileDataFilter, ISmartFilter //Comparator<FileData>,
	{
	
    	List<FileData> list = new ArrayList<>(200); 
		
    	String subDir = null;
    	
    	public void clearList()
    	{
    		subDir = null;
    		if(list!=null)
    			list.clear();
    		list = new ArrayList<>(200);
    	}
    	
    	public List<FileData> getList()
    	{
    		return list;
    	}
		
		public boolean accept(FileData file) 
		{
			if( !file.isFile() || !isValidName(file.getName()))
				return false;
			if(checkFileDate(file.getModTime())== false)
				return false;
			
			file.setFullName(null);
			if(subDir!=null)
				file.setAltName(subDir + getFtpSeparator() + file.getName());
			try {
				fts.updateTimestamp(file);
			} catch (ParseException e) {
				throw new IllegalArgumentException("ParseDate: agent="+getName()+" : "+e.getMessage()); 
			}
			if( !inBlackList(file) )
				list.add(file);
			else if(getStartFileDateLong()!= null) 
			{
				FileData fd = getFromBlackList(file);
				if(fd.getModTime()< file.getModTime()) {
					removeFromBlackList(fd);
					addToBlackList(file);
					list.add(file);
				}
			}
			return false;
		}
	}

	public Filter getFilter() {
		return filter;
	}

	public void beforeListing() throws IOException 
	{
		String cWorkDir = getCurrentSrcDir(ftpSrcDir, ftpSeparator);
		
		if(!sftp.isConnected()) {
			sftp.setTimeout(ftpTimeout*1000);
			sftp.connect(ftpServer, ftpServerPort, ftpUser, ftpPassword);
		}
		sftp.chDir(cWorkDir);
		curWorkDir = cWorkDir;
	}

	public void afterListing() throws IOException {
	}

	public int applyDstFileLimit(int fileCount) throws IOException 
	{
		return getAllowedFilesNumber(fileCount);
	}

	public void beforeLoop() throws IOException {
	}


	public void finallyAction(boolean flag) 
	{
		if(reconnect==true || flag) {
			sftp.disconnect();
		}
	}

	public boolean deleteOnServer(String fileName, boolean firstAttempt) throws IOException 
	{
		return sftp.delete(fileName, firstAttempt);
	}

	public boolean renameOnServer(String from, String to, boolean firstAttempt) throws IOException 
	{
		return sftp.rename(from, to, firstAttempt);
	}
	
}
