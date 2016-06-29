package prob.ghs.exception;

public class MarkProcessedException extends Exception {
	private static final long serialVersionUID = -3628049417336006907L;
	
	public MarkProcessedException(){}
	public MarkProcessedException(String message){ super(message); }
	public MarkProcessedException(Throwable cause){ super(cause); }
	public MarkProcessedException(String message, Throwable cause){ super(message,cause); }
}
