package com.winson.fegin;

import feign.Client;
import feign.Request;
import feign.Response;
import org.springframework.cloud.netflix.feign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class CustomLoadBalancerFeignClient extends LoadBalancerFeignClient {

    private CustomLoadBalancerProperties customLoadBalancerProperties;

    public CustomLoadBalancerFeignClient(Client delegate,
                                         CachingSpringLoadBalancerFactory lbClientFactory,
                                         SpringClientFactory clientFactory,
                                         CustomLoadBalancerProperties customLoadBalancerProperties) {
        super(delegate, lbClientFactory, clientFactory);
        this.customLoadBalancerProperties = customLoadBalancerProperties;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        URI uri = URI.create(request.url());
        if (customLoadBalancerProperties != null && !CollectionUtils.isEmpty(customLoadBalancerProperties.getHosts())
                && customLoadBalancerProperties.getHosts().containsKey(uri.getHost())) {
            URI newUri = null;
            try {
                newUri = new URI(uri.getScheme(), uri.getUserInfo(), customLoadBalancerProperties.getHosts().get(uri.getHost()),
                        uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            if (newUri != null) {
                return super.execute(Request.create(request.method(), newUri.toString(),
                        request.headers(), request.body(), request.charset()), options);
            }
        }
        return super.execute(request, options);
    }
}
