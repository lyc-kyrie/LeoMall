package com.leomall.wx.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阶段2A：springfox → springdoc-openapi 1.x。
 * 在线文档：http://host:port/swagger-ui.html（分组 admin/wx）。
 *
 * P2-6 开关语义保留但更名：生产设 SWAGGER_ENABLED=false 整体关闭在线文档。
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class WxSwagger2Configuration {

    @Bean
    public GroupedOpenApi wxApi() {
        return GroupedOpenApi.builder()
                .group("wx")
                .packagesToScan("com.leomall.wx.web")
                .build();
    }

    @Bean
    public OpenAPI wxOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("leomall-wx API")
                .description("leomall小商场API")
                .version("1.0"));
    }
}