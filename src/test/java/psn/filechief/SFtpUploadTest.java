package psn.filechief;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.sshd.server.SshServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SFtpUploadTest extends FCTest {

	public static String AGENT = "testSftpUp";
	
	static final String CFG = "<sftpUpload name='@name' srcDir='@srcDir' ftpDstDir='@ftpDstDir' ftpServerPort='@ftpPort' " +
							"ftpServer='@ftpServer' ftpUser='@ftpUser' ftpPassword='@ftpPassword' @opt /> ";
		
	private SshServer server;
	
	String srcDir;
	String ftpDstDir;
	String dstDir;
	String cacheDir;
	String ftpCacheDir;
	
	String[][] subst;
	
	SFtpUpload up;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		clearAndRemoveDir(new File(FtpDownloadTest.FTPD_PATH));
	}

	@Before
	public void setUp() throws Exception {
		srcDir = makeNewSubDir(SRC); 
		cacheDir = makeNewSubDir(CACHE);
		ftpDstDir = "/"+FtpDownloadTest.REMOTE_DIR;
		dstDir = makeNewSubDir(FtpDownloadTest.FTPD_USER_PATH+ftpDstDir);
		ftpCacheDir = ftpDstDir+"/"+CACHE;
		
		subst = new String[][] { 
			{"@name", AGENT},
			{"@srcDir", srcDir},
			{"@ftpDstDir", ftpDstDir},
			//{"@ftpCacheDir", ftpCacheDir},
			{"@ftpPort", ""+FtpDownloadTest.FTP_PORT},
			{"@ftpServer", FtpDownloadTest.FTP_ADDR},
			{"@ftpUser", FtpDownloadTest.FTP_USER},
			{"@ftpPassword", FtpDownloadTest.FTP_PASSWORD}
		};
		up = new SFtpUpload();
		server = getSftpServer();
		server.start();
	}

	@After
	public void tearDown() throws Exception {
		clearAndRemoveDir(new File(dstDir));
		clearAndRemoveDir(new File(srcDir));
		clearAndRemoveDir(new File(cacheDir));
		server.stop();
		//clearAndRemoveDir(new File(FtpDownloadTest.FTPD_USER_PATH));
		clearAndRemoveDir(STAT_DATA_DIR);
		clearAndRemoveDir(new File(FtpDownloadTest.FTPD_PATH));
		clearAndRemoveDir(CONF_DIR);
	}

	@Test
	public void testIsValid() {
		up.setFtpDstDir(ftpDstDir);
		up.setSrcDir(srcDir);
		up.setFtpCacheDir(ftpCacheDir);
		up.setFtpServer(FtpDownloadTest.FTP_ADDR);
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
		assertEquals(LocalSource.DEFAULT_DELAY, up.getDelayBetween().intValue() );
		assertEquals(22, up.getFtpServerPort().intValue());
		assertEquals(FtpUpload.DEFAULT_TIMEOUT, up.getFtpTimeout().intValue());
		assertTrue(up.getSaveTime());
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
   		final SFtpUpload agent = (SFtpUpload) fc.getAgentByName(AGENT);
		
		makeFile(dstDir,"file1"); 
		makeFile(dstDir,"file2"); 
		
		agent.setDstFileLimit(Integer.valueOf(1));
		// connect
		agent.afterListing();
		int ret = agent.applyDstFileLimit(5);
		assertEquals(0, ret);
		
		agent.setDstFileLimit(Integer.valueOf(4));
		ret = agent.applyDstFileLimit(5);
		//disconnect
		agent.finallyAction(true);
		
		assertEquals(2, ret);
	}
	
	@Test(timeout=2000)
	public void testUp01() throws IOException, InterruptedException {
   		File srcFile = makeFile(srcDir, "s_file_1", getTestContent());
   		String cfg = makeConfig(CFG, "", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
   		
   		assertTrue(srcFile.exists());
   		assertTrue(agent.getFtpCacheDir()!=null);
   		assertTrue(agent.getFtpCacheDir().startsWith(agent.getFtpDstDir()));
   		Thread t = new Thread() { public void run(){ agent.run(); } }; 
   		t.start();
   		Thread.sleep(50);
   		agent.setSlowStop(true);
   		agent.setCleanStopFlag();
   		t.join(2000);
   		assertFalse(srcFile.exists());
   		File dstFile = new File(dstDir,srcFile.getName());
   		assertTrue(dstFile.exists());
   		assertTrue(checkFileContent(dstFile));
	}

	@Test(timeout=2000)
	public void testUp02() throws IOException, InterruptedException {
   		File srcFile =  makeFile(srcDir, "s_file_2");
   		String cfg = makeConfig(CFG, "cacheDir = '' substitute = 's/^s_file/new_file/' ", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
   		assertTrue(agent.getCacheDir().length()==0);  
   		Thread t = new Thread() { public void run(){ agent.run(); } }; 
   		t.start();
   		Thread.sleep(50);
   		agent.setSlowStop(true);
   		agent.setCleanStopFlag();
   		t.join(2000);
   		assertFalse(srcFile.exists());
   		File dstFile = new File(dstDir,"new_file_2"); // check new name
   		assertTrue(dstFile.exists());
   		assertTrue(checkFileContent(dstFile));
	}

	@Test(timeout=2000)
	public void testUp03() throws IOException, InterruptedException {
   		File srcFile =  makeFile(srcDir, "s_file_3");
   		String cfg = makeConfig(CFG, "cacheDir = '' saveTime = 'true' gzip='true'", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
   		assertTrue(agent.getFtpCacheDir().startsWith(agent.getFtpDstDir()));
   		Thread t = new Thread() { public void run(){ agent.run(); } }; 
   		t.start();
   		Thread.sleep(50);
   		agent.setSlowStop(true);
   		agent.setCleanStopFlag();
   		t.join(2000);
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
   		final Cfg agent = fc.getAgentByName(AGENT);
   		assertTrue(agent.getFtpCacheDir().startsWith(agent.getFtpDstDir())); // check cacheDir - not empty !
   		Thread t = new Thread() { public void run(){ agent.run(); } }; 
   		long before = System.currentTimeMillis();
   		t.start();
   		Thread.sleep(50);
   		agent.setSlowStop(true);
   		agent.setCleanStopFlag();
   		t.join(2000);
   		assertFalse(srcFile.exists());
   		File dstFile = new File(dstDir, prefix+srcFile.getName()+".zip"); // check prefix and zip
   		assertTrue(dstFile.exists());
   		assertTrue(checkZipFileContent(dstFile));
   		assertTrue(before < dstFile.lastModified()); // check no saveTime
	}

}
