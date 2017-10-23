package psn.filechief;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import org.apache.ftpserver.FtpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import psn.filechief.ftp.IFtpTransport;
import psn.filechief.util.bl.FileDataBlackList;

public class FtpDownloadTest extends FCTest {
	
	public static final String AGENT = "testFtpDown";
	
	static final String CFG = "<ftpDownload name='@name' ftpSrcDir='@ftpSrcDir' dstDir='@dstDir' ftpServerPort='@ftpPort' " +
							"ftpServer='@ftpServer' ftpUser='@ftpUser' ftpPassword='@ftpPassword' @opt /> ";
	
	String ftpSrcDir;
	String srcDir;
	String dstDir;
	String cacheDir;
	
	private FtpServer server;
	
	private String[][] subst;
	
	private FtpDownload down;

	@Before
	public void setUp() throws Exception {
		dstDir = makeNewSubDir(DST); 
		cacheDir = makeNewSubDir(CACHE);
		ftpSrcDir = "/"+REMOTE_DIR;
		srcDir = makeNewSubDir(FTPD_USER_PATH+ftpSrcDir);
		
		subst = new String[][] { 
			{"@name", AGENT},
			{"@ftpSrcDir", ftpSrcDir},
			{"@dstDir", dstDir},
			{"@cacheDir", cacheDir},
			{"@ftpPort", ""+FTP_PORT},
			{"@ftpServer", FTP_ADDR},
			{"@ftpUser", FTP_USER},
			{"@ftpPassword", FTP_PASSWORD}
		};
		down = new FtpDownload();
		server = getFtpServer();
		server.start();		
	}

	@After
	public void tearDown() throws Exception {
		clearAndRemoveDir(dstDir);
		clearAndRemoveDir(srcDir);
		clearAndRemoveDir(cacheDir);
		//clearAndRemoveDir(new File(USER_PATH));
		server.stop();		
		clearAndRemoveDir(FTPD_PATH);
		clearAndRemoveDir(STAT_DATA_DIR);
		clearAndRemoveDir(CONF_DIR);
	}

	@Test
	public void testIsValid() {
		down.setDstDir(dstDir);
		down.setFtpSrcDir(ftpSrcDir);
		down.setFtpServer(FTP_ADDR);
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
		assertFalse(down.getFtpRemoteVerification());
		assertEquals(down.getDelayBetween().intValue(), 30);
		assertEquals(down.getFtpServerPort().intValue(), 21);
		assertEquals(down.getFtpTimeout().intValue(), FtpUpload.DEFAULT_TIMEOUT);
		assertEquals(down.getFtpFileType(), IFtpTransport.FTP_BINARY);
		assertTrue(down.getSaveTime());
		assertTrue(down.getFtpActiveMode());
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
	public void testDown01() throws Exception {
   		File srcFile = makeFile(srcDir, "s_file_1", getTestContent());
   		String cfg = makeConfig(CFG, "", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
   		assertTrue(srcFile.exists());
   		assertTrue(agent.getCacheDir()!=null);
   		assertTrue(agent.getCacheDir().startsWith(agent.getDstDir()));
   		Thread t = new Thread() { public void run(){ agent.run(); } }; 
   		t.start();
   		t.interrupt();
   		try { t.join(2000); } catch (InterruptedException e1) {}
   		assertFalse("srcFile:"+srcFile.getAbsolutePath(), srcFile.exists());
   		File dstFile = new File(dstDir,srcFile.getName());
   		assertTrue(dstFile.exists());
   		assertTrue(checkFileContent(dstFile));
	}

	@Test(timeout=2000)
	public void testDown02() throws IOException {
   		File srcFile =  makeFile(srcDir, "s_file_2");
   		String cfg = makeConfig(CFG, "substitute = 's/^s_file/new_file/' ", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
   		Thread t = new Thread() { public void run(){ agent.run(); } }; 
   		t.start();
   		t.interrupt();
   		try { t.join(2000); } catch (InterruptedException e1) {}
   		assertFalse(srcFile.exists());
   		File dstFile = new File(dstDir,"new_file_2"); // check new name
   		assertTrue(dstFile.exists());
   		assertTrue(checkFileContent(dstFile));
	}

	@Test(timeout=2000)
	public void testDown03() throws IOException {
   		File srcFile =  makeFile(srcDir, "s_file_3");
   		long ft = srcFile.lastModified() - 120_000;
   		srcFile.setLastModified(ft);
   		String cfg = makeConfig(CFG, "cacheDir = '' gzip='true'", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
   		assertTrue(agent.getCacheDir().startsWith(agent.getDstDir()));
   		Thread t = new Thread() { public void run(){ agent.run(); } }; 
   		t.start();
   		t.interrupt();
   		try { t.join(2000); } catch (InterruptedException e1) {}
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
   		File dstFile = new File(dstDir, prefix+srcFile.getName()+".zip"); // check prefix and zip
   		assertFalse(dstFile.exists());
   		t.start();
   		t.interrupt();
   		t.join(2000);
   		assertFalse(srcFile.exists());
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
   		t.interrupt();
   		try { t.join(2000); } catch (InterruptedException e1) {}
   		assertTrue(srcFile.exists());
   		File dstFile = new File(dstDir, srcFile.getName()); // check prefix and zip
   		assertTrue(dstFile.exists());
   		assertTrue(checkFileContent(dstFile));
	}

	
	/**
	 * @throws Exception
	 */
	@Test(timeout=2000)
	public void testBlackList() throws Exception {
		server.stop();
		server = getFtpServer(false); // write (and delete) disabled 
		server.start();
   		File srcFile = makeFile(srcDir, "s_file_1", getTestContent());
   		String cfg = makeConfig(CFG, "", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName(AGENT);
   		
   		assertTrue(srcFile.exists());
   		assertTrue(agent.getCacheDir()!=null);
   		assertTrue(agent.getCacheDir().startsWith(agent.getDstDir()));

   		File blackList = new File(STAT_DATA_DIR, AGENT + FileDataBlackList.SUFFIX_BL);  
   		assertFalse(blackList.exists()); // 
   		
   		Thread t = new Thread() { public void run(){ agent.run(); } }; 
   		t.start();
   		t.interrupt();
   		try { t.join(2000); } catch (InterruptedException e1) {}
   		
   		assertTrue(srcFile.exists()); // remote file not deleted !
   		
   		File dstFile = new File(dstDir,srcFile.getName()); 
   		assertTrue(dstFile.exists());    // and local copy created
   		assertTrue("blackList:"+blackList.getAbsolutePath(),blackList.exists());  // and blackList created   
   		
   		assertEquals(2, new File(dstDir).list().length); // cacheDir + received file
   		assertTrue(checkFileContent(dstFile));
   		Files.delete(Paths.get(dstFile.getAbsolutePath()));  // delete received file

		server.stop();
		server = getFtpServer(true); // write enabled
		server.start();
		
   		File srcFile2 = makeFile(srcDir, "s_file_2", getTestContent());
   		t = new Thread() { public void run(){ agent.run(); } }; 
   		t.start();
   		t.interrupt();
   		try { t.join(2000); } catch (InterruptedException e1) {}
   		assertFalse(srcFile.exists()); //  remote file 's_file_1' not exists ...
   		assertFalse(dstFile.exists()); //  and not loaded => deleted
   		assertFalse(blackList.exists());  // and blackList cleared 
   		
   		assertFalse(srcFile2.exists());  //  remote file 's_file_2' not exists ...
   		File dstFile2 = new File(dstDir,srcFile2.getName());
   		assertTrue(dstFile2.exists());  //  loaded
   		assertEquals(2, new File(dstDir).list().length);
   		assertTrue(checkFileContent(dstFile2));
	}


}
