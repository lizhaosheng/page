package com.lzs.tool.page.partition;

/**
 * 所有分页对象必须实现该接口，设置{@link Page}注解的方法中必须有一个参数是{@link PageInfo}的实现类。<br>
 * 拦截器会拦截所有{@link Page}注解的方法，并调用注解指定的统计总数方法，然后将统计总数结果通过PageInfo.setCount设置进去
 * @author lzs
 *
 */
public interface PageInfo {

	/**
	 * 设置记录总数
	 */
	void setCount(long count);
	
	/**
	 * 跳过统计总数，即不执行{@link Page}注解的方法
	 * @return - true跳过，false执行统计
	 */
	boolean skipCount();
}
