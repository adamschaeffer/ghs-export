package prob.ghs.exception;

public class AcknowledgmentException extends Exception {
	private static final long serialVersionUID = 7226587736809770128L;

	public AcknowledgmentException(){}
	public AcknowledgmentException(String message){ super(message); }
	public AcknowledgmentException(Throwable cause){ super(cause); }
	public AcknowledgmentException(String message, Throwable cause){ super(message,cause); }
}
