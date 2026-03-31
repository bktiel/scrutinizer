package com.scrutinizer.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI scrutinizerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Scrutinizer API")
                        .description("Supply-chain posture evaluation API. Evaluate CycloneDX SBOMs against "
                                + "YAML policies, persist results, and query findings, trends, and review status.")
                        .version("0.1.0")
                        .contact(new Contact().name("Scrutinizer").url("https://github.com/scrutinizer"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
