package psn.filechief.util.bl;

public class FileDataBlackList extends StoredBlackList<FileData> 
{
	public static final String SUFFIX_BL = ".blackList.json";
	public static final String BL_NAME = "blackList";
	private static String dataDir;

	public FileDataBlackList() {
		super(dataDir,BL_NAME, SUFFIX_BL);
	}
	
	/**
	 * Данный конструктор вызывается только один раз из FileChief для присвоения dataDir. 
	 * @param dir
	 */
	public FileDataBlackList(String dir) 
	{
		super("--null--",BL_NAME, SUFFIX_BL);
		if(dataDir==null) {
			dataDir = dir;
		}
		//else throw new IllegalArgumentException("FileDataBlackList2 - dataDir already inited!. old='"+dataDir+"', new='"+dir+"'");
	}

	public Class<? extends IElement> getContainerClass() {
		return FileData.class;
	}
	
}
