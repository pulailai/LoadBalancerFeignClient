


### 场景
```
因早起过度设计导致项目拆分过于细，导致资源浪费，以及增加了项目的复杂度等；so开始重构合并项目吧！
```

### 合并后遇问题
   多个项目合并后，测试验证过后，就需要将被合并的项目下线。问题来了，被合并的项目中，向其它服务提供了@FeignClient api。那么就需要修改其它项目中引用的jar包，然后重启。呃.....受影响的项目有90多个。全部升级重启，风险很大。讨论.....
### 解决方案一
通过修改Eureka服务注册逻辑
spring.application.name=权限服务,商户服务,账号服务

```
public abstract class AbstractJerseyEurekaHttpClient implements EurekaHttpClient {
    private static final Logger logger = LoggerFactory.getLogger(AbstractJerseyEurekaHttpClient.class);
    protected final Client jerseyClient;
    protected final String serviceUrl;

    protected AbstractJerseyEurekaHttpClient(Client jerseyClient, String serviceUrl) {
        this.jerseyClient = jerseyClient;
        this.serviceUrl = serviceUrl;
        logger.debug("Created client for url: {}", serviceUrl);
    }

    public EurekaHttpResponse<Void> register(InstanceInfo info) {
        String urlPath = "apps/" + info.getAppName();
        ClientResponse response = null;

        EurekaHttpResponse var5;
        try {
            Builder resourceBuilder = this.jerseyClient.resource(this.serviceUrl).path(urlPath).getRequestBuilder();
            this.addExtraHeaders(resourceBuilder);
            response = (ClientResponse)((Builder)((Builder)((Builder)resourceBuilder.header("Accept-Encoding", "gzip")).type(MediaType.APPLICATION_JSON_TYPE)).accept(new String[]{"application/json"})).post(ClientResponse.class, info);
            var5 = EurekaHttpResponse.anEurekaHttpResponse(response.getStatus()).headers(headersOf(response)).build();
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("Jersey HTTP POST {}/{} with instance {}; statusCode={}", new Object[]{this.serviceUrl, urlPath, info.getId(), response == null ? "N/A" : response.getStatus()});
            }

            if (response != null) {
                response.close();
            }

        }

        return var5;
    }
```

修改后：

```

    public EurekaHttpResponse<Void> register(InstanceInfo instanceInfo) {
        List<InstanceInfo> infos = this.getInstanceInfo(instanceInfo);
        EurekaHttpResponse<Void> result = null;
        Iterator var4 = infos.iterator();

        while(var4.hasNext()) {
            InstanceInfo info = (InstanceInfo)var4.next();
            String urlPath = "apps/" + info.getAppName();
            ClientResponse response = null;

            try {
                Builder resourceBuilder = this.jerseyClient.resource(this.serviceUrl).path(urlPath).getRequestBuilder();
                this.addExtraHeaders(resourceBuilder);
                response = (ClientResponse)((Builder)((Builder)((Builder)resourceBuilder.header("Accept-Encoding", "gzip")).type(MediaType.APPLICATION_JSON_TYPE)).accept(new String[]{"application/json"})).post(ClientResponse.class, info);
                result = EurekaHttpResponse.anEurekaHttpResponse(response.getStatus()).headers(headersOf(response)).build();
            } finally {
                if (logger.isDebugEnabled()) {
                    logger.debug("Jersey HTTP POST {}/{} with instance {}; statusCode={}", new Object[]{this.serviceUrl, instanceInfo.getAppName(), Integer.valueOf(-1), response == null ? "N/A" : response.getStatus()});
                }

                if (response != null) {
                    response.close();
                }

            }
        }

        return result;
    }

    private List<InstanceInfo> getInstanceInfo(InstanceInfo info) {
        if (!info.getAppName().contains(",")) {
            return Lists.newArrayList(new InstanceInfo[]{info});
        } else {
            String[] appNames = info.getAppName().split(",");
            List<InstanceInfo> result = new ArrayList(appNames.length);
            String[] var4 = appNames;
            int var5 = appNames.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                String app = var4[var6];
                String[] instance = info.getInstanceId().split(":");
                String appLower = app.toLowerCase();
                PortWrapper insec = new PortWrapper(true, info.getPort());
                PortWrapper sec = new PortWrapper(false, info.getSecurePort());
                InstanceInfo item = new InstanceInfo(instance[0] + ":" + appLower + ":" + instance[2], app, info.getAppGroupName(), info.getIPAddr(), info.getSID(), insec, sec, info.getHomePageUrl(), info.getStatusPageUrl(), info.getHealthCheckUrl(), info.getSecureHealthCheckUrl(), appLower, appLower, info.getCountryId(), info.getDataCenterInfo(), info.getHostName(), info.getStatus(), info.getOverriddenStatus(), info.getLeaseInfo(), info.isCoordinatingDiscoveryServer(), new HashMap(info.getMetadata()), info.getLastUpdatedTimestamp(), info.getLastDirtyTimestamp(), info.getActionType(), info.getASGName());
                result.add(item);
            }

            return result;
        }
    }

    private List<String> getAppNameAndId(String appNames, String ids) {
        if (!appNames.contains(",")) {
            return Lists.newArrayList(new String[]{"apps/" + appNames + "/" + ids});
        } else {
            String[] appName = appNames.split(",");
            List<String> result = new ArrayList(appName.length);

            for(int i = 0; i < appName.length; ++i) {
                result.add("apps/" + appName[i] + "/" + ids);
            }

            return result;
        }
    }

    public EurekaHttpResponse<Void> cancel(String appName, String id) {
        List<String> nameIds = this.getAppNameAndId(appName, id);
        EurekaHttpResponse<Void> result = null;
        Iterator var5 = nameIds.iterator();

        while(var5.hasNext()) {
            String urlPath = (String)var5.next();
            ClientResponse response = null;

            try {
                Builder resourceBuilder = this.jerseyClient.resource(this.serviceUrl).path(urlPath).getRequestBuilder();
                this.addExtraHeaders(resourceBuilder);
                response = (ClientResponse)resourceBuilder.delete(ClientResponse.class);
                result = EurekaHttpResponse.anEurekaHttpResponse(response.getStatus()).headers(headersOf(response)).build();
            } finally {
                if (logger.isDebugEnabled()) {
                    logger.debug("Jersey HTTP DELETE {}/{}; statusCode={}", new Object[]{this.serviceUrl, id, response == null ? "N/A" : response.getStatus()});
                }

                if (response != null) {
                    response.close();
                }

            }
        }

        return result;
    }

    public EurekaHttpResponse<InstanceInfo> sendHeartBeat(String appName, String id, InstanceInfo info, InstanceStatus overriddenStatus) {
        List<String> nameIds = this.getAppNameAndId(appName, id);
        EurekaHttpResponse<InstanceInfo> result = null;
        Iterator var7 = nameIds.iterator();

        while(var7.hasNext()) {
            String urlPath = (String)var7.next();
            ClientResponse response = null;
            WebResource webResource = this.jerseyClient.resource(this.serviceUrl).path(urlPath).queryParam("status", info.getStatus().toString()).queryParam("lastDirtyTimestamp", info.getLastDirtyTimestamp().toString());
            if (overriddenStatus != null) {
                webResource = webResource.queryParam("overriddenstatus", overriddenStatus.name());
            }

            try {
                Builder requestBuilder = webResource.getRequestBuilder();
                this.addExtraHeaders(requestBuilder);
                response = (ClientResponse)requestBuilder.put(ClientResponse.class);
                EurekaHttpResponseBuilder<InstanceInfo> eurekaResponseBuilder = EurekaHttpResponse.anEurekaHttpResponse(response.getStatus(), InstanceInfo.class).headers(headersOf(response));
                if (response.hasEntity()) {
                    eurekaResponseBuilder.entity(response.getEntity(InstanceInfo.class));
                }

                result = eurekaResponseBuilder.build();
            } finally {
                if (logger.isDebugEnabled()) {
                    logger.debug("Jersey HTTP PUT {}/{}; statusCode={}", new Object[]{this.serviceUrl, appName, response == null ? "N/A" : response.getStatus()});
                }

                if (response != null) {
                    response.close();
                }

            }
        }

        return result;
    }

......
```



### 解决方案二
通过重写LoadBalancerFeignClient 类中的execute方法，实现对Feign的请求url重写

```
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

```

```
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

```

    



