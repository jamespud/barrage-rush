package com.spud.barrage.common.mq.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spud
 * @date 2025/3/23
 */
@Configuration
@ConfigurationProperties(prefix = "mq.consumer")
public class ConsumerProperties {

}
