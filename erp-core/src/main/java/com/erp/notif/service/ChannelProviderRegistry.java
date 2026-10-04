package com.erp.notif.service;

import com.erp.notif.channel.ChannelProvider;
import com.erp.notif.channel.InAppChannelProvider;
import com.erp.notif.channel.LoggingChannelProvider;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Picks the {@link ChannelProvider} for a channel code (erp-core step 08):
 * <ol>
 *   <li>an application-defined provider bean for the channel, if any (it replaces the core one);</li>
 *   <li>otherwise the core provider ({@code EmailChannelProvider}, {@link InAppChannelProvider});</li>
 *   <li>otherwise a {@link LoggingChannelProvider}, which answers {@code SKIPPED_NO_PROVIDER}.</li>
 * </ol>
 * Providers are looked up on every call, so the registry has no state of its own.
 */
@Component
@RequiredArgsConstructor
public class ChannelProviderRegistry {

    /** By name: naming {@code EmailChannelProvider.class} would load Spring Mail types, which are optional. */
    private static final Set<String> CORE_PROVIDERS = Set.of(
        "com.erp.notif.channel.EmailChannelProvider", InAppChannelProvider.class.getName());

    private final ObjectProvider<ChannelProvider> providers;

    /** The provider for {@code channel}; never {@code null}. */
    public ChannelProvider resolve(String channel) {
        ChannelProvider core = null;
        for (ChannelProvider provider : providers.orderedStream().toList()) {
            if (!channel.equals(provider.channel())) {
                continue;
            }
            if (!CORE_PROVIDERS.contains(AopUtils.getTargetClass(provider).getName())) {
                return provider;
            }
            if (core == null) {
                core = provider;
            }
        }
        return core != null ? core : new LoggingChannelProvider(channel);
    }
}
