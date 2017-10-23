package psn.filechief.sftp;

import java.io.File;
import java.io.IOException;
import java.util.List;

import psn.filechief.IFileDataFilter;
import psn.filechief.util.bl.FileData;

public interface ISftpTransport 
{
	public static final String PROXY_HTTP = "http";
	public static final String PROXY_SOCKS4 = "socks4";
	public static final String PROXY_SOCKS5 = "socks5";

	public void connect(String server, int port, String user, String password) throws IOException;
	public void setTimeout(long timeout);
	public void chDir(String dir) throws IOException;
//	public void setWorkDir(String dir); 
	public List<FileData> listFiles(String dir, IFileDataFilter filter) throws IOException;
	public void retrieveFile(String fileName, File result) throws IOException;
	public void sendFile(String srcFile, String dstFile) throws IOException;
	public boolean rename(String srcName, String dstName, boolean highErrorLevel); // throws IOException;
	public boolean delete(String fileName, boolean highErrorLevel); // throws IOException;
	public void disconnect();
	public boolean isConnected();
	public boolean makeDirectory(String dir);
	public boolean setModificationTime(String file, long time);
	
	public void setProxyType(String type);
	public void setProxyHost(String host);
	public void setProxyPort(int port);
	public void setProxyUserPasswd(String user, String passwd);
	
}
