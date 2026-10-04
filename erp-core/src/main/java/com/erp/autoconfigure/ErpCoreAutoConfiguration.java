package com.erp.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.util.Streamable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * The erp-core composition root, applied to any Spring Boot application that has erp-core on its
 * classpath. It component-scans, entity-scans and registers the JPA repositories of the core
 * packages ({@link #CORE_PACKAGES}), and relies on Boot's standard
 * {@code HibernateJpaAutoConfiguration} for the EntityManagerFactory (dialect auto-detected, no
 * default schema set by core).
 *
 * <p>Declaring core entity-scan and repository packages switches off Boot's default "scan the
 * {@code @SpringBootApplication} package" behaviour for entities and repositories, so the
 * application's auto-configuration packages are added to the entity scan, and to the repository
 * scan unless the application registers its own repositories. Overlapping packages are collapsed,
 * and a repository the application already registered is never registered again
 * (see {@link CoreJpaRepositoriesRegistrar}).
 *
 * <p>Also contributes the {@code messageSource}: the core i18n bundle ({@value #CORE_MESSAGES_BASENAME})
 * behind any bundles the application lists in {@code spring.messages.basename}, so application keys
 * win and core error codes always resolve.
 */
@AutoConfiguration(before = {
    HibernateJpaAutoConfiguration.class,
    DataJpaRepositoriesAutoConfiguration.class,
    MessageSourceAutoConfiguration.class})
@EnableConfigurationProperties(ErpCoreProperties.class)
@ComponentScan(basePackages = ErpCoreAutoConfiguration.CORE_PACKAGE_LIST, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)})
@Import({ErpCoreAutoConfiguration.CoreEntityScanRegistrar.class,
    ErpCoreAutoConfiguration.CoreJpaRepositoriesRegistrar.class})
public class ErpCoreAutoConfiguration {

    /**
     * The core packages, comma-separated — <b>the one place to edit</b> when a step adds a core
     * package (tenant in 05, events in 08, sequence in 09, audit in 10, report in 11). It is a
     * {@code String} because an annotation attribute cannot reference a {@code String[]} constant;
     * {@code @ComponentScan} tokenizes it on commas, and {@link #CORE_PACKAGES} is derived from it.
     */
    public static final String CORE_PACKAGE_LIST =
        "com.erp.common,com.erp.cu,com.erp.mdl,com.erp.sec,com.erp.file,com.erp.notif,com.erp.tenant"
            + ",com.erp.events"
            + ",com.erp.sequence"
            + ",com.erp.audit"
            + ",com.erp.report";

    /** The core packages: component scan, entity scan and JPA repositories all use exactly these. */
    public static final String[] CORE_PACKAGES = StringUtils.commaDelimitedListToStringArray(CORE_PACKAGE_LIST);

    /** Classpath basename of the core message bundles ({@code messages.properties}, {@code messages_ar.properties}). */
    public static final String CORE_MESSAGES_BASENAME = "i18n/messages";

    /**
     * Application bundles first ({@code spring.messages.basename}, when set), then the core bundle.
     * UTF-8 by default, and no fallback to the JVM locale: on a host whose default locale is Arabic
     * a fallback would answer English requests with Arabic messages.
     */
    @Bean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME)
    @ConditionalOnMissingBean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME,
        search = SearchStrategy.CURRENT)
    public MessageSource messageSource(Environment environment) {
        List<String> basenames = new ArrayList<>();
        for (String basename : StringUtils.commaDelimitedListToStringArray(
                environment.getProperty("spring.messages.basename", ""))) {
            String trimmed = basename.trim();
            if (!trimmed.isEmpty() && !trimmed.equals(CORE_MESSAGES_BASENAME)) {
                basenames.add(trimmed);
            }
        }
        basenames.add(CORE_MESSAGES_BASENAME);

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames(basenames.toArray(String[]::new));
        messageSource.setDefaultEncoding(environment.getProperty("spring.messages.encoding", "UTF-8"));
        messageSource.setFallbackToSystemLocale(
            environment.getProperty("spring.messages.fallback-to-system-locale", Boolean.class, false));
        messageSource.setUseCodeAsDefaultMessage(
            environment.getProperty("spring.messages.use-code-as-default-message", Boolean.class, false));
        messageSource.setAlwaysUseMessageFormat(
            environment.getProperty("spring.messages.always-use-message-format", Boolean.class, false));
        return messageSource;
    }

    /**
     * Removes every package already covered by another entry ({@code com.erp} covers
     * {@code com.erp.sec}) and exact duplicates, keeping first-seen order. Scanning overlapping
     * packages would find the same entity or repository twice.
     */
    static String[] collapsePackages(Collection<String> packages) {
        Set<String> unique = new LinkedHashSet<>();
        for (String pkg : packages) {
            if (StringUtils.hasText(pkg)) {
                unique.add(pkg.trim());
            }
        }
        List<String> result = new ArrayList<>();
        for (String pkg : unique) {
            boolean covered = unique.stream()
                .anyMatch(other -> !other.equals(pkg) && pkg.startsWith(other + "."));
            if (!covered) {
                result.add(pkg);
            }
        }
        return result.toArray(String[]::new);
    }

    /** The application's auto-configuration packages ({@code @SpringBootApplication} package), if any. */
    static List<String> applicationPackages(BeanFactory beanFactory) {
        return beanFactory != null && AutoConfigurationPackages.has(beanFactory)
            ? AutoConfigurationPackages.get(beanFactory) : List.of();
    }

    /** Registers the core packages plus the application packages, collapsed, for Boot's JPA entity scanning. */
    static class CoreEntityScanRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

        private BeanFactory beanFactory;

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            List<String> packages = new ArrayList<>(Arrays.asList(CORE_PACKAGES));
            packages.addAll(applicationPackages(beanFactory));
            EntityScanPackages.register(registry, collapsePackages(packages));
        }
    }

    /**
     * Registers the Spring Data JPA repositories of the core packages, backing off per repository:
     * <ul>
     *   <li>The application's auto-configuration packages are added only when the application has
     *       registered no JPA repositories of its own (no {@code @EnableJpaRepositories}), mirroring
     *       Boot's own {@code DataJpaRepositoriesAutoConfiguration}, which backs off in that case.</li>
     *   <li>A repository interface that is already registered (for example by an application
     *       {@code @EnableJpaRepositories} covering {@code com.erp}) is skipped, so every core
     *       repository is registered exactly once.</li>
     *   <li>Overlapping packages are collapsed first (see {@link #collapsePackages}).</li>
     * </ul>
     * Application configuration is processed before auto-configurations, so the application's own
     * repository definitions are already in the registry when this runs. An
     * {@code @EnableJpaRepositories} annotation can express none of this, nor take the derived array.
     */
    static class CoreJpaRepositoriesRegistrar
            implements ImportBeanDefinitionRegistrar, BeanFactoryAware, ResourceLoaderAware, EnvironmentAware {

        private BeanFactory beanFactory;
        private ResourceLoader resourceLoader;
        private Environment environment;

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }

        @Override
        public void setResourceLoader(ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }

        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry,
                                            BeanNameGenerator importBeanNameGenerator) {
            Set<String> alreadyRegistered = registeredRepositoryInterfaces(registry);
            List<String> packages = new ArrayList<>(Arrays.asList(CORE_PACKAGES));
            if (alreadyRegistered.isEmpty()) {
                packages.addAll(applicationPackages(beanFactory));
            }
            String[] basePackages = collapsePackages(packages);

            AnnotationRepositoryConfigurationSource source = new AnnotationRepositoryConfigurationSource(
                    AnnotationMetadata.introspect(EnableCoreJpaRepositories.class), EnableJpaRepositories.class,
                    resourceLoader, environment, registry, importBeanNameGenerator) {

                @Override
                public Streamable<String> getBasePackages() {
                    return Streamable.of(basePackages);
                }

                @Override
                public Streamable<BeanDefinition> getCandidates(ResourceLoader loader) {
                    return super.getCandidates(loader)
                        .filter(candidate -> !alreadyRegistered.contains(candidate.getBeanClassName()));
                }

                @Override
                public BootstrapMode getBootstrapMode() {
                    return BootstrapMode.DEFAULT;
                }
            };
            new RepositoryConfigurationDelegate(source, resourceLoader, environment)
                .registerRepositoriesIn(registry, new JpaRepositoryConfigExtension());
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            registerBeanDefinitions(metadata, registry, null);
        }

        /** Repository interface names of every Spring Data repository factory bean already defined. */
        static Set<String> registeredRepositoryInterfaces(BeanDefinitionRegistry registry) {
            Set<String> interfaces = new LinkedHashSet<>();
            for (String name : registry.getBeanDefinitionNames()) {
                BeanDefinition definition = registry.getBeanDefinition(name);
                if (!isRepositoryFactoryBean(definition.getBeanClassName())) {
                    continue;
                }
                Object type = definition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
                if (type == null) {
                    ConstructorArgumentValues args = definition.getConstructorArgumentValues();
                    ConstructorArgumentValues.ValueHolder holder = args.getIndexedArgumentValue(0, null);
                    if (holder == null && !args.getGenericArgumentValues().isEmpty()) {
                        holder = args.getGenericArgumentValues().get(0);
                    }
                    type = holder != null ? holder.getValue() : null;
                }
                String interfaceName = typeName(type);
                if (interfaceName != null) {
                    interfaces.add(interfaceName);
                }
            }
            return interfaces;
        }

        private static boolean isRepositoryFactoryBean(String beanClassName) {
            if (beanClassName == null) {
                return false;
            }
            try {
                return RepositoryFactoryBeanSupport.class.isAssignableFrom(
                    ClassUtils.forName(beanClassName, CoreJpaRepositoriesRegistrar.class.getClassLoader()));
            } catch (ClassNotFoundException | LinkageError e) {
                return false;
            }
        }

        private static String typeName(Object type) {
            if (type instanceof Class<?> clazz) {
                return clazz.getName();
            }
            if (type instanceof ResolvableType resolvable) {
                Class<?> resolved = resolvable.resolve();
                return resolved != null ? resolved.getName() : null;
            }
            if (type instanceof String text && !text.isBlank()) {
                return text;
            }
            return null;
        }

        @EnableJpaRepositories
        private static final class EnableCoreJpaRepositories {
        }
    }
}
