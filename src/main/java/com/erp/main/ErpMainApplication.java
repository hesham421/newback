package com.erp.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;

/**
 * HibernateJpaAutoConfiguration is excluded here — {@code JpaConfig} builds the
 * EntityManagerFactory/TransactionManager by hand so it can read the
 * {@code spring.jpa.*} placeholders explicitly (see application.properties).
 */
@SpringBootApplication(
    scanBasePackages = "com.erp",
    exclude = HibernateJpaAutoConfiguration.class
)
public class ErpMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpMainApplication.class, args);
    }
}
