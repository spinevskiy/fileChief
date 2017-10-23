package psn.filechief.util.bl;

public class UploadBlackList extends StoredBlackList<NotRenamedFile> 
{
	public static final String suffix_BL = ".upBlackList.json";
	public static final String BL_NAME = "uploadBlackList";
	private static String dataDir;

	public UploadBlackList() {
		super(dataDir,BL_NAME, suffix_BL);
	}
	
	/**
	 * Данный конструктор вызывается только один раз из FileChief для присвоения dataDir. 
	 * @param dir
	 */
	public UploadBlackList(String dir) 
	{
		super("--null-",BL_NAME, suffix_BL);
		if(dataDir==null)
			dataDir = dir;
		//else throw new IllegalArgumentException("UploadBlackList - dataDir already inited!. old='"+dataDir+"', new='"+dir+"'");
	}
	
	public Class<? extends IElement> getContainerClass() {
		return NotRenamedFile.class;
	}

}
