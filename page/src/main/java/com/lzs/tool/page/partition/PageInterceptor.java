package com.lzs.tool.page.partition;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分页拦截器,拦截指定包下所有方法,并过滤出有{@link Page}注解的方法
 * 
 * @author lzs
 *
 */
public class PageInterceptor {
    protected final Logger log = LoggerFactory.getLogger(PageInterceptor.class);

    /**
     * 注解和注解上指定的统计方法实例映射 String : class | value
     */
    private Map<String, Method> countMethodMap = new ConcurrentHashMap<>();
    /**
     * 分页对象在参数列表中的索引
     */
    private Map<Method, Integer> pageArgIndexMap = new ConcurrentHashMap<>();

    // //配置切入点,该方法无方法体,主要为方便同类中其他方法使用此处配置的切入点
    // @Pointcut("@annotation(com.weidai.wpai.dal.page.Page)") //
    // 接口上的注解,尝试了多种方式，没有办法拦截
    // public void aspect() {
    // }
    // @Pointcut("execution(* (com.weidai.wpai.dal.auction.mapper.*||com.weidai.wpai.dal.trade.dao.*).*(..))")
    // com.weidai.wpai.dal.mapper包及其子包下的所有方法
    // public void aspect() {
    // }

    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // bean
        Object target = joinPoint.getTarget();
        // 参数数组
        Object[] args = joinPoint.getArgs();
        // 数据查询方法对象
        Signature sig = joinPoint.getSignature();
        MethodSignature msig = (MethodSignature) sig;
        // 获取方法上的page注解
        Page annotation = msig.getMethod().getAnnotation(Page.class);

        // 不存在page注解，跳过
        if (annotation == null) {
            return joinPoint.proceed(joinPoint.getArgs());
        }

        // 判断是否是处理过的方法，若没有处理过，则进行检查处理
        if (!countMethodMap.containsKey(getKey(msig, annotation))) {
            // 统计总数的方法名
            String countMethodName = annotation.value();
            // 通过方法名获取统计总数的方法
            Method countMethod = target.getClass().getMethod(countMethodName, msig.getParameterTypes());
            if (countMethod == null) {
                // 当前被拦截方法名
                String queryMethodName = msig.getName();
                // 方法参数列表字符串
                String methodParams = getParamsString(msig);
                throw new PageInterceptException("Count method with the same(order/number/type) parameters for '"
                        + queryMethodName + "(" + methodParams + ")' is not exist!");
            }
            // 检查返回值是否是数值类型
            Class<?> type = countMethod.getReturnType();
            if (type != int.class && type != long.class && type != Integer.class && type != Long.class) {// 不是数值类型，抛异常
                // 方法参数列表字符串
                String methodParams = getParamsString(msig);
                throw new PageInterceptException("The return type of method '" + countMethod + "(" + methodParams
                        + ")' isn't numberic!");
            }

            // 获取参数列表上的PageInfo实例
            PageInfo page = null;
            int idx = 0;
            for (Object obj : args) {
                if (obj instanceof PageInfo) {
                    // 存在PageInfo实例
                    page = (PageInfo) obj;
                    pageArgIndexMap.put(countMethod, idx);
                    countMethodMap.put(getKey(msig, annotation), countMethod);
                    break;
                }
                idx++;
            }
            if (page == null) {// 没有分页对象，抛异常
                // 当前被拦截方法名
                String queryMethodName = msig.getName();
                // 方法参数列表字符串
                String methodParams = getParamsString(msig);
                throw new PageInterceptException("No param of type '" + PageInfo.class.getName() + "' in method '"
                        + queryMethodName + "(" + methodParams + ")' parameter list!");
            }
        }

        // 获取当前方法对应的统计总数方法
        Method countMethod = countMethodMap.get(getKey(msig, annotation));
        if (countMethod == null) {
            // 不存在表明不需要统计，直接执行
            return joinPoint.proceed(args);
        } else {
            // 获取PageInfo分页对象，并将总数设置进去
            int idx = pageArgIndexMap.get(countMethod);
            PageInfo page = (PageInfo) args[idx];
            if (page.skipCount()) {
                // 不执行统计
                return joinPoint.proceed(args);
            } else {
                // 调用统计方法,获取总数
                Object count = countMethod.invoke(target, args);
                long countNum = 0L;
                if (count instanceof Integer) {
                    countNum = ((Integer) count).longValue();
                } else {
                    countNum = (long) count;
                }
                page.setCount(countNum);
                // 总数大于0，执行数据查询，否则直接返回空列表
                if (countNum > 0) {
                    return joinPoint.proceed(args);
                } else {
                    return new ArrayList<Object>();
                }
            }
        }
    }

    private String getKey(MethodSignature msig, Page annotation) {
        return msig.toString() + "|" + annotation.value();
    }

    /**
     * 获取参数列表字符串
     * 
     * @param msig
     * @return
     */
    private static String getParamsString(MethodSignature msig) {
        StringBuffer sb = new StringBuffer();
        for (Class<?> type : msig.getParameterTypes()) {
            sb.append(",").append(type.getName());
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }
}
