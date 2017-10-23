package psn.filechief.sftp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS4;
import com.jcraft.jsch.ProxySOCKS5;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import psn.filechief.IFileDataFilter;
import psn.filechief.util.bl.FileData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JschSftpTransport implements ISftpTransport 
{
	private static Logger log = LoggerFactory.getLogger(JschSftpTransport.class.getName());
	
	private JSch jsch;
	private ChannelSftp sftpChannel = null;
	private Session session = null;
	private long timeout = 60000;
//	private String workDir = "";
	private String connectStr = "";
	//Channel channel = null;
	private String proxyType;
	private String proxyHost;
	private int proxyPort = -1;
	private String proxyUser;
	private String proxyPasswd;
	
	public JschSftpTransport()
	{
		jsch = new JSch();
		String home = System.getProperty("user.home");
		if(home==null) return;
		home = home+"/.ssh";
		String knHosts = home+"/known_hosts";
		String identRsa = home+"/id_rsa";
		String identDsa = home+"/id_dsa";
		try {
			File f = new File(knHosts);
			if(f.exists())
				jsch.setKnownHosts(knHosts);
		} catch (JSchException e) {
			log.error("on setKnownHosts to '{}' : {}", knHosts, getMessage(e));
		}
		//boolean rsa = false;
		try {
			File f = new File(identRsa);
			if(f.exists()) {
				jsch.addIdentity(identRsa);
				//rsa = true;
			}	
		} catch (JSchException e) {
			log.error("on addIdentity(id_rsa) to '{}' : {}", identRsa, getMessage(e));
		}
		//if( rsa ) return;
		try {
			File f = new File(identDsa);
			if(f.exists())
				jsch.addIdentity(identDsa);
		} catch (JSchException e) {
			log.error("on addIdentity(id_dsa) to '{}' : {}", identDsa, getMessage(e));
		}
	}
	
	public static String getMessage(Exception e)
	{
		String m = e.getMessage();
		if(m==null || m.trim().length()==0)
			m = "unknown error";
		return m;
	}

	public boolean isConnected()
	{
		if(sftpChannel==null) return false;
		return !sftpChannel.isClosed();
	//	    System.out.println("exit-status: "+channel.getExitStatus());
	}

	public void chDir(String dir) throws IOException 
	{
		try {
			sftpChannel.cd(dir);
			if(log.isDebugEnabled())
				log.debug("sftp - ok chDir to "+dir);
		} catch (SftpException e) {
			if(log.isDebugEnabled())
				log.debug("sftp chDir ("+dir + ") : ",e);
			throw new IOException("sftp chDir to "+dir+" : " + getMessage(e));
		}
	}
	
//	public void setWorkDir(String dir) 
//	{
//		workDir = dir;
//	}
	
	protected Proxy getProxy()
	{
		if(proxyHost==null)
			return null;
		if(ISftpTransport.PROXY_HTTP.equals(proxyType)) {
			ProxyHTTP proxyHttp;
			if(proxyPort==-1)
				proxyHttp = new ProxyHTTP(proxyHost);
			else 
				proxyHttp = new ProxyHTTP(proxyHost, proxyPort);
			if(proxyUser!=null && proxyPasswd!=null)
				proxyHttp.setUserPasswd(proxyUser, proxyPasswd);
			return proxyHttp;
		}
		if(ISftpTransport.PROXY_SOCKS4.equals(proxyType)) {
			ProxySOCKS4 proxySocks4;
			if(proxyPort==-1)
				proxySocks4 = new ProxySOCKS4(proxyHost);
			else 
				proxySocks4 = new ProxySOCKS4(proxyHost, proxyPort);
			return proxySocks4;
		}
		if(ISftpTransport.PROXY_SOCKS5.equals(proxyType)) {
			ProxySOCKS5 proxySocks5;
			if(proxyPort==-1)
				proxySocks5 = new ProxySOCKS5(proxyHost);
			else 
				proxySocks5 = new ProxySOCKS5(proxyHost, proxyPort);
			if(proxyUser!=null && proxyPasswd!=null)
				proxySocks5.setUserPasswd(proxyUser, proxyPasswd);
			return proxySocks5;
		}
		return null;		
	}

	public void connect(String server, int port, String user, String password) throws IOException 
	{
		Channel channel = null;
		connectStr = new StringBuilder(user).append("@").append(server).append(":").append(port).toString();
		if(log.isDebugEnabled())
			log.debug("sftp - try connect ok to "+connectStr);
		try {
			session = jsch.getSession(user, server, port);
			Proxy proxy = getProxy();
			if(proxy!=null)
				session.setProxy(proxy);
			session.setTimeout((int)timeout);
			UserInfo ui=new XUserInfo(password);
			session.setUserInfo(ui);
			if(password.length()!=0)
				session.setPassword(password);
			session.connect((int) timeout);
			channel = session.openChannel("sftp");
			channel.connect((int) timeout);
			sftpChannel = (ChannelSftp)channel;
			if(log.isDebugEnabled())
				log.debug("sftp connect ok to "+connectStr);
		} catch (JSchException e) {
			log.debug("sftp connect : ",e);
			if(session!=null)
				try {session.disconnect(); } catch(Exception ee) {}
			sftpChannel = null;
			throw new IOException("sftp connect to "+connectStr+" : " + getMessage(e));
		}
	}

	public void disconnect() 
	{
		try { sftpChannel.disconnect(); } catch(Exception e) {}
		try { session.disconnect(); } catch(Exception e) {}
		sftpChannel = null;
	}

	public List<FileData> listFiles(String dir, IFileDataFilter filter) throws IOException 
	{
		@SuppressWarnings("rawtypes")
		Vector vv = null;
		if(dir==null)
			dir = ".";
		List<FileData> ret = new ArrayList<>(); 
		try {
			vv = sftpChannel.ls(dir);
		} catch (SftpException e) {
			if(log.isDebugEnabled())
				log.debug("sftp : ",e.getMessage());
			if(e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE)
				throw new IOException("sftp listFiles : " + getMessage(e));
		}
		if(vv==null) return ret;
		int type;
		for(int ii=0; ii<vv.size(); ii++)
		{
			Object obj=vv.elementAt(ii);
	    	if(obj instanceof LsEntry){
	    		LsEntry le = (LsEntry) obj;
	    		SftpATTRS attr = le.getAttrs();
	    		if(attr.isDir())
	    			type = FileData.T_DIR;
	    		else if(attr.isLink())
	    			type = FileData.T_UNKNOWN;
	    		else type = FileData.T_FILE;
	    		//if(attr.isDir()) continue;
	    		int t = attr.getMTime();
	    		FileData f = new FileData(le.getFilename(),((long)t)*1000, type, attr.getSize());
	    		if(filter==null || filter.accept(f))
	    			ret.add(f);
	    	}
		  }
		return ret;
	}

	public void retrieveFile(String fileName, File result) throws IOException 
	{
		try {
			sftpChannel.get(fileName, result.getPath());
		} catch (SftpException e) {
			log.debug("sftp retrieveFile : ",e);
			throw new IOException("sftp retrieveFile : " + getMessage(e));
		}
	}
	
	public static class XUserInfo implements UserInfo	
	{
		String password;
		
		public XUserInfo(String passw) {
			password = passw;
		}
		
		public String getPassphrase() {
			return null;
		}

		public String getPassword() {
			return password;
		}

		public boolean promptPassphrase(String arg0) {
			return true;
		}

		public boolean promptPassword(String arg0) {
			return true;
		}

		public boolean promptYesNo(String arg0) {
			return true;
		}

		public void showMessage(String arg0) {
		}
		
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;		
	}

	public boolean rename(String srcName, String dstName, boolean highErrorLevel) {
		try {
			sftpChannel.rename(srcName, dstName);
		} catch (SftpException e) {
			if(highErrorLevel)
				log.error("CRITICAL sftp - on rename '{}' to '{}' : {}", new Object[]{ srcName, dstName, getMessage(e)});
			else 
				log.warn("sftp - on rename '{}' to '{}' : {}", new Object[]{ srcName, dstName, getMessage(e)});
			return false;
		}
		return true;
	}

	public boolean delete(String fileName, boolean highErrorLevel) 
	{
		int level = highErrorLevel ? 1 : 0;  
		return delete(fileName, level);
	}

	/**
	 * 
	 * @param fileName 
	 * @param errorLevel logging level on error: 0 WARN, 1 ERROR, 2 CRITICAL
	 * @return true if success
	 */
	private boolean delete(String fileName, int errorLevel) 
	{
		try {
			sftpChannel.rm(fileName);
		} catch (SftpException e) {
			String mes = e.getMessage();
    		if(mes==null || mes.trim().length()==0)
    			mes = "not deleted";
    		String mesPref = (errorLevel==2) ? "CRITICAL " : "";
    		if(errorLevel>0)
    			log.error(mesPref + "sftp deleteFile: '{}' - {}", fileName, mes);
    		else 
    			log.warn("sftp deleteFile: '{}' - {}", fileName, mes);	
			return false;
		}
		return true;
	}

	public boolean makeDirectory(String dir) {
		try {
			sftpChannel.mkdir(dir);
			return true;
		} catch (SftpException e) {
			//e.printStackTrace();
			return false;
		}
	}

	public void sendFile(String srcFile, String dstFile) throws IOException 
	{
		try {
			sftpChannel.put(srcFile, dstFile);
		} catch (SftpException e) {
			log.debug("sftp sendFile : ",e);
			throw new IOException("sftp sendFile (" + dstFile + ") : " + getMessage(e));
		}
	}

	public boolean setModificationTime(String file, long time) {
		try {
			sftpChannel.setMtime(file, (int) (time/1000));
			return true;
		} catch (SftpException e) {
		//	if(log.isDebugEnabled())
			//	log.debug("setModificationTime : false for '{}'", file);
		}
		return false;
	}

	@Override
	public void setProxyType(String type) {
		proxyType = type;
	}

	@Override
	public void setProxyHost(String host) {
		proxyHost = host;
	}

	@Override
	public void setProxyPort(int port) {
		proxyPort = port;
	}

	@Override
	public void setProxyUserPasswd(String user, String passwd) {
		proxyUser = user;
		proxyPasswd = passwd;
	}
	
}
