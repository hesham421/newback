package com.erp.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.erp.file.service.DownloadTokenStore;
import com.erp.file.service.InMemoryDownloadTokenStore;
import com.erp.file.service.RedisDownloadTokenStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Which {@link DownloadTokenStore} the auto-configuration picks, with and without Redis. */
class DownloadTokenStoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DownloadTokenStoreAutoConfiguration.class));

    @Test
    void withoutRedis_usesTheInMemoryStore() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(DownloadTokenStore.class);
            assertThat(context.getBean(DownloadTokenStore.class)).isInstanceOf(InMemoryDownloadTokenStore.class);
        });
    }

    @Test
    void withAStringRedisTemplate_usesTheRedisStoreOnly() {
        runner.withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .run(context -> {
                assertThat(context).hasSingleBean(DownloadTokenStore.class);
                assertThat(context.getBean(DownloadTokenStore.class)).isInstanceOf(RedisDownloadTokenStore.class);
            });
    }
}
