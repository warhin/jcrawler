package jcrawler;

public class JCrawlerException extends RuntimeException {

	private static final long serialVersionUID = 6836276507210115361L;

	public JCrawlerException(String message, Throwable cause) {
		super(message, cause);
	}

	public JCrawlerException(String message) {
		super(message);
	}

	public JCrawlerException(Throwable cause) {
		super(cause);
	}

}
