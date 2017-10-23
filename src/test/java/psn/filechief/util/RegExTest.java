package psn.filechief.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class RegExTest {
	public static final String SPLIT_T_1 = "\\{\\s*(.+?)\\s*\\}";
	public static final String SPLIT_T_2 = "\\{\\s*.+?\\s*\\}";
	public static final String TEST_1 = "{11} { 22} , {33 } ; { 44 }";
/*	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
*/
	
	@Test
	public void splitListGrpTest() {
		String[] res = RegEx.splitListGrp(TEST_1, SPLIT_T_1);
		assertTrue(res[0].equals("11"));
		assertTrue(res[1].equals("22"));
		assertTrue(res[2].equals("33"));
		assertTrue(res[3].equals("44"));
		res = RegEx.splitListGrp(TEST_1, SPLIT_T_2);
		assertTrue(res[0].equals("{11}"));
		assertTrue(res[1].equals("{ 22}"));
		assertTrue(res[2].equals("{33 }"));
		assertTrue(res[3].equals("{ 44 }"));
	}

	@Test
	public void getGroupsSTest() {
		String[] res = RegEx.getGroupsS("beforeSome2017___01____31after", "\\D{4}(\\d{4})\\D{3}(\\d{2})\\D{4}(\\d{2})");
		assertTrue(res[0].equals("2017"));
		assertTrue(res[1].equals("01"));
		assertTrue(res[2].equals("31"));
	}
	
	
}
