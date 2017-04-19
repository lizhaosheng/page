package com.lzs.tool.page.my;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分页标记注解，设置在mapper方法上表明当前方法需要进行分页统计，并且分页统计方法value，如:<br>
 * @Page("count")<br>
	List<DataDO> queryPage(BaseInfoQUERY<DataDO> query);<br>
	表示调用queryPage时需要分页，并且分页方法是"count"。<br>
	注意query对象必须实现{@link PageInfo}接口，并且 queryPage对应的sql语句需要自己写limit子句。<br>
	也就是说{@link Page}注解实际上只是自动执行指定统计方法，并将结果通过PageInfo接口设置。<br>
	另外，注意统计方法count参数列表必须和queryPage一致！并且返回值必须是int或者long
	
 * @author lzs
 *
 */
@Target(ElementType.METHOD) //作用于方法
@Retention(RetentionPolicy.RUNTIME) // 在运行时有效（即运行时保留）
public @interface Page {
	String value();
}
