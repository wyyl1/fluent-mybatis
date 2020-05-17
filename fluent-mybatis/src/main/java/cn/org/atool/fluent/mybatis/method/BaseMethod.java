package cn.org.atool.fluent.mybatis.method;

import cn.org.atool.fluent.mybatis.method.model.MapperParam;
import cn.org.atool.fluent.mybatis.method.model.SqlBuilder;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.incrementer.IKeyGenerator;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 * BaseMethod, 后续替换掉 AbstractMethod
 *
 * @author darui.wu
 * @create 2020/5/14 1:35 下午
 */
@Slf4j
public abstract class BaseMethod extends AbstractMethod {

    /**
     * 添加 MappedStatement 到 Mybatis 容器
     *
     * @param param
     * @return
     */
    protected MappedStatement addMappedStatement(MapperParam param) {
        if (hasMappedStatement(param.getStatementId())) {
            log.warn(LEFT_SQ_BRACKET + param.getStatementId() + "] Has been loaded, so ignoring this injection for [" + getClass() + RIGHT_SQ_BRACKET);
            return null;
        } else {
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, param.getSql(), param.getParameterType());
            return builderAssistant.addMappedStatement(
                param.getStatementId(),
                sqlSource,
                StatementType.PREPARED,
                param.getSqlCommandType(),
                null,
                null,
                null,
                param.getParameterType(),
                param.getResultMap(),
                param.getResultType(),
                null,
                param.isFlushCache(),
                param.isUseCache(),
                false,
                param.getKeyGenerator(),
                param.getKeyProperty(),
                param.getKeyColumn(),
                configuration.getDatabaseId(),
                languageDriver,
                null);
        }
    }

    /**
     * 是否已经存在MappedStatement
     *
     * @param mappedStatement MappedStatement
     * @return true or false
     */
    private boolean hasMappedStatement(String mappedStatement) {
        return configuration.hasStatement(mappedStatement, false);
    }


    /**
     * 设置主键和主键生产器
     *
     * @param mapper
     * @param table
     */
    protected void setKeyGenerator(MapperParam mapper, TableInfo table) {
        String keyProperty = table.getKeyProperty();
        if (keyProperty == null || "".equals(keyProperty.trim())) {
            return;
        }
        mapper.setKeyProperty(keyProperty).setKeyColumn(table.getKeyColumn());
        if (table.getIdType() == IdType.AUTO) {
            mapper.setKeyGenerator(new Jdbc3KeyGenerator());
        } else if (table.getKeySequence() != null) {
            mapper.setKeyGenerator(genKeyGenerator(table, mapper));
        }
    }

    /**
     * 自定义 KEY 生成器
     *
     * @param table
     * @param mapper
     * @return
     */
    public KeyGenerator genKeyGenerator(TableInfo table, MapperParam mapper) {
        IKeyGenerator keyGenerator = GlobalConfigUtils.getKeyGenerator(this.configuration);
        if (null == keyGenerator) {
            throw new IllegalArgumentException("not configure IKeyGenerator implementation class.");
        }
        String sql = keyGenerator.executeSql(table.getKeySequence().value());
        MapperParam keyMapper = new MapperParam(mapper.getStatementId() + "!selectKey")
            .setSqlCommandType(SqlCommandType.SELECT)
            .setResultType(table.getKeySequence().clazz())
            .setFlushCache(false)
            .setUseCache(false)
            .setKeyGenerator(new NoKeyGenerator())
            .setKeyProperty(table.getKeyProperty())
            .setKeyColumn(table.getKeyColumn())
            .setSql(sql)
            .setParameterType(null);

        this.addMappedStatement(keyMapper);

        String statementId = builderAssistant.applyCurrentNamespace(keyMapper.getStatementId(), false);
        MappedStatement keyStatement = this.configuration.getMappedStatement(statementId, false);
        SelectKeyGenerator selectKeyGenerator = new SelectKeyGenerator(keyStatement, true);
        this.configuration.addKeyGenerator(statementId, selectKeyGenerator);
        return selectKeyGenerator;
    }

    /**
     * 返回包含主键的字段拼接
     *
     * @param table
     * @return
     */
    protected String getColumnsWithPrimary(TableInfo table) {
        List<String> list = new ArrayList<>();
        if (table.getKeyColumn() != null) {
            list.add(table.getKeyColumn());
        }
        table.getFieldList().forEach(field -> list.add(field.getColumn()));
        return list.stream().collect(joining(", "));
    }

    /**
     * where部分
     *
     * @param table
     * @param builder
     * @return
     */
    protected SqlBuilder whereEntity(TableInfo table, SqlBuilder builder) {
        return builder
            .ifThen("ew != null and ew.entity != null", () -> {
                builder
                    .ifThen("ew.entity.@property != null", "@column=#{ew.entity.@column}", table.getKeyProperty(), table.getKeyColumn())
                    .eachJoining(table.getFieldList(),
                        (field) -> builder.ifThen(
                            "ew.entity.@property != null", "AND @column=#{ew.entity.@property}", field.getProperty(), field.getColumn()
                        ));
            })
            .ifThen("ew != null and ew.sqlSegment != null and ew.sqlSegment != ''", "AND ${ew.sqlSegment}");
    }

    /**
     * where id = #{id}
     * 没有主键的表，需要特殊处理，避免扫表
     *
     * @param table
     * @param builder
     * @return
     */
    protected SqlBuilder whereById(TableInfo table, SqlBuilder builder) {
        if (table.getKeyProperty() == null) {
            return builder.append("1!=1");
        } else {
            return builder.value("@column=#{et.@property}", table.getKeyProperty(), table.getKeyColumn());
        }
    }

    /**
     * where id in(?, ?, ?)
     * 没有主键的表，需要特殊处理，避免扫表
     *
     * @param table
     * @param builder
     * @return
     */
    protected SqlBuilder whereByIds(TableInfo table, SqlBuilder builder) {
        if (table.getKeyProperty() == null) {
            return builder.append("1!=1");
        } else {
            return builder
                .value("@property IN (", table.getKeyProperty(), null)
                .foreach("coll", "item", ",", () -> builder.append("#{item}"))
                .append(")");
        }
    }

    /**
     * 根据map条件查询
     *
     * @param table
     * @param builder
     */
    protected void whereByMap(TableInfo table, SqlBuilder builder) {
        builder.ifThen("cm != null and !cm.isEmpty", () -> {
            builder.foreach("cm", "v", "AND ", () -> {
                builder.choose("v == null", "${k} IS NULL ", "${k} = #{v}");
            });
        });
    }

    protected SqlBuilder comment(SqlBuilder builder) {
        return builder.ifThen("ew != null and ew.sqlComment != null", "${ew.sqlComment}");
    }


    /**
     * 返回方法具体的sql语句
     *
     * @param table 表结构信息
     * @return
     */
    protected abstract String getMethodSql(TableInfo table);
}