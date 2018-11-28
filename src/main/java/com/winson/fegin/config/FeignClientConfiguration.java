package com.winson.fegin.config;

import com.google.common.collect.ImmutableMap;
import com.winson.fegin.CustomLoadBalancerFeignClient;
import com.winson.fegin.CustomLoadBalancerProperties;
import feign.Client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.feign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public Client feignClient(CachingSpringLoadBalancerFactory cachingFactory,
                              SpringClientFactory clientFactory) {
        CustomLoadBalancerProperties customLoadBalancerProperties = CustomLoadBalancerProperties.builder().hosts(ImmutableMap.of(
                "服务A", "服务B",
                "服务C", "服务B")).build();
        return new CustomLoadBalancerFeignClient(new Client.Default(null, null),
                cachingFactory, clientFactory, customLoadBalancerProperties);
    }

}
