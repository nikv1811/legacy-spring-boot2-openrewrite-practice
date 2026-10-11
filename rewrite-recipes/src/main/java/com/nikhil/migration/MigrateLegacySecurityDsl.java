/*
 * Copyright 2026 Nikhil Verma
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nikhil.migration;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Renames the legacy Spring Security 5 authorization DSL methods while their
 * original type attribution is still available. The Boot 3 recipe runs after
 * this recipe and performs the dependency/framework migration.
 */
public class MigrateLegacySecurityDsl extends Recipe {

    private static final String HTTP_SECURITY =
            "org.springframework.security.config.annotation.web.builders.HttpSecurity";
    private static final String REQUEST_MATCHER_REGISTRY =
            "org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry";

    @Override
    public String getDisplayName() {
        return "Migrate legacy Spring Security authorization DSL calls";
    }

    @Override
    public String getDescription() {
        return "Renames type-attributed Spring Security 5 authorizeRequests/antMatchers calls " +
               "to authorizeHttpRequests/requestMatchers before the framework upgrade. " +
               "Review the resulting authorization rules and compile the application.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation visited = super.visitMethodInvocation(method, ctx);
                if (visited.getMethodType() == null || visited.getMethodType().getDeclaringType() == null) {
                    return visited;
                }

                String methodName = visited.getName().getSimpleName();
                String declaringType = visited.getMethodType().getDeclaringType().getFullyQualifiedName();
                if ("authorizeRequests".equals(methodName) && HTTP_SECURITY.equals(declaringType)) {
                    return visited.withName(visited.getName().withSimpleName("authorizeHttpRequests"));
                }
                if ("antMatchers".equals(methodName) && REQUEST_MATCHER_REGISTRY.equals(declaringType)) {
                    return visited.withName(visited.getName().withSimpleName("requestMatchers"));
                }
                return visited;
            }
        };
    }
}
