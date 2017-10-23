package psn.filechief;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.ftpserver.FtpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import psn.filechief.ftp.IFtpTransport;

public class FtpUploadTest extends FCTest {

	public static String AGENT = "testFtpUp";
	
	static final String CFG = "<ftpUpload name='@name' srcDir='@srcDir' ftpDstDir='@ftpDstDir' ftpServerPort='@ftpPort' " +
							"ftpServer='@ftpServer' ftpUser='@ftpUser' ftpPassword='@ftpPassword' @opt /> ";
	
	String srcDir;
	String ftpDstDir;
	String dstDir;
	String cacheDir;
	String ftpCacheDir;
	
	private FtpServer server;
	
	String[][] subst;
	
	FtpUpload up;
	
//	@BeforeClass
//	public static void setUpBeforeClass() throws Exception {
//		server = getFtpServer();
//		server.start();		
//	}

//	@AfterClass
//	public static void tearDownAfterClass() throws Exception {
//		server.stop();		
//		clearAndRemoveDir(new File(FTPD_PATH));
//	}

	@Before
	public void setUp() throws Exception {
		srcDir = makeNewSubDir(SRC); 
		cacheDir = makeNewSubDir(CACHE);
		ftpDstDir = "/"+REMOTE_DIR;
		dstDir = makeNewSubDir(FTPD_USER_PATH+ftpDstDir);
		ftpCacheDir = ftpDstDir+"/"+CACHE;
		
		subst = new String[][] { 
			{"@name", AGENT},
			{"@srcDir", srcDir},
			{"@ftpDstDir", ftpDstDir},
			//{"@ftpCacheDir", ftpCacheDir},
			{"@ftpPort", ""+FTP_PORT},
			{"@ftpServer", FTP_ADDR},
			{"@ftpUser", FTP_USER},
			{"@ftpPassword", FTP_PASSWORD}
		};
		up = new FtpUpload();
		server = getFtpServer();
		server.start();		
	}

	@After
	public void tearDown() throws Exception {
		clearAndRemoveDir(dstDir);
		clearAndRemoveDir(srcDir);
		clearAndRemoveDir(cacheDir);
		//clearAndRemoveDir(new File(FTPD_USER_PATH));
		server.stop();		
		clearAndRemoveDir(new File(FTPD_PATH));
		clearAndRemoveDir(STAT_DATA_DIR);
		clearAndRemoveDir(CONF_DIR);
	}

	@Test
	public void testIsValid() {
		up.setFtpDstDir(ftpDstDir);
		up.setSrcDir(srcDir);
		up.setFtpCacheDir(ftpCacheDir);
		up.setFtpServer(FTP_ADDR);
		assertTrue(up.isValid());
		
		up.setSrcDir(null);
		assertFalse(up.isValid());
		
		up.setSrcDir(srcDir);
		up.setFtpDstDir(null);
		assertFalse(up.isValid());

		up.setFtpDstDir(ftpDstDir);
		up.setFtpCacheDir(null);
		assertFalse(up.isValid());
	}
	
	@Test
	public void testSetDefaultValues() {
		up.setDefaultValues();
		assertFalse(up.getZip());
		assertFalse(up.getGzip());
		assertFalse(up.getUnpack());
		assertFalse(up.getFtpRemoteVerification());
		assertEquals(LocalSource.DEFAULT_DELAY, up.getDelayBetween().intValue());
		assertEquals(21, up.getFtpServerPort().intValue());
		assertEquals(FtpUpload.DEFAULT_TIMEOUT, up.getFtpTimeout().intValue());
		assertEquals(IFtpTransport.FTP_BINARY, up.getFtpFileType());
		assertTrue(up.getSaveTime());
		assertTrue(up.getFtpActiveMode());
		assertTrue(up.getReconnect());
	}

	@Test
	public void testAutoMakeDirs() throws IOException {
		
		File src = new File(srcDir);
		File cache = new File(cacheDir);
		clearAndRemoveDir(src);
		clearAndRemoveDir(cache);

		assertFalse(src.exists());
		assertFalse(cache.exists());
		
   		up.setSrcDir(srcDir);
		up.setCacheDir(cacheDir);
		up.init();
		
		assertTrue(src.exists());
		assertTrue(cache.exists());
	}

	@Test
	public void testApplyDstFileLimit() throws IOException {
   		String cfg = makeConfig(CFG, "", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final FtpUpload agent = (FtpUpload) fc.getAgentByName(AGENT);
		
		makeFile(dstDir,"file1"); 
		makeFile(dstDir,"file2"); 
		
		agent.setDstFileLimit(Integer.valueOf(1));
		//agent.connect(true, "/"+REMOTE_DIR);
		// connect
		agent.afterListing();
		int ret = agent.applyDstFileLimit(5);
		assertEquals(0, ret);
		
		agent.setDstFileLimit(Integer.valueOf(4));
		ret = agent.applyDstFileLimit(5);
		//disconnect
		agent.finallyAction(true);;
		assertEquals(2, ret);
	}
	
	
	@Test(timeout=2000)
	public void testUp01() throws IOException {
   		File srcFile = makeFile(srcDir, "s_file_1", getTestContent());
   		String cfg = makeConfig(CFG, "", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
   		assertTrue(srcFile.exists());
   		assertTrue(agent.getFtpCacheDir()!=null);
   		assertTrue(agent.getFtpCacheDir().startsWith(agent.getFtpDstDir()));
   		Thread t = new Thread() { public void run(){ agent.run(); } }; 
   		t.start();
   		t.interrupt();
   		try { t.join(2000); } catch (InterruptedException e1) {}
   		assertFalse(srcFile.exists());
   		File dstFile = new File(dstDir,srcFile.getName());
   		assertTrue(dstFile.exists());
   		assertTrue(checkFileContent(dstFile));
	}

	@Test(timeout=2000)
	public void testUp02() throws IOException {
   		File srcFile =  makeFile(srcDir, "s_file_2");
   		String cfg = makeConfig(CFG, "cacheDir = '' substitute = 's/^s_file/new_file/' ", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg copy = fc.getAgentByName(AGENT);
   		assertTrue(copy.getCacheDir().length()==0);  
   		Thread t = new Thread() { public void run(){ copy.run(); } }; 
   		t.start();
   		t.interrupt();
   		try { t.join(2000); } catch (InterruptedException e1) {}
   		assertFalse(srcFile.exists());
   		File dstFile = new File(dstDir,"new_file_2"); // check new name
   		assertTrue(dstFile.exists());
   		assertTrue(checkFileContent(dstFile));
	}

	@Test(timeout=2000)
	public void testUp03() throws IOException {
   		File srcFile =  makeFile(srcDir, "s_file_3");
   		String cfg = makeConfig(CFG, "cacheDir = '' saveTime = 'true' gzip='true'", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg copy = fc.getAgentByName(AGENT);
   		assertTrue(copy.getFtpCacheDir().startsWith(copy.getFtpDstDir()));
   		Thread t = new Thread() { public void run(){ copy.run(); } }; 
   		t.start();
   		t.interrupt();
   		try { t.join(2000); } catch (InterruptedException e1) {}
   		assertFalse(srcFile.exists());
   		File dstFile = new File(dstDir,srcFile.getName()+".gz"); // check gzip
   		assertTrue(dstFile.exists());
   		assertTrue(checkGzipFileContent(dstFile));
	}

	@Test(timeout=2000)
	public void testUp04() throws IOException, InterruptedException {
   		File srcFile =  makeFile(srcDir, "s_file_4");
   		long ft = srcFile.lastModified() - 120_000;
   		srcFile.setLastModified(ft);
   		String prefix = "pref_";
   		String cfg = makeConfig(CFG, "cacheDir = '' saveTime = 'false' zip='true' prefix = \"'"+prefix+"'\" ", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg copy = fc.getAgentByName(AGENT);
   		assertTrue(copy.getFtpCacheDir().startsWith(copy.getFtpDstDir())); // check cacheDir - not empty !
   		Thread t = new Thread() { public void run(){ copy.run(); } }; 
   		long before = System.currentTimeMillis();
   		Thread.sleep(100);
   		t.start();
   		t.interrupt();
   		t.join(2000);
   		assertFalse(srcFile.exists());
   		File dstFile = new File(dstDir, prefix+srcFile.getName()+".zip"); // check prefix and zip
   		assertTrue(dstFile.exists());
   		assertTrue(checkZipFileContent(dstFile));
   		assertTrue(before < dstFile.lastModified()); // check no saveTime
	}

}
