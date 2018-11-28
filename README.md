


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

    



