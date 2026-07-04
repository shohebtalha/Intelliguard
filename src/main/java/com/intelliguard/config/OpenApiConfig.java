package com.intelliguard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Swagger UI with JWT authentication support.
 * Access at: http://localhost:8080/swagger-ui.html
 *
 * From here an interviewer can:
 * 1. See every endpoint with full request/response schemas
 * 2. Click "Authorize" and paste a JWT token
 * 3. Test any endpoint directly in the browser
 *
 * This is what production APIs at Razorpay, Stripe, etc. look like.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI intelliguardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IntelliGuard — Fraud Detection API")
                        .description("""
                    AI-powered real-time fraud detection engine.
                    
                    **Features:**
                    - XGBoost ML model with 99.3% ROC-AUC (ONNX Runtime)
                    - 6 fraud detection rules (Strategy Pattern)
                    - Redis velocity checks (sliding windows)
                    - Kafka event streaming
                    - SHAP explainability for every decision
                    - JWT authentication with role-based access
                    - Immutable audit log with model versioning
                    
                    **Decision time:** P99 < 100ms
                    """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("IntelliGuard Team")
                                .email("intelliguard@example.com")))
                // Add JWT auth button to Swagger UI
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token")));
    }
}