package br.com.oficina.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor flywayDependencyPostProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                String[] candidates = { "entityManagerFactory", "jpaSharedEM_entityManagerFactory" };
                for (String name : candidates) {
                    if (beanFactory.containsBeanDefinition(name)) {
                        BeanDefinition bd = beanFactory.getBeanDefinition(name);
                        String[] existing = bd.getDependsOn();
                        String[] updated = existing == null ? new String[]{ "flyway" }
                                : java.util.Arrays.copyOf(existing, existing.length + 1);
                        if (updated.length > 1) updated[updated.length - 1] = "flyway";
                        bd.setDependsOn(updated);
                    }
                }
            }
        };
    }
}
