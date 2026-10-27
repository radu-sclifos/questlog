package com.radusclifos.questlog.entry;

import com.radusclifos.questlog.user.User;
import com.radusclifos.questlog.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class EntryStatusMigrationTests {
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;

    @Test void currentSchemaDefaultsStatusAndRejectsNull() {
        var user=users.saveAndFlush(new User("status-default-test","Test","hash"));
        String status=jdbc.queryForObject("INSERT INTO entries(user_id,title) VALUES(?,?) RETURNING status",
            String.class,user.getId(),"Default status");
        assertEquals("UNPLAYED",status);
        assertEquals("NO",jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='entries' AND column_name='status'",String.class));
    }
}
