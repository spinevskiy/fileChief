package psn.filechief.ftp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psn.filechief.Cfg;

public class CNFtpTransport implements IFtpTransport {
	private static Logger log = LoggerFactory.getLogger(CNFtpTransport.class.getName());
	
    private SimpleDateFormat sdf = new SimpleDateFormat(FTP_TIME);

	private FTPClient ftp;
	private boolean loginOk = false;

	private String server = null;
	private int port = 21;
	private String user = null;
	private String password = null;
	private boolean activeMode = true;
	private String serverTimeZone = null;
	private String fileType = FTP_BINARY;

	private int fileTypeI = FTP.BINARY_FILE_TYPE;
	private long timeout = 30000;

	public CNFtpTransport() {
		ftp = new FTPClient();
		timeout = 30000;
		setFileType(FTP_BINARY);
	}
	
	public String getFileType() {
		return fileType;
	}

	@Override
	public void setFileType(String fType) 
	{
		fType = fType.toLowerCase();
		if(FTP_BINARY.startsWith(fType)) {
			fType = FTP_BINARY;
			fileTypeI = FTP.BINARY_FILE_TYPE;
		} else if(FTP_ASCII.startsWith(fType)) {
			fType = FTP_ASCII;
			fileTypeI = FTP.ASCII_FILE_TYPE;
		} else { 
			log.error("invalid FtpFileType '{}' , replace to '{}'", fType, FTP_BINARY);
			fType = FTP_BINARY;
			fileTypeI = FTP.BINARY_FILE_TYPE;
		}
		fileType = fType;
	}

	@Override
	public boolean isBinary() {
		return fileTypeI==FTP.BINARY_FILE_TYPE;
	}

	/**
     * соединяется и устанавливает Active/Passive Mode, BINARY_FILE_TYPE
     * @throws IOException при любой ошибке
     */
	@Override
    public void connect() throws IOException
    {
    	ftp.setDefaultTimeout((int)timeout);
    	ftp.setConnectTimeout((int)timeout); 
    	if(serverTimeZone!=null && serverTimeZone.length()>0) {
    		 FTPClientConfig config = new FTPClientConfig();
    		 config.setServerTimeZoneId(serverTimeZone);
    		 ftp.configure(config);
    	}
	    ftp.connect(server, port);
	    if(log.isDebugEnabled())
	    	log.debug("connected to " +  server);
	    if(ftp.login( user, password )==false) 
	    	throw new FTPConnectionClosedException(getErrMessage("login error"));
	    
	    loginOk = true;
	    log.debug("login ok");
	    ftp.setDataTimeout((int)timeout);
	    if(log.isTraceEnabled())
	    	log.trace("setting ftpFileTypeI="+fileTypeI);
	    if( ftp.setFileType(fileTypeI) == false ) 
	    	throw new FTPConnectionClosedException(getErrMessage("can't set fileType="+fileType));
	    if(activeMode) ftp.enterLocalActiveMode();
    		else ftp.enterLocalPassiveMode();
    }

	@Override
	public int getReplyCode() {
		return ftp.getReplyCode();
	}
	
	@Override
	public FTPFile[] listFiles(String dir, FTPFileFilter filter) throws IOException {
		return ftp.listFiles(dir, filter);
	}

	@Override
	public FTPFile[] listFiles(String dir) throws IOException {
		return ftp.listFiles(dir);
	}

	@Override
    public boolean checkConnection() throws IOException {
    	return isConnected() && ftp.isAvailable() && ftp.sendNoOp();
    }
	
	@Override
    public boolean makeRemoteDir(String dirname) throws IOException
    {
    	boolean ret = ftp.makeDirectory(dirname);
    	if(ret)
    		log.warn("created remote dir '{}'", dirname);
    		//throw new IOException("can't create remote directory '"+workDir+"' , code="+ftp.getReplyCode());
    	return ret;
    }
	
	@Override
	public void retrieveFile(String fileName, File result) throws IOException {
		try( OutputStream fos = new FileOutputStream( result ) ) {  
			boolean res = ftp.retrieveFile( fileName, fos );
			fos.flush();
			if(res==false) 
				throw new IOException(getErrMessage("retrieve error"));
		} 
	}

	@Override
	public boolean sendFile(File srcFile, String dstFile) throws IOException {
		try( InputStream fis = new BufferedInputStream (new FileInputStream( srcFile )) ) {
			return ftp.storeFile( dstFile, fis );
		}
	}

	public String getFtpTime(Date d) {
		return sdf.format(d);	
	}

	public String getFtpTime(long d) {
		return getFtpTime(new Date(d));	
	}
	
	@Override	
	public boolean setModificationTime(String fileName, long time) throws IOException {
		return ftp.setModificationTime(fileName, getFtpTime(time));
	}
	
	@Override
	public boolean delete(String fileName, int errorLevel) {
		return deleteRemoteFile(fileName, errorLevel);
	}

	@Override
	public boolean delete(String fileName) {
		return deleteRemoteFile(fileName, 2);
	}
	
	@Override
	public String getErrMessage(String prefix)
	{
    	StringBuilder sb = new StringBuilder();
    	if(prefix!=null)
    		sb.append(prefix).append(", ");
    	sb.append("ftpUser='").append(user).append("' code=").append(ftp.getReplyCode()).append(", reply= ");
    	for(String rep : ftp.getReplyStrings())
    		sb.append(rep).append(" ");
		return sb.toString();
	}
    
	@Override
    public void chDir(String workDir) throws IOException  
    {
    	chDir(workDir, false);    	
    }

	@Override
    public void chDir(String workDir, boolean autoCreate) throws IOException  
    {
    	if( ftp.changeWorkingDirectory( workDir ) == true) return; 
   		if(autoCreate) 
   		{
   			if( !ftp.makeDirectory(workDir) ) 
   				throw new IOException(getErrMessage("can't create remote directory '"+workDir+"'"));
   			log.warn("created remote directory: '{}'", workDir);
   			if( ftp.changeWorkingDirectory(workDir) ) 
   				return;
   		}
    	throw new IOException(getErrMessage("can't change remote directory to '"+workDir+"'"));
    }
    
	@Override
    public void disconnect()
    {
    	if(ftp==null) return;
    	if(loginOk) 
    		try { ftp.logout(); } 
    			catch(Exception e) { log.warn("logout:",e); }
    	if(ftp.isConnected())
    		try { ftp.disconnect(); } 
    			catch(Exception e) { log.warn("disconnect:",e);}
    	loginOk = false;
    	log.debug("disconnected");	
    }

	@Override
    public boolean isConnected() {
    	return loginOk;
    }
	
    /**
     * Удаляет файл на сервере
     * @param fname имя файла
     * 
     */
    private boolean deleteRemoteFile(String fname, int errorLevel)
    {
    	String mes = "";
    	boolean del = false;
    	try { 
    	  del = ftp.deleteFile(fname);
    	} catch(Exception e) { 	
    		mes = e.getMessage(); 
    	}
    	if(!del) {
    		if(mes==null || mes.trim().length()==0)
    			mes = Cfg.NOT_DELETED;
    		String mesPref = (errorLevel==2) ? Cfg.CRIT3 + " " : "";
    		if(errorLevel>0)
    			log.error(mesPref + Cfg.delErrMess, fname , mes);
    		else
    			log.warn(Cfg.delErrMess, fname, mes);
    	}	
    	return del;
    }

/*    
    public boolean deleteRemoteFile(String fname)
    {
    	return deleteRemoteFile(fname, 2);
    }
    
    public boolean renameRemoteFile(String from, String to)
    {
    	return renameRemoteFile(from, to, true, null); 
    }
*/    
	@Override
	public boolean renameRemoteFile(String from, String to, boolean highErrorLevel, AtomicBoolean timeoutFlag) // throws IOException
    {
    	String mes = "";
    	boolean ok = false;
    	long t = System.currentTimeMillis();
    	try { 
      	  ok = ftp.rename(from,to);
      	} catch(Exception e) { 	
      		mes = e.getMessage();
      	}
      	boolean ftimeout = false;      	
  		String extraMes = "";
      	if(!ok) {
      		if(mes==null || mes.trim().length()==0)
      			mes = Cfg.NOT_RENAMED;
      		if( timeoutFlag!=null) {
      			ftimeout = System.currentTimeMillis()-t >= timeout;
      			timeoutFlag.set(ftimeout);
      		}	
      		if(ftimeout)
      			log.error("failed remote rename file from {} to {}, detected timeout{}, ReplyCode={}. {}", new Object[] {from, to, extraMes, ftp.getReplyCode(), mes});
      		else if(highErrorLevel)
      				log.error("CRITICAL : failed remote rename file from {} to {}, ReplyCode={}. {}", new Object[] {from, to, ftp.getReplyCode(), mes});
      			else
      				log.warn("failed remote rename file from {} to {}, ReplyCode={}. {}", new Object[] {from, to, ftp.getReplyCode(), mes});
      	}	
    	return ok;
    }
	
	public String getFtpServerTimeZone() {
		return serverTimeZone;
	}

	@Override
	public void setServerTimeZone(String ftpServerTimeZone) {
		this.serverTimeZone = ftpServerTimeZone;
	}

	public String getServer() {
		return server;
	}

	@Override
	public void setServer(String server) {
		this.server = server;
	}

	public int getPort() {
		return port;
	}

	@Override
	public void setPort(int port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	@Override
	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isActiveMode() {
		return activeMode;
	}

	@Override
	public void setActiveMode(boolean activeMode) {
		this.activeMode = activeMode;
	}

	public long getTimeout() {
		return timeout;
	}

	@Override
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public void setRemoteVerificationEnabled(boolean verify) {
		ftp.setRemoteVerificationEnabled(verify);
	}

	
}

