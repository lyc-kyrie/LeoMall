package com.leomall.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 阶段2A：springfox → springdoc；阶段2B：springdoc 2.x（jakarta / Boot 3）。
 * 在线文档：http://host:port/swagger-ui.html（分组 admin/wx）。
 *
 * P2-6 开关语义保留但更名：生产设 SWAGGER_ENABLED=false（即 springdoc.api-docs.enabled=false）
 * 时，自动配置与本分组 Bean 全部关闭，/v3/api-docs 与 swagger-ui 不再暴露。
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class AdminSwagger2Configuration {

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .packagesToScan("com.leomall.admin.web")
                .build();
    }

    @Bean
    @Primary // all-in-one 部署时与 wx 模块的 OpenAPI Bean 共存，以 admin 为准
    public OpenAPI adminOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("leomall-admin API")
                .description("leomall管理后台API")
                .version("1.0"));
    }
}