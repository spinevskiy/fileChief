package psn.filechief.util;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import psn.filechief.Copy;

public class RefUtlTest {

	private RefUtl ru, ru2;
	private Copy cfg;
	
	@Before
	public void setUp() throws Exception {
		cfg = new Copy();
		ru = new RefUtl(cfg);
		ru2 = new RefUtl(new Copy(), true);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRefUtlObjectBoolean() throws IllegalArgumentException, ReflectiveOperationException {
		assertTrue(ru2.set("name", "aaa"));
		assertEquals("aaa", ru2.get("name"));
		
		assertFalse(ru2.set("name2", "aaa")); // not set !
		assertNull(ru2.get("name2")); // unknown - return null
	}

	@Test
	public void testRefUtlObject() throws IllegalArgumentException, ReflectiveOperationException {
		ru2.get("name2"); // unknown - exception
	}

	@Test
	public void testRefUtlObject2() throws IllegalArgumentException, ReflectiveOperationException {
		ru2.get("name2"); // unknown - exception
	}
	
	@Test
	public void testSetGet() throws IllegalArgumentException, ReflectiveOperationException {
		ru.set("batchMode", "true");
		assertTrue(cfg.getBatchMode().booleanValue());
		Boolean bm = (Boolean)ru.get("batchMode");
		assertTrue(bm.booleanValue());
		
		ru.set("lagInterval", "10h");
		assertEquals(10*60*60, cfg.getLagIntervalI().intValue());
		String s = (String)ru.get("lagInterval");
		assertEquals(""+10*60*60,s);
		ru.set("lagInterval", s);
		assertEquals(""+10*60*60,s);
		
		assertTrue(ru2.set("name", "aaa"));
		assertEquals("aaa", ru2.get("name"));

	}

	@Test
	public void testHasGetter() {
		assertTrue(ru.hasGetter("name"));
		assertFalse(ru.hasGetter("naMe"));

		assertTrue(ru.hasGetter("saveTime"));
		assertFalse(ru.hasGetter("savetime"));
		
		assertTrue(ru.hasGetter("srcDir"));
		assertFalse(ru.hasGetter("srcDir0"));
		
		assertTrue(ru.hasGetter("keepLastFiles"));
		assertFalse(ru.hasGetter("undef"));
	}

	@Test
	public void testHasSetter() {
		assertTrue(ru.hasSetter("name"));
		assertFalse(ru.hasSetter("naMe"));

		assertTrue(ru.hasSetter("saveTime"));
		assertFalse(ru.hasSetter("savetime"));

		assertTrue(ru.hasSetter("srcDir"));
		assertFalse(ru.hasGetter("srcDir0"));

		assertTrue(ru.hasSetter("keepLastFiles"));
		assertFalse(ru.hasSetter("undef"));
	}

}
