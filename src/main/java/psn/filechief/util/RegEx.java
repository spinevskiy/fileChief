package psn.filechief.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegEx 
{
	public static final String attrListPattern = "\".*?\"|\\S+"; 
	public static final Pattern attrList = Pattern.compile(attrListPattern);
	
	protected Pattern pat;
	
	public RegEx() {
		pat = attrList;
	}
	
	public RegEx(String pattern) {
		pat = Pattern.compile(pattern);
	}

	public RegEx(Pattern pattern) {
		pat = pattern;
	}
	
	/**
	 * Ищет во входной строке один фрагмент, отвечающий шаблону, и извлекает из него все группы.
	 * @param in входная строка
	 * @return
	 */
	public String[] getGroups(String in)
	{
		ArrayList<String> res = new ArrayList<>(); 
		Matcher m = pat.matcher(in);
		if( m.find()) 
			for(int i=1; i<=m.groupCount(); i++) 
				res.add(m.group(i));
		return res.toArray(new String[0]);
	}

	/**
	 * Ищет во входной строке один фрагмент, отвечающий шаблону, и извлекает из него все группы.
	 * @param in
	 * @return
	 */
	public static String[] getGroupsS(String in,String pattern) {
		return new RegEx(pattern).getGroups(in);
	}

/*	
	public String[] split(String in)
	{
		ArrayList<String> res = new ArrayList<>(); 
		Matcher m = pat.matcher(in);
		while( m.find() ) 
			res.add(m.group());
		return res.toArray(new String[0]);
	}
*/
	/**
	 * Ищет во входной строке все последовательности, отвечающие шаблону.
	 * В выходной массив попадает сама последовательность, либо первая группа из неё (если есть).
	 * Например для входной строки <b>"aaa 555bbb 787ccc999"</b> и шаблона <b>"\s*(\d+)\s*"</b>
	 * вернёт значения <b>{"555","787","999"}</b>.
	 * @param in входная строка
	 * @return
	 */
	public String[] splitGrp(String in)
	{
		ArrayList<String> res = new ArrayList<>(); 
		Matcher m = pat.matcher(in);
		while( m.find() ) {
			int gc = ( m.groupCount()==0 ) ? 0 : 1; 
			res.add(m.group(gc));
		}
		return res.toArray(new String[0]);
	}

	/**
	 * Ищет во входной строке все последовательности, отвечающие шаблону.
	 * В выходной массив попадает сама последовательность, либо первая группа из неё (если есть).
	 * Например для входной строки <b>"aaa 555bbb 787ccc999"</b> и шаблона <b>"\s*(\d+)\s*"</b>
	 * вернёт значения <b>{"555","787","999"}</b>.
	 * @param in входная строка
	 * @return
	 */
	public static String[] splitListGrp(String in,String pattern) {
		return new RegEx(pattern).splitGrp(in);
	}

//	public static String[] splitList(String in) {
//	return new RegEx().split(in);
//}

//public static String[] splitList(String in,String pattern) {
//	return new RegEx(pattern).split(in);
//}
	
//	public static String[] getAllGrpoups(String in,String pattern) {
//		return new RegEx(pattern).splitGrp(in);
//	}
	
	//unpaired double quotes
/*	
	public static final String DBL_QUOTE = "\"";  
	public static String unQuoteX(String in) throws IllegalArgumentException 
	{
		
		boolean beg = in.startsWith(DBL_QUOTE);
		boolean end = in.endsWith(DBL_QUOTE);
		if(beg!=end)
			throw new IllegalArgumentException("unpaired double quotes "+in);
		if(beg && end)
			return in.substring(1, in.length()-2);
		return in;
	}
*/	
//	public static boolean find(String val, String pattern)
//	{
//		 return Pattern.compile(pattern).matcher(val).find();
//	}
	
	public static void main(String args[]) 
	{
		//String re = "^(?i)\\s*(table\\(.+?\\))\\s+values\\((.+?)\\)\\s*$";
		//String re = "(\\d{2}_\\d{2}_\\d{4})";
		String re = "\\s*,\\s*";
		//String tst = "table(po1,po2,po3) Values( #aaa#, #bbb# ) ";
		//String tst = "file_01_04_2112.ttt";
		String tst = "(\\d{8}_\\d{6}) , ddMMyyyy_HHmmss";
		//String tt =  "#par#, #par2#, #par3# ";
		//String[] rr = getGroupsS(tst, re);
		String[] rr = tst.split(re);
		for( String r: rr)
			System.out.println(r);
		//String[] xx = splitListGrp(rr[1], "#(\\w+)#" );
		//for( String x: xx)
		//	System.out.println(x);
		
	}


}
