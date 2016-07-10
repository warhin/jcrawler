package jcrawler.exporter;

public class ExportException extends RuntimeException {

	private static final long serialVersionUID = -4770461479921498322L;

	public ExportException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public ExportException(String arg0) {
		super(arg0);
	}

	public ExportException(Throwable arg0) {
		super(arg0);
	}

}
