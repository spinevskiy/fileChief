package psn.filechief;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.ftpserver.FtpServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RemoteSourceTest extends FCTest {
	static final String AGENT = "testDown";
	static final String AGENT2 = "testCopy2";
	static final String AGENT3 = "testCopy3";
	static final String AGENT4 = "testCopy4";
	static final String CFG = "<ftpDownload name = '@name1' ftpSrcDir = '@ftpSrcDir' cacheDir='@cacheDir'"
			+ " dstDir='@dstDir1' ftpServer='@ftpServer' ftpUser='@ftpUser' ftpPassword='@ftpPassword' "
			+ " ftpServerPort='@ftpPort' createLinks = 'true' > "
			+ "<copy name='@name2'  dstDir='@dstDir2' cacheDir='' saveTime = 'false' />"
			+ "<copy name='@name3'  dstDir='@dstDir3' cacheDir='' saveTime = 'false' moveToNext = 'false' />"
			+ "<copy name='@name4' dstDir='@dstDir4' />"
			+ "</ftpDownload>";

	String[][] subst;
	
	String srcDir1;
	String srcDir2;
	String srcDir3;
	String srcDir4;
	String dstDir1;
	String dstDir2;
	String dstDir3;
	String dstDir4;
	String cacheDir;
	String ftpSrcDir;
	
	RemoteSource down;
	RemoteSource sdown;
	
	static FtpServer server;

	public void prepare(RemoteSource rs) {
		rs.setFtpServer(FTP_ADDR);
		rs.setFtpSrcDir("path");
		rs.setDstDir("path");
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		server = getFtpServer();
		server.start();		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		server.stop();		
		clearAndRemoveDir(new File(FTPD_PATH));
		clearAndRemoveDir(STAT_DATA_DIR);
	}
	
	@Before
	public void setUp() throws Exception {
		down = new FtpDownload();
		sdown = new SFtpDownload();
		dstDir1 = makeNewSubDir(DST); 
		dstDir2 = makeNewSubDir(DST+"2"); 
		dstDir3 = makeNewSubDir(DST+"3"); 
		dstDir4 = makeNewSubDir(DST+"4");
		
		cacheDir = makeNewSubDir(CACHE);
		ftpSrcDir = "/"+REMOTE_DIR;
		srcDir1 = makeNewSubDir(FTPD_USER_PATH+ftpSrcDir);
		
		subst = new String[][] { 
			{"@name1", AGENT},
			{"@name2", AGENT2},
			{"@name3", AGENT3},
			{"@name4", AGENT4},
			{"@srcDir1", srcDir1},
			{"@dstDir2", dstDir2},
			{"@dstDir3", dstDir3},
			{"@dstDir4", dstDir4},
			{"@ftpSrcDir", ftpSrcDir},
			{"@dstDir", dstDir1},
			{"@cacheDir", cacheDir},
			{"@ftpPort", ""+FTP_PORT},
			{"@ftpServer", FTP_ADDR},
			{"@ftpUser", FTP_USER},
			{"@ftpPassword", FTP_PASSWORD}
		};
		srcDir2 = null;
		srcDir3 = null;
		srcDir4 = null;
	}

	@After
	public void tearDown() throws Exception {
		clearAndRemoveDir(dstDir2);
		clearAndRemoveDir(dstDir3);
		clearAndRemoveDir(dstDir4);
		if(srcDir4!=null)
			clearAndRemoveDir(new File(srcDir4));
		if(srcDir3!=null)
			clearAndRemoveDir(new File(srcDir3));
		if(srcDir2!=null)
			clearAndRemoveDir(new File(srcDir2));
		clearAndRemoveDir(new File(srcDir1));
		clearAndRemoveDir(new File(cacheDir));
		clearAndRemoveDir(new File(dstDir1));
		clearAndRemoveDir(CONF_DIR);
	}

	@Test
	public final void testIsValid() {
		prepare(down);
		prepare(sdown);
		assertTrue(down.isValid());
		assertTrue(sdown.isValid());

		down.setFtpServer("");
		sdown.setFtpServer("");
		assertFalse(down.isValid());
		assertFalse(sdown.isValid());

		prepare(down);
		prepare(sdown);
		down.setFtpSrcDir("");
		sdown.setFtpSrcDir("");
		assertFalse(down.isValid());
		assertFalse(sdown.isValid());

		prepare(down);
		prepare(sdown);
		down.setDstDir("");
		sdown.setDstDir("");
		assertFalse(down.isValid());
		assertFalse(sdown.isValid());
	}

//	@Test
//	public final void testWorkWithFile() {
//		fail("Not yet implemented");
//	}

//	@Test
//	public final void testAfterAction() {
//		fail("Not yet implemented");
//	}

	@Test
	public final void testSetSrcDir() {
		assertNull(down.getSrcDir());
		assertNull(sdown.getSrcDir());
		down.setSrcDir("path");
		sdown.setSrcDir("path");
		assertNull(down.getSrcDir());
		assertNull(sdown.getSrcDir());
	}


	@Test(timeout=2000)
	public final void testHardLink() throws IOException {
		File srcFile = makeFile(srcDir1, "s_file_1", getTestContent());
		String cfg = makeConfig(CFG, "", subst);
		File dst2 = new File(dstDir2);
		File dst3 = new File(dstDir3);
		clearAndRemoveDir(dst2);
		clearAndRemoveDir(dst3);

		assertFalse(dst2.exists());
		assertFalse(dst3.exists());
		
		final FileChief fc = FileChief.newInstance(cfg, false, false);

		assertTrue(dst2.exists());
		assertTrue(dst3.exists());
		
		final Cfg agent = fc.getAgentByName(AGENT);
		final Cfg agent2 = fc.getAgentByName(AGENT2);
		final Cfg agent3 = fc.getAgentByName(AGENT3);
		final Cfg agent4 = fc.getAgentByName(AGENT4);
		
		assertTrue(srcFile.exists());
		assertEquals("agent2.getCacheDir():"+agent2.getCacheDir(), 0, agent2.getCacheDir().length());
		assertEquals(0, agent3.getCacheDir().length());
		assertNotEquals(0, agent4.getCacheDir().length());
		
		srcDir2 = agent2.getSrcDir();
		srcDir3 = agent3.getSrcDir();
		srcDir4 = agent4.getSrcDir();

		assertTrue(srcDir2.startsWith(agent.getDstDir()));
		assertTrue(srcDir3.startsWith(agent2.getSrcDir()));
		assertTrue(srcDir4.equals(agent3.getDstDir()));
		
		assertTrue(new File(srcDir2).exists());
		assertTrue(new File(srcDir3).exists());
		assertTrue(new File(srcDir4).exists());
		
		Thread t = new Thread() { public void run(){ agent.run(); } }; 
		t.start();
		t.interrupt();
		try { t.join(2000); } catch (InterruptedException e1) {}
		assertFalse(srcFile.exists());
		File dstFile2 = new File(srcDir2,srcFile.getName());
		File dstFile3 = new File(srcDir3,srcFile.getName());
		File dstFile4 = new File(srcDir4,srcFile.getName());
		assertTrue(dstFile2.exists());
		assertTrue(dstFile3.exists());
		assertFalse(dstFile4.exists());
		assertTrue(checkFileContent(dstFile2));
		assertTrue(checkFileContent(dstFile3));
	}


}
