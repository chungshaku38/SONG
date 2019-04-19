package bio.overture.song.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DataInitializr {
    @Autowired
    DataSource dataSource;

    @EventListener({ApplicationReadyEvent.class})
    public void prepareTestData() {
        ResourceDatabasePopulator databasePurger = new ResourceDatabasePopulator(new ClassPathResource("purge.sql"));
        databasePurger.execute(dataSource);

        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator(new ClassPathResource("data.sql"));
        databasePopulator.execute(dataSource);
    }
}