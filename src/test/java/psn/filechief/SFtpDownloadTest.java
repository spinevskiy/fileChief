package psn.filechief;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import org.apache.sshd.server.SshServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SFtpDownloadTest extends FCTest {

	public static String AGENT = "testSftpDown";
	
	static final String CFG = "<sftpDownload name='@name' ftpSrcDir='@ftpSrcDir' dstDir='@dstDir' ftpServerPort='@ftpPort' " +
							"ftpServer='@ftpServer' ftpUser='@ftpUser' ftpPassword='@ftpPassword' delayBetween='1' @opt /> ";
		
	private SshServer server;
	
	String ftpSrcDir;
	String srcDir;
	String dstDir;
	String cacheDir;
	
	String[][] subst;
	
	SFtpDownload down;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		clearAndRemoveDir(new File(FtpDownloadTest.FTPD_PATH));
	}

	@Before
	public void setUp() throws Exception {
		dstDir = makeNewSubDir(DST); 
		cacheDir = makeNewSubDir(CACHE);
		ftpSrcDir = "/"+FtpDownloadTest.REMOTE_DIR;
		srcDir = makeNewSubDir(FtpDownloadTest.FTPD_USER_PATH+ftpSrcDir);
		
		subst = new String[][] { 
			{"@name", AGENT},
			{"@ftpSrcDir", ftpSrcDir},
			{"@dstDir", dstDir},
			{"@cacheDir", cacheDir},
			{"@ftpPort", ""+FtpDownloadTest.FTP_PORT},
			{"@ftpServer", FtpDownloadTest.FTP_ADDR},
			{"@ftpUser", FtpDownloadTest.FTP_USER},
			{"@ftpPassword", FtpDownloadTest.FTP_PASSWORD}
		};
		down = new SFtpDownload();
		server = getSftpServer();
		server.start();
	}

	@After
	public void tearDown() throws Exception {
		clearAndRemoveDir(dstDir);
		clearAndRemoveDir(srcDir);
		server.stop();
		//clearAndRemoveDir(new File(USER_PATH));
		clearAndRemoveDir(STAT_DATA_DIR);
		clearAndRemoveDir(new File(FtpDownloadTest.FTPD_PATH));
		clearAndRemoveDir(CONF_DIR);
	}

	@Test
	public void testIsValid() {
		down.setDstDir(dstDir);
		down.setFtpSrcDir(ftpSrcDir);
		down.setFtpServer(FtpDownloadTest.FTP_ADDR);
		assertTrue(down.isValid());
		
		down.setFtpSrcDir(null);
		assertFalse(down.isValid());
		
		down.setDstDir(null);
		down.setFtpSrcDir(ftpSrcDir);
		assertFalse(down.isValid());
	}

	@Test
	public void testSetDefaultValues() {
		down.setDefaultValues();
		assertFalse(down.getZip());
		assertFalse(down.getGzip());
		assertFalse(down.getUnpack());
		assertEquals(RemoteSource.DEFAULT_DELAY, down.getDelayBetween().intValue());
		assertEquals(22, down.getFtpServerPort().intValue());
		assertEquals(FtpUpload.DEFAULT_TIMEOUT, down.getFtpTimeout().intValue());
		assertTrue(down.getSaveTime());
		assertTrue(down.getReconnect());
	}

	@Test
	public void testAutoMakeDirs() throws IOException {
		down.setDstDir(dstDir);
		down.setCacheDir(cacheDir);
		File dst = new File(dstDir);
		File cache = new File(cacheDir);
		clearAndRemoveDir(dst);
		clearAndRemoveDir(cache);
		
		assertFalse(dst.exists());
		assertFalse(cache.exists());
		down.init();
		assertTrue(dst.exists());
		assertTrue(cache.exists());
	}

	@Test
	public void testApplyDstFileLimit() throws IOException {
		down.setDstDir(dstDir);
		
		makeFile(dstDir,"file1"); 
		makeFile(dstDir,"file2"); 
		
		down.setDstFileLimit(Integer.valueOf(1));
		int ret = down.applyDstFileLimit(5);
		assertEquals(0, ret);
		
		down.setDstFileLimit(Integer.valueOf(4));
		ret = down.applyDstFileLimit(5);
		assertEquals(2, ret);
	}
	
	@Test(timeout=2000)
	public void testDown01() throws IOException, InterruptedException {
   		File srcFile = makeFile(srcDir, "s_file_1", getTestContent());
   		String cfg = makeConfig(CFG, "", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
   		assertTrue(srcFile.exists());
   		assertTrue(agent.getCacheDir()!=null);
   		assertTrue(agent.getCacheDir().startsWith(agent.getDstDir()));
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
	public void testDown02() throws IOException, InterruptedException {
   		File srcFile =  makeFile(srcDir, "s_file_2");
   		String cfg = makeConfig(CFG, "substitute = 's/^s_file/new_file/' ", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
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
	public void testDown03() throws IOException, InterruptedException {
   		File srcFile =  makeFile(srcDir, "s_file_3");
   		long ft = srcFile.lastModified() - 120_000;
   		srcFile.setLastModified(ft);
   		String cfg = makeConfig(CFG, "cacheDir = '' gzip='true'", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
   		assertTrue(agent.getCacheDir().startsWith(agent.getDstDir()));
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
	public void testDown04() throws IOException, Exception {
   		File srcFile =  makeFile(srcDir, "s_file_4");
   		long ft = srcFile.lastModified() - 120_000;
   		srcFile.setLastModified(ft);
   		String prefix = "pref_";
   		String cfg = makeConfig(CFG, "cacheDir = '' saveTime = 'false' zip='true' prefix = \"'"+prefix+"'\" ", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
   		assertTrue(agent.getCacheDir().startsWith(agent.getDstDir())); // check cacheDir - not empty !
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

	@Test(timeout=2000)
	public void testStartFileDate() throws IOException, Exception {
   		File srcFile =  makeFile(srcDir, "s_file_5");
   		String cfg = makeConfig(CFG, " startFileDate='2014-01-01 00:00:00' ", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
   		assertTrue(agent.getCacheDir().startsWith(agent.getDstDir())); 

   		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		assertEquals(sdf.parse("2014-01-01 00:00:00").getTime(), agent.getStartFileDateLong().longValue());

   		Thread t = new Thread() { public void run(){ agent.run(); } }; 
   		t.start();
   		Thread.sleep(50);
   		agent.setSlowStop(true);
   		agent.setCleanStopFlag();
   		t.join(2000);
   		assertTrue(srcFile.exists());
   		File dstFile = new File(dstDir, srcFile.getName()); // check prefix and zip
   		assertTrue(dstFile.exists());
   		assertTrue(checkFileContent(dstFile));
	}

}
