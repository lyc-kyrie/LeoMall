package com.leomall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 阶段2B：Spring 6.1 收紧了同名 Bean 兼容判定——组件扫描扫到 admin/wx 模块的
 * 独立启动类（简单类名同为 Application → bean 名 "application"）会直接冲突
 * （Boot 2.7 时代被静默跳过）。这里展开 @SpringBootApplication 为等价三注解，
 * 并在扫描中排除两个模块启动类（其 @MapperScan/事务/调度注解本类已具备）。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.leomall",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = {com.leomall.admin.Application.class, com.leomall.wx.Application.class})
        })
@MapperScan("com.leomall.db.dao")
@EnableTransactionManagement
@EnableScheduling
public class Application {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

}