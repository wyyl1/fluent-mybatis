package cn.org.atool.fluent.mybatis.generator.annoatation;

import java.lang.annotation.*;

import static cn.org.atool.fluent.mybatis.mapper.StrConstant.NOT_DEFINED;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Table {
    /**
     * 表名称
     *
     * @return
     */
    String value();

    /**
     * 排除字段列表
     *
     * @return
     */
    String[] excludes() default {};

    /**
     * 显式指定字段转换属性
     *
     * @return
     */
    Column[] columns() default {};

    /**
     * 生成Entity文件时, 需要去除的表前缀
     *
     * @return
     */
    String[] tablePrefix() default {};

    /**
     * 生成Mapper bean时在bean name前缀
     *
     * @return
     */
    String mapperPrefix() default NOT_DEFINED;
}
