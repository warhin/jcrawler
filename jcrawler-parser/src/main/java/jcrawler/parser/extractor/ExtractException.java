package jcrawler.parser.extractor;

public class ExtractException extends IllegalStateException {

	private static final long serialVersionUID = -7854773353113422671L;

	public ExtractException() {
		super();
	}

	public ExtractException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExtractException(String s) {
		super(s);
	}

	public ExtractException(Throwable cause) {
		super(cause);
	}
	
	public ExtractException(String msg, Object... params) {
        super(String.format(msg, params));
    }

}
