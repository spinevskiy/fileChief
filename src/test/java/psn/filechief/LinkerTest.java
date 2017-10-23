package psn.filechief;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LinkerTest extends FCTest{
	static final String AGENT = "testLink";
	static final String AGENT2 = "testCopy2";
	static final String AGENT3 = "testCopy3";
	static final String AGENT4 = "testCopy4";
	static final String CFG = "<link name = '@name1' srcDir = '@srcDir1' @opt > "
			+ "<copy name='@name2'  dstDir='@dstDir2' cacheDir='' />"
			+ "<copy name='@name3'  dstDir='@dstDir3' cacheDir=''  moveToNext = 'false' />"
			+ "<copy name='@name4' dstDir='@dstDir4' />"
			+ "</link>";

	String[][] subst;
	
	String srcDir1;
	String srcDir2;
	String srcDir3;
	String srcDir4;
	String dstDir2;
	String dstDir3;
	String dstDir4;
	
	Linker link;

	@Before
	public void setUp() throws Exception {
		srcDir1 = makeNewSubDir(SRC); 
		dstDir2 = makeNewSubDir(DST); 
		dstDir3 = makeNewSubDir(DST+"3"); 
		dstDir4 = makeNewSubDir(DST+"4"); 
		subst = new String[][] { 
			{"@name1", AGENT},
			{"@name2", AGENT2},
			{"@name3", AGENT3},
			{"@name4", AGENT4},
			{"@srcDir1", srcDir1},
			{"@dstDir2", dstDir2},
			{"@dstDir3", dstDir3},
			{"@dstDir4", dstDir4}
		};
		link = new Linker();
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
			clearAndRemoveDir(srcDir4);
		if(srcDir3!=null)
			clearAndRemoveDir(srcDir3);
		if(srcDir2!=null)
			clearAndRemoveDir(srcDir2);
		clearAndRemoveDir(srcDir1);
		clearAndRemoveDir(STAT_DATA_DIR);
		clearAndRemoveDir(CONF_DIR);
	}

	@Test
	public final void testInit() throws IOException {
		link.setSrcDir(srcDir1);
		File src = new File(srcDir1);
		clearAndRemoveDir(src);
		assertFalse(src.exists());
		link.init();
		assertTrue(src.exists());
	}

	@Test
	public final void testIsValid() {
		link.setSrcDir(null);
		assertFalse(link.isValid());
		link.setSrcDir("");
		assertFalse(link.isValid());
		link.setSrcDir(srcDir1);
		assertTrue(link.isValid());
	}

	@Test
	public final void testSetDefaultValues() {
		link.setDefaultValues();
		assertFalse(link.getZip());
		assertFalse(link.getGzip());
		assertFalse(link.getUnpack());
		assertEquals(5, link.getDelayBetween().intValue());
		assertFalse(link.getMoveToNext());
	}

	@Test
	public final void testApplyDstFileLimit() throws IOException {
		link.setDstFileLimit(1);
		int limit = link.applyDstFileLimit(5);
		assertEquals(5, limit);
	}

	@Test(timeout=2000)
	public final void testLinker() throws IOException {
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
		assertEquals(0, agent2.getCacheDir().length());
		assertEquals(0, agent3.getCacheDir().length());
		assertNotEquals(0, agent4.getCacheDir().length());
		
		srcDir2 = agent2.getSrcDir();
		srcDir3 = agent3.getSrcDir();
		srcDir4 = agent4.getSrcDir();

		assertTrue(srcDir2.startsWith(agent.getSrcDir()));
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
