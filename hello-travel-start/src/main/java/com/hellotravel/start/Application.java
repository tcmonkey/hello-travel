package com.hellotravel.start;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * hello-travel旅行助手启动与模块装配入口。
 *
 * @author AIGenerator
 */
@SpringBootApplication(scanBasePackages = "com.hellotravel")
@MapperScan("com.hellotravel.infrastructure")
public class Application {

    /**
     * 启动本地一期旅行服务。
     *
     * @author AIGenerator
     * @param args 受控args参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * 处理mybatisPlusInterceptor对应的受控业务操作。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
