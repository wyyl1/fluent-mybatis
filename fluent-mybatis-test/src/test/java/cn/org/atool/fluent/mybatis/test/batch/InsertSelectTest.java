package cn.org.atool.fluent.mybatis.test.batch;

import cn.org.atool.fluent.mybatis.base.BatchCrud;
import cn.org.atool.fluent.mybatis.base.model.FieldMapping;
import cn.org.atool.fluent.mybatis.generate.ATM;
import cn.org.atool.fluent.mybatis.generate.entity.StudentEntity;
import cn.org.atool.fluent.mybatis.generate.helper.StudentMapping;
import cn.org.atool.fluent.mybatis.generate.mapper.StudentMapper;
import cn.org.atool.fluent.mybatis.generate.wrapper.StudentQuery;
import cn.org.atool.fluent.mybatis.test.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class InsertSelectTest extends BaseTest {
    @Autowired
    private StudentMapper mapper;

    @Test
    void testInsertSelect() {
        ATM.dataMap.student.table(3)
            .address.values("address1", "address2", "address3")
            .age.values(34, 45, 55)
            .cleanAndInsert();
        int count = mapper.insertSelect(new String[]{"address", "age"},
            new StudentQuery()
                .select.address().age().end()
                .where.id().in(new long[]{1, 2, 3}).end()
        );
        db.sqlList().wantFirstSql()
            .eq("INSERT INTO student (address,age) SELECT address, age FROM student WHERE id IN (?, ?, ?)");
        want.number(count).eq(3);
        ATM.dataMap.student.table(6)
            .address.values("address1", "address2", "address3", "address1", "address2", "address3")
            .age.values(34, 45, 55, 34, 45, 55)
            .eqTable();
    }

    @Test
    void testBatchInsertSelect() {
        ATM.dataMap.student.table().clean();
        mapper.batchCrud(BatchCrud.batch()
            .addInsert(newStudent("user1"), newStudent("user2"), newStudent("test1"))
            .addInsertSelect(ATM.table.student, new FieldMapping[]{StudentMapping.userName},
                new StudentQuery().select.userName().end()
                    .where.userName().likeRight("user").end())
        );

        db.sqlList().wantFirstSql()
            .containsInOrder("INSERT INTO student(gmt_created, gmt_modified, is_deleted, env, tenant, user_name)",
                "INSERT INTO student(gmt_created, gmt_modified, is_deleted, env, tenant, user_name)",
                "INSERT INTO student(gmt_created, gmt_modified, is_deleted, env, tenant, user_name)",
                "INSERT INTO student (user_name) SELECT user_name FROM student WHERE user_name LIKE ?");
        ATM.dataMap.student.table(5)
            .userName.values("user1", "user2", "test1", "user1", "user2")
            .eqTable();
    }

    private StudentEntity newStudent(String name) {
        return new StudentEntity().setUserName(name);
    }
}