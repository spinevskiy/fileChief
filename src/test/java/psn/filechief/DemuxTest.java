package psn.filechief;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DemuxTest extends FCTest {
	static final String AGENT = "testDemux";
	static final String CFG = "<demux name = '@name1' srcDir = '@srcDir' dstDirList='@dstDir1 ; @dstDir2 ; @dstDir3' />";

	String[][] subst;

	String srcDir;
	String dstDir1;
	String dstDir2;
	String dstDir3;
	Demux demux;
	
	@Before
	public void setUp() throws Exception {
		demux = new Demux();
		srcDir = makeNewSubDir(SRC); 
		dstDir1 = makeNewSubDir(DST+File.separator+"sub1"); 
		dstDir2 = makeNewSubDir(DST+File.separator+"sub2"); 
		dstDir3 = makeNewSubDir(DST+File.separator+"sub3"); 
		subst = new String[][] { 
			{"@name1", AGENT},
			{"@srcDir", srcDir},
			{"@dstDir1", dstDir1},
			{"@dstDir2", dstDir2},
			{"@dstDir3", dstDir3}
		};
	}

	@After
	public void tearDown() throws Exception {
		clearAndRemoveDir(dstDir1);
		clearAndRemoveDir(dstDir2);
		clearAndRemoveDir(dstDir3);
		clearAndRemoveDir(srcDir);
		clearAndRemoveDir(STAT_DATA_DIR);
	}

	@Test
	public void testIsValid() {
		assertFalse(demux.isValid());
		demux.setSrcDir(SRC);
		assertFalse(demux.isValid());
		demux.setDstDirList(dstDir1);
		assertTrue(demux.isValid());
	}

	@Test
	public void testSetDefaultValues() {
		demux.setDefaultValues();
		assertFalse(demux.getZip());
		assertFalse(demux.getGzip());
		assertFalse(demux.getUnpack());
		assertEquals(5, demux.getDelayBetween().intValue());
		assertFalse(demux.getMoveToNext());
	}

	@Test
	public void testApplyDstFileLimit() throws IOException {
		assertEquals(10, demux.applyDstFileLimit(10));
		demux.setDstFileLimit(1);
		assertEquals(10, demux.applyDstFileLimit(10));
	}

	@Test(timeout=2000)
	public void testDemux() throws IOException {
		byte[] content1 = getTestContent();
		byte[] content2 = getTestContent();
		byte[] content3 = getTestContent();
		byte[] content4 = getTestContent();
		File srcFile1 = makeFile(srcDir, "s_file_1", content1);
		File srcFile2 = makeFile(srcDir, "s_file_2", content2);
		File srcFile3 = makeFile(srcDir, "s_file_3", content3);
		File srcFile4 = makeFile(srcDir, "s_file_4", content4);
		changeTime(srcFile1, - 10_000); // - 10 sec
		changeTime(srcFile2, - 9_000);
		changeTime(srcFile3, - 8_000);
		String cfg = makeConfig(CFG, "", subst);
		
		final FileChief fc = FileChief.newInstance(cfg, false, false);
		
		final Cfg agent = fc.getAgentByName(AGENT);
		assertNull(agent.getCacheDir());
		
		Thread t = new Thread() { public void run(){ agent.run(); } }; 
		t.start();
		t.interrupt();
		try { t.join(2000); } catch (InterruptedException e1) {}

		assertFalse(srcFile1.exists());
		assertFalse(srcFile2.exists());
		assertFalse(srcFile3.exists());
		assertFalse(srcFile4.exists());

		File dstFile1 = new File(dstDir1,srcFile1.getName()); // file1 -> dstDir1
		File dstFile2 = new File(dstDir2,srcFile2.getName()); // file2 -> dstDir2
		File dstFile3 = new File(dstDir3,srcFile3.getName()); // file3 -> dstDir3
		File dstFile4 = new File(dstDir1,srcFile4.getName()); // file4 -> dstDir1
		
		assertTrue(dstFile1.exists());
		assertTrue(dstFile2.exists());
		assertTrue(dstFile3.exists());
		assertTrue(dstFile4.exists());
		
		assertTrue(checkFileContent(dstFile1, content1));
		assertTrue(checkFileContent(dstFile2, content2));
		assertTrue(checkFileContent(dstFile3, content3));
		assertTrue(checkFileContent(dstFile4, content4));
	}

}
