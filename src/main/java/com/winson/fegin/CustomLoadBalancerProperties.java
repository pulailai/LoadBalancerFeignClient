package com.winson.fegin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class CustomLoadBalancerProperties {
    private Map<String, String> hosts;
}
