package com.lzs.tool.page.pagehelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInterceptor;

/**
 * 封装PageInterceptor，简化操作。
 * 推荐使用另一种方式{@link com.lzs.tool.page.my.MyPageInterceptor}
 */
@Intercepts({@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
	        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})})
public class WrapPageInterceptor extends PageInterceptor {
   
	private static final String SINGLE_PARAM_FLAG = "-1";
	
	private static final String NO_PAGE_FLAG = "-2";
	
	/**
	 * mapper方法id做为key，方法中分页对象参数索引作为value（比如在第一位，则value是0，或者设置了@Param("page"),则value是"page"字符串）
	 */
	private Map<String,String> map = new HashMap<>();
	
	@SuppressWarnings("unchecked")
	@Override
    public Object intercept(Invocation invocation) throws Throwable {
		MappedStatement ms = (MappedStatement) invocation.getArgs()[0];// 当前被拦截mapper方法mybatis的解析结果
		String id = ms.getId();// 方法的id值，其实是类名+方法名（全路径）
		if(!map.containsKey(id)){// 判断是否检查过该方法，若没有则检查该方法是否存在分页参数
			Object params = invocation.getArgs()[1];// 获取方法参数列表
			if(params instanceof BasePageQUERY){// 若只有一个参数，则类型是该参数本身，除非设置了@Param
				map.put(id, SINGLE_PARAM_FLAG);// 只有一个参数,invocation.getArgs()[1]就是该参数
			} else if(params instanceof ParamMap){// 若有多个参数，或者设置了@Param，则params是一个map
				ParamMap<Object> paramMap = (ParamMap<Object>)params;// 这个map的key是参数在方法参数列表中的顺序号，或者设置了@Param中的值
				Set<Entry<String, Object>> entry = paramMap.entrySet();
				Iterator<Entry<String, Object>> it = entry.iterator();
				map.put(id, NO_PAGE_FLAG);// 默认没有分页对象
				while (it.hasNext()){
					Entry<String, Object> e = it.next();
					if(e.getValue() instanceof BasePageQUERY){
						map.put(id, e.getKey());// 存在分页对象，则设置map并跳出循环
						break;
					}
					if(e.getValue() instanceof BasePageQUERY[]){
						// 在多个对象，且BasePageQUERY没有@Param 而其他参数有@Param ，这时mybatis解析的Args会把BasePageQUERY放到一个数组中，
						// 如1=[Lcom.lzs.tool.page.pagehelper.BasePageQUERY，param2=[Lcom.lzs.tool.page.pagehelper.BasePageQUERY
						map.put(id, e.getKey());// 存在分页对象，则设置map并跳出循环
						break;
					}
				}
			} else {
				map.put(id, NO_PAGE_FLAG);// 不存在分页对象，设置NO_PAGE_FLAG标记该mapper方法不分页处理
			}
		}
		
		String index = map.get(id);// 获取分页参数的key
		if(SINGLE_PARAM_FLAG.equals(index)){// 只有一个参数，并且是BasePageQUERY分页参数
			BasePageQUERY page = (BasePageQUERY) invocation.getArgs()[1];
			return getPageList(invocation,page);
		} else if(!NO_PAGE_FLAG.equals(index)){// 存在多个参数，且其中一个是BasePageQUERY分页参数
			ParamMap<?> paramsMap = (ParamMap<?>) invocation.getArgs()[1];
			BasePageQUERY page = null;
			Object pageArg = paramsMap.get(index);
			if(pageArg instanceof BasePageQUERY[]){
				page = ((BasePageQUERY[]) pageArg)[0];
			} else {
				page = (BasePageQUERY) pageArg;
			}
			return getPageList(invocation,page);
		} else {// 没有分页参数
			return super.intercept(invocation);
		}
    }
	private List<?> getPageList(Invocation invocation, BasePageQUERY query) throws Throwable {
		try{
			// 设置分页信息
			Page<Object> page = PageHelper.startPage(new Long(query.getPageIndex()).intValue(), new Long(query.getPageSize()).intValue());
			List<?> list = (List<?>) super.intercept(invocation);// 调用查询方法
	        long total = page.getTotal(); //获取总记录数
	        query.setCount(Long.valueOf(total).intValue());
	        //也可以 List<?> datalist = page.getList();  获取分页后的结果集
	        return list;
		}catch (Exception e){
			// 防止PageHelper 生产了一个分页参数，但是没有被消费，这个参数就会一直保留在这个线程上。当这个线程再次被使用时，就可能导致不该分页的方法去消费这个分页参数，这就产生了莫名其妙的分页。
			PageHelper.clearPage();
			throw e;
		}
        
	}
}