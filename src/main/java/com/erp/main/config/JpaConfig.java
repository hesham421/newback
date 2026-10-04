package com.erp.main.config;

import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Hand-rolled JPA bootstrap — HibernateJpaAutoConfiguration is excluded in
 * {@code ErpMainApplication}, so this class owns the EntityManagerFactory and
 * TransactionManager and reads the {@code spring.jpa.*} placeholders directly.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.erp")
@EnableTransactionManagement
@EntityScan(basePackages = "com.erp")
public class JpaConfig {

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String hbm2ddlAuto;

    @Value("${spring.jpa.show-sql}")
    private boolean showSql;

    @Value("${spring.jpa.properties.hibernate.dialect}")
    private String dialect;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String defaultSchema;

    @Value("${spring.jpa.properties.hibernate.format_sql}")
    private boolean formatSql;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.erp");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        factory.setJpaVendorAdapter(vendorAdapter);

        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto", hbm2ddlAuto);
        jpaProperties.put("hibernate.show_sql", showSql);
        jpaProperties.put("hibernate.dialect", dialect);
        jpaProperties.put("hibernate.default_schema", defaultSchema);
        jpaProperties.put("hibernate.format_sql", formatSql);
        factory.setJpaProperties(jpaProperties);

        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslationPostProcessor() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
}
