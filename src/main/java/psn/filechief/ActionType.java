package psn.filechief;

public enum ActionType {
	LOCAL2LOCAL, LOCAL2REMOTE, REMOTE2LOCAL ;
	
	public boolean isRemote()
	{
		return equals(LOCAL2REMOTE) || equals(REMOTE2LOCAL); 
	}
}
