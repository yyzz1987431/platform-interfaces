package com.aic.paas.task.peer.dep.bean;

public class LogReq extends GeneralReq {
	private Long lastFetchTime;
	private Integer reqId;

	public Integer getReqId() {
		return reqId;
	}

	public Long getLastFetchTime() {
		return lastFetchTime;
	}

	public void setLastFetchTime(Long lastFetchTime) {
		this.lastFetchTime = lastFetchTime;
	}

	public void setReqId(Integer reqId) {
		this.reqId = reqId;
	}

}
