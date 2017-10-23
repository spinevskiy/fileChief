package psn.filechief.ftp;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public interface IFtpTransport {
	public static final String FTP_BINARY = "binary";
	public static final String FTP_ASCII = "ascii";
	public static final String FTP_EDCDIC = "ebcdic";
	public static final String FTP_LOCAL = "local";
	public static final String FTP_TIME = "yyyyMMddhhmmss";

	/**
	 * 
	 * @param timeout - connect and data timeouts, milliseconds
	 */
	public void setTimeout(long timeout);
	public void setRemoteVerificationEnabled(boolean verify);
	public void setServer(String server); 
	public void setPort(int port); 
	public void setUser(String user); 
	public void setPassword(String password); 
	public void setActiveMode(boolean amode);
	public void setServerTimeZone(String ftpServerTimeZone);
	public void setFileType(String fType);
	
	public void connect() throws IOException;
	public void disconnect();
	
	public void chDir(String dir) throws IOException;
	public void chDir(String workDir, boolean autoCreate) throws IOException;	

	public boolean isBinary();
	public boolean isConnected();
	public boolean checkConnection() throws IOException;
	public int getReplyCode();
	
	public FTPFile[] listFiles(String dir, FTPFileFilter filter) throws IOException;
	public FTPFile[] listFiles(String dir) throws IOException;
	
	public void retrieveFile(String fileName, File result) throws IOException;
	public boolean sendFile(File srcFile, String dstFile) throws IOException;
	
    public boolean renameRemoteFile(String from, String to, boolean highErrorLevel, AtomicBoolean timeoutFlag);
    
	public boolean delete(String fileName, int errorLevel); // throws IOException;
	public boolean delete(String fileName);	
	
    public boolean makeRemoteDir(String dirname) throws IOException;

	public boolean setModificationTime(String fileName, long time) throws IOException;

	public String getErrMessage(String prefix);
	
}
