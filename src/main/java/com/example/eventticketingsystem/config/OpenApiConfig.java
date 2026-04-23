package com.example.eventticketingsystem.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Event Ticketing System API",
                version = "v1",
                description = "REST API for event ticketing system"
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    private static final Pattern QUOTED_VALUE_PATTERN = Pattern.compile("'([^']+)'");

    @Bean
    public OperationCustomizer scopeOperationCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            Set<String> scopes = new LinkedHashSet<>();

            PreAuthorize classPreAuthorize = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(),
                    PreAuthorize.class
            );
            collectScopes(classPreAuthorize, scopes);

            PreAuthorize methodPreAuthorize = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getMethod(),
                    PreAuthorize.class
            );
            collectScopes(methodPreAuthorize, scopes);

            if (!scopes.isEmpty()) {
                String requiredScopesText = String.join(", ", scopes);
                String existingDescription = operation.getDescription();
                String scopeLine = "Required scopes: " + requiredScopesText;

                if (existingDescription == null || existingDescription.isBlank()) {
                    operation.setDescription(scopeLine);
                } else if (!existingDescription.contains(scopeLine)) {
                    operation.setDescription(existingDescription + "\n\n" + scopeLine);
                }

                operation.addExtension("x-required-scopes", scopes);
            }

            return operation;
        };
    }

    private static void collectScopes(PreAuthorize preAuthorize, Set<String> scopes) {
        if (preAuthorize == null) {
            return;
        }
        String expression = preAuthorize.value();
        if (expression == null || expression.isBlank()) {
            return;
        }
        if (!(expression.contains("hasAuthority") || expression.contains("hasAnyAuthority"))) {
            return;
        }

        Matcher matcher = QUOTED_VALUE_PATTERN.matcher(expression);
        while (matcher.find()) {
            scopes.add(matcher.group(1));
        }
    }
}

