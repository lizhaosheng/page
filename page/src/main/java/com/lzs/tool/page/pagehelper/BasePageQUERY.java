package com.lzs.tool.page.pagehelper;

import java.io.Serializable;

/**
 * 分页数据
 * 
 * @author lizhaosheng
 *
 */
public class BasePageQUERY implements Serializable{

	/**
	 * 
	 */
	protected static final long serialVersionUID = 1L;
	// 默认不分页
	/**
	 * 分页大小，为0表示不分页且其他分页字段无意义
	 */
	protected long pageSize = 0;
	/**
	 * 页码，从1开始
	 */
	protected long pageIndex;
	/**
	 * 记录总数
	 */
	protected long count;
	/**
	 * 页总数
	 */
	protected long pageCount;
	/**
	 * 从第几条开始查询，默认不用传，从pageIndex和pageSize计算。优先使用分页
	 */
	protected long from;
	/**
	 * 返回多少条数据，默认不用传，等于pageSize。优先使用分页
	 */
	protected long limit;
	
	public long getPageSize() {
		return pageSize;
	}

	public void setPageSize(long pageSize) {
		this.pageSize = pageSize;
	}

	public long getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(long pageIndex) {
		this.pageIndex = pageIndex;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public long getPageCount() {
		if(pageSize <= 0){
			return 0;
		}
		pageCount = count/pageSize + (count%pageSize==0?0:1);
		return pageCount;
	}

	public long getFrom(){
		if(pageSize > 0){
			return (pageIndex-1) * pageSize;
		} else {// 不分页
			return from;
		}
	}

	public long getLimit() {
		if(pageSize > 0){
			return pageSize;
		} else {// 不分页
			return limit;
		}
	}

	public void setLimit(long limit) {
		this.limit = limit;
	}

	public void setFrom(long from) {
		this.from = from;
	}
}