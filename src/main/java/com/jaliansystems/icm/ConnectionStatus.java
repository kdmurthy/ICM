package com.jaliansystems.icm;

import java.util.Date;

public class ConnectionStatus {
	private boolean success;
	private Date start;
	private Date end;

	public ConnectionStatus(boolean success, Date start, Date end) {
		super();
		this.success = success;
		this.start = start;
		this.end = end;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public long getDuration() {
		return (getEnd().getTime() - getStart().getTime()) / 1000;
	}
}
