# page
两种分页方式

第一种：
在包路径com.lzs.tool.page.pagehelper 下
主要是通过封装com.github.pagehelper.PageInterceptor 简化操作
<dependency>
			<groupId>com.github.pagehelper</groupId>
			<artifactId>pagehelper</artifactId>
			<version>5.0.0</version>
</dependency>

使用：
1、在mybatis.xml中添加插件
  <plugins>
	    <plugin interceptor="com.lzs.tool.page.pagehelper.WrapPageInterceptor" ></plugin>
	</plugins>
  
2、查询时，传入参数BasePageQUERY 即可：
class Param extends BasePageQUERY{
...
}
@Resource
public interface InfoMapper {
  List<InfoDO> queryInfo(Param query);
}
或
class Param {
...
}
@Resource
public interface InfoMapper {
  List<InfoDO> queryInfo(@Param("query")Param query, @Param("page")BasePageQUERY page);
}

插件WrapPageInterceptor会自动将统计结果设置到分页对象page中（page.setCount(total)），通过page.getCount()可以获取到记录总数，通过page.getPageCount()可以获取总页数
public  Result queryInfo(Param query,BasePageQUERY page){
  List<InfoDO> list = infoMapper.queryInfo(query,page);
  int cout = page.getCount();
  int pageCount = page.getPageCount();
  ...
}


第二种：
在包路径com.lzs.tool.page.partition 下
通过切面拦截带特定注解的mapper方法，并通过解析注解自动调用注解指定的统计总数方法

使用
1、引入配置
	<!-- 分页处理 -->
	<bean id="pageInterceptor" class="com.lzs.tool.page.partition.PageInterceptor" />
    <aop:config>
        <aop:aspect id="pageAspect" ref="pageInterceptor">
            <aop:around  method="around" pointcut="execution(* com.xxx.mapper..*.*(..))" />
        </aop:aspect>
    </aop:config>
	
2、添加aspectJ支持 <aop:aspectj-autoproxy />  （注意不能是：<aop:aspectj-autoproxy proxy-target-class="true" />强制使用CGLIB代理 ,需要被代理对象有默认构造方法，也就是说不能代理接口。 使用默认：自动在JDK动态代理和CGLIB之间转换 ，jdk的默认代理必须要目标类实现接口）

3、在mapper方法上添加注解
  @Resource
  public interface InfoMapper {
    int count(InfoQUERY query);
	  @Page("count")
	  List<InfoDO> query(InfoQUERY query);
  }
  分页对象必须implements PageInfo
  public class InfoQUERY implements PageInfo{
    @Override
    public void setCount(long count) {
      this.count = count;
    }
    @Override
    public boolean skipCount() {
      return pageSize <= 0;
    }
  }
  
Page注解
分页标记注解，设置在mapper方法上表明当前方法需要进行分页统计，并且分页统计方法value，如:
@Page("count")
List queryPage(BaseInfoQUERY query);
表示调用queryPage时需要分页，并且分页方法是"count"。
注意query对象必须实现PageInfo接口，并且 queryPage对应的sql语句需要自己写limit子句。
也就是说Page注解实际上只是自动执行指定统计方法，并将结果通过PageInfo接口设置。
另外，注意统计方法count参数列表必须和queryPage一致！并且返回值必须是int或者long

