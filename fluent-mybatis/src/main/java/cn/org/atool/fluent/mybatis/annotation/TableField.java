package cn.org.atool.fluent.mybatis.annotation;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.UnknownTypeHandler;

import java.lang.annotation.*;


/**
 * 表字段标识
 *
 * @author darui.wu
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TableField {
    /**
     * 字段值
     */
    String value();

    /**
     * 字段 update set 默认值
     */
    String update() default "";

    /**
     * insert的时候默认值
     *
     * @return
     */
    String insert() default "";

    /**
     * 是否进行显式的 select 查询
     * <p>大字段可设置为 false 使用 select(BaseFieldMeta::select)不加入 select 查询范围</p>
     */
    boolean select() default true;

    /**
     * JDBC类型 (该默认值不代表会按照该值生效)
     */
    JdbcType jdbcType() default JdbcType.UNDEFINED;

    /**
     * 类型处理器 (该默认值不代表会按照该值生效)
     */
    Class<? extends TypeHandler> typeHandler() default UnknownTypeHandler.class;

    /**
     * 指定小数点后保留的位数
     */
    String numericScale() default "";
}