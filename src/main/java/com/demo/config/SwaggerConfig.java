package com.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("주문 관리 시스템 API")
                        .description("사용자, 상품, 주문 및 배송 상태를 관리하는 REST API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Demo Team")));
    }
}
