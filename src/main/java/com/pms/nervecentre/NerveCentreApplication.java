package com.pms.nervecentre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

// TODO: Remember to remove in phase 2
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
public class NerveCentreApplication {

    public static void main(String[] args) {
        SpringApplication.run(NerveCentreApplication.class, args);
    }

}
