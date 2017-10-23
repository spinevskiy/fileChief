package psn.filechief;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.UserFactory;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;


public class FCTest {
	public static final String SRC = "tstSrc";
	public static final String DST = "tstDst";
	public static final String CACHE = "tstCache";
	public static final int FILE_CHIEF_PORT = 8771;
	public static final String REL_ROOT = "tests";
	public static final String ROOT = new File(REL_ROOT).getAbsolutePath();
	public static final String STAT_DATA_DIR = new File(ROOT, "testData").getAbsolutePath();
	public static final String CONF_DIR = new File(ROOT, "conf").getAbsolutePath();
	public static final int CONTENT_LENGTH = 4000;
	private static final byte[] TEST_CONTENT = initTestContent();
	private static final byte[] BUFFER = new byte[CONTENT_LENGTH];
	public static final int FTP_PORT = 2221;
	public static final String FTP_ADDR = "127.0.0.1";
	public static final String FTP_USER = "test"; 
	public static final String FTP_PASSWORD = "testpwd"; 
	public static final String FTPD_PATH = "/ftpd"; 
	public static final String FTPD_USER_PATH = FTPD_PATH + "/work";
	public static final String REMOTE_DIR = "data";
	public static final int DEFAULT_TIMEOUT = 60; 
	public static final String FC_CLOSE = "</fileChief>";
	public static final String XML_HEAD = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n";
	public static final String FC_OPEN0 = XML_HEAD+"<fileChief port = '"+FILE_CHIEF_PORT+"' dataDir = '"+STAT_DATA_DIR + "'";
	public static final String FC_OPEN = FC_OPEN0 + " >";
	public static final String FC_OPEN_EXT = FC_OPEN0 + " @ext >";
	
	private static byte[] initTestContent() {
		byte[] x = new byte[CONTENT_LENGTH];
		for(int i=0; i<CONTENT_LENGTH; i++)
			x[i] = (byte) i;
		return x;
	}
	
	public static byte[] getTestContent() {
		TEST_CONTENT[0]++;
		return TEST_CONTENT;
	}
	
	public static boolean checkFileContent(File f) throws IOException {
		try (InputStream is = new BufferedInputStream( new FileInputStream(f))) {
			return checkFileContent(is, TEST_CONTENT);			
		}
	}

	public static boolean checkFileContent(File f, byte[] expected) throws IOException {
		try (InputStream is = new BufferedInputStream( new FileInputStream(f))) {
			return checkFileContent(is, expected);			
		}
	}
	
	public static boolean checkFileContent(InputStream in) throws IOException {
		return checkFileContent(in, TEST_CONTENT);
	}

	public static boolean checkFileContent(InputStream in, byte[] expected) throws IOException {
		int read = in.read(BUFFER, 0, BUFFER.length);
   		if(read != expected.length)
   			throw new IOException("Invalid content length: expected="+expected.length+" actual="+read);
   		if(in.read()!=-1)
   			throw new IOException("Invalid content length: expected="+expected.length+" actual>"+read);
		if(!Arrays.equals(expected, BUFFER))
				return false;
		return true;
	}

	public static boolean checkZipFileContent(File f) throws IOException {
		return checkZipFileContent(f, TEST_CONTENT);
	}
	
	public static boolean checkZipFileContent(File f, byte[] expected) throws IOException {
		try( ZipFile zf = new ZipFile(f) ) {
			Enumeration<? extends ZipEntry> entries = zf.entries();
			if(entries.hasMoreElements())
				try( InputStream is = new BufferedInputStream(zf.getInputStream(entries.nextElement())) ) {
					return checkFileContent(is, expected);
				} 
		}
		return false; 
	}

	public static boolean checkGzipFileContent(File f) throws IOException {
		return checkGzipFileContent(f, TEST_CONTENT);
	}
	
	public static boolean checkGzipFileContent(File f, byte[] expected) throws IOException {
		try(InputStream is = new GZIPInputStream( new FileInputStream(f))) {
			return checkFileContent(is, expected);
		}
	}

	
	public static String makeConfig(String cfgTemplate, String opt, String[][] subst) throws IOException {
		String cfg = cfgTemplate;
		if(opt==null)
			opt = "";
		cfg = cfg.replace("@opt", opt);
		for(String[] r : subst)
			cfg = cfg.replace(r[0], r[1]);
		
		StringBuilder sb = new StringBuilder();
		sb.append(FC_OPEN);
		sb.append(cfg);
		sb.append(FC_CLOSE);
		
		new File(CONF_DIR).mkdirs();

		File cfgFile = new File(CONF_DIR,"testConfig.xml");
		FileWriter w = new FileWriter(cfgFile, false);
		w.write(sb.toString());
   		w.close();
   		return cfgFile.getAbsolutePath();
	}

	public static String makeConfigExt(String cfgTemplate, String ext, String opt, String[][] subst) throws IOException {
		String cfg = cfgTemplate;
		if(opt==null)
			opt = "";
		cfg = cfg.replace("@opt", opt);
		for(String[] r : subst)
			cfg = cfg.replace(r[0], r[1]);

		if(ext==null)
			ext = "";
		StringBuilder sb = new StringBuilder();
		sb.append(FC_OPEN_EXT.replace("@ext", ext));
		sb.append(cfg);
		sb.append(FC_CLOSE);
		
		String conf = sb.toString();
		
		new File(CONF_DIR).mkdirs();

		File cfgFile = new File(CONF_DIR,"testConfig.xml");
		FileWriter w = new FileWriter(cfgFile, false);
		w.write(conf);
   		w.close();
   		return cfgFile.getAbsolutePath();
	}
	
	
	public static File makeFile(String dir, String name, byte[] content) throws IOException {
		File file =  new File(dir,name);
		try ( OutputStream out = new BufferedOutputStream( new FileOutputStream(file, false)) ) {
			if(content==null)
				content = getTestContent(); 
   			out.write(content);
   			out.flush();
		}
   		return file;
	}

	public static File makeFile(String dir, String name) throws IOException {
		return makeFile(dir, name, null);
	}
	
	public static void cleanDir(File d) throws IOException {
		File[] lst = d.listFiles();
		if(lst!=null)
			for(File x: lst)
				Files.delete(Paths.get(x.getAbsolutePath()));
	}
	
	public static void clearAndRemoveDir(File d) throws IOException {
		if(!d.exists())
			return;
		cleanDir(d);
		if(!d.delete())
			throw new IOException("error delete dir " + d.getAbsolutePath());
	}

	public static void clearAndRemoveDir(String d) throws IOException {
		clearAndRemoveDir(new File(d));
	}
	
	public static String makeNewSubDir(String subdir) throws IOException {
		File d = new File(ROOT, subdir);
		if(!d.exists()) {
			if(!d.mkdirs())
				throw new IOException("Not createded dir: " + d.getAbsolutePath());
		}	
		return d.getAbsolutePath();
	}

	public static String makeNewDir(String dir, String subdir) throws IOException {
		File d = new File(dir, subdir);
		if(!d.exists()) {
			if(!d.mkdirs())
				throw new IOException("Not createded dir: " + d.getAbsolutePath());
		}	
		return d.getAbsolutePath();
	}

//	public static String makeNewDir(String dir) throws IOException {
//		return makeNewDir(null, dir);
//	}

	public static FtpServer getFtpServer() throws Exception {
		return getFtpServer(true);
	}
	
	public static FtpServer getFtpServer(boolean writePermission) throws Exception {
		makeNewSubDir(FTPD_PATH);
		String ftpdHomeDir = makeNewSubDir(FTPD_USER_PATH);
		//String remDir = 
		FtpServerFactory srvFactory = new FtpServerFactory();
		ListenerFactory lstFactory = new ListenerFactory();
		lstFactory.setPort(FTP_PORT);
		lstFactory.setServerAddress(FTP_ADDR);
		srvFactory.addListener("default", lstFactory.createListener());
		
		UserFactory usrFactory = new UserFactory();
		usrFactory.setName(FTP_USER);
		usrFactory.setPassword(FTP_PASSWORD);
		usrFactory.setEnabled(true);
		usrFactory.setHomeDirectory(ftpdHomeDir);
		if(writePermission) {
			List<Authority> lst = new ArrayList<>();
			lst.add(new WritePermission("/"));
			usrFactory.setAuthorities(lst);
		}
		srvFactory.getUserManager().save(usrFactory.createUser());
		return srvFactory.createServer();
	}

	public static SshServer getSftpServer() throws Exception {
		return getSftpServer(true);
	}
	
	public static SshServer getSftpServer2(boolean writePermission) throws Exception 
	{
	    SshServer sshd = SshServer.setUpDefaultServer();
	    sshd.setHost(FTP_ADDR);
	    sshd.setPort(FTP_PORT);
	    //sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(  ));
	    AbstractGeneratorHostKeyProvider hostKeyProvider = new SimpleGeneratorHostKeyProvider( new File("hostkey.ser") );
	    hostKeyProvider.setAlgorithm("RSA");
	    sshd.setKeyPairProvider(hostKeyProvider);	    
	    
	    /*
		UserFactory usrFactory = new UserFactory();
		usrFactory.setName(FTP_USER);
		usrFactory.setPassword(FTP_PASSWORD);
		usrFactory.setEnabled(true);
		usrFactory.setHomeDirectory(ftpdHomeDir);
		if(writePermission) {
			List<Authority> lst = new ArrayList<>();
			lst.add(new WritePermission("/"));
			usrFactory.setAuthorities(lst);
		}
	    */
	    //sshd.setPublickeyAuthenticator(new MyPublickeyAuthenticator());

	    List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<>();
	    userAuthFactories.add(new UserAuthPasswordFactory());
	    sshd.setUserAuthFactories(userAuthFactories);
	    

	    sshd.setCommandFactory(new ScpCommandFactory());

	    List<NamedFactory<Command>> namedFactoryList = new ArrayList<>();
	    //SftpSubsystemFactory nc = new SftpSubsystemFactory();
	    namedFactoryList.add(new SftpSubsystemFactory());
	    sshd.setSubsystemFactories(namedFactoryList);

	    try {
	        sshd.start();
	        sshd.open();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    //Thread.sleep(3000);
	    return sshd;
	}

	public static SshServer getSftpServer(boolean writePermission) throws Exception 
	{
		makeNewSubDir(FTPD_PATH);
		final String ftpdHomeDir = makeNewSubDir(FTPD_USER_PATH);

		final SshServer sshd = SshServer.setUpDefaultServer();
		sshd.setHost(FTP_ADDR);
		sshd.setPort(FTP_PORT);
		
    
//		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
	    AbstractGeneratorHostKeyProvider hostKeyProvider = new SimpleGeneratorHostKeyProvider( new File("hostkey.ser") );
	    hostKeyProvider.setAlgorithm("RSA");
	    sshd.setKeyPairProvider(hostKeyProvider);	    

		List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<>();
		userAuthFactories.add(new UserAuthPasswordFactory());
		sshd.setUserAuthFactories(userAuthFactories);

		
		sshd.setCommandFactory(new ScpCommandFactory());
		
	    List<NamedFactory<Command>> namedFactoryList = new ArrayList<>();
	    namedFactoryList.add(new SftpSubsystemFactory());
	    sshd.setSubsystemFactories(namedFactoryList);
		
		sshd.setFileSystemFactory(new VirtualFileSystemFactory(new File(ftpdHomeDir).toPath()));
		
		sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
			@Override
			public boolean authenticate(String user, String password, ServerSession session) {
				if ( FTP_USER.equals(user) && FTP_PASSWORD.equals(password) ) {
					return true;
				}
				return false;
			}
		});
    return sshd;
}
	
	
	public static void changeTime(File f, long offset) {
		f.setLastModified(f.lastModified() + offset);
	}
	
}
