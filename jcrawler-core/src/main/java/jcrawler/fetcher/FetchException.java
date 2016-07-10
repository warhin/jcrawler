package jcrawler.fetcher;

public class FetchException extends RuntimeException {

	private static final long serialVersionUID = 1014016287703339589L;
	
	private int statusCode = -1;
	
	private String statusMessage;

	public FetchException(String message, Throwable cause) {
		super(message, cause);
	}

	public FetchException(String message) {
		super(message);
	}

	public FetchException(Throwable cause) {
		super(cause);
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append(",statusCode is ")
				.append(statusCode).append(",statusMessage is ")
				.append(statusMessage);
		return sb.toString();
	}

}
