package com.airbnb.viaduct.demoapp.helloworld.rest;

import graphql.ExecutionResult;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import viaduct.service.api.ExecutionInput;
import viaduct.service.api.Viaduct;
import viaduct.service.runtime.SchemaRegistryBuilder;
import viaduct.service.runtime.StandardViaduct;
import viaduct.tenant.runtime.bootstrap.ViaductTenantAPIBootstrapper;

@RestController
public class ViaductGraphQLController {

  private static final String SCHEMA_ID = "publicSchema";
  private static final String SCOPE_ID = "publicScope";

  private final Viaduct viaduct =
      new StandardViaduct.Builder()
          .withTenantAPIBootstrapperBuilder(
              new ViaductTenantAPIBootstrapper.Builder()
                  .tenantPackagePrefix("viaduct.demoapp.tenant1"))
          .withSchemaRegistryBuilder(
              new SchemaRegistryBuilder()
                  .withFullSchemaFromResources("viaduct.demoapp", ".*demoapp.*graphqls")
                  .registerScopedSchema(SCHEMA_ID, Set.of(SCOPE_ID)))
          .build();

  @PostMapping("/graphql")
  public ResponseEntity<Map<String, Object>> graphql(@RequestBody Map<String, Object> request) {
    ExecutionInput executionInput =
        new ExecutionInput(
            (String) request.get("query"),
            SCHEMA_ID,
            new Object(),
            (Map<String, Object>) request.getOrDefault("variables", Collections.emptyMap()),
            null);

    ExecutionResult result = viaduct.executeAsync(executionInput).join();

    // This handles the introspection query returning the GraphQL Schema
    if ("IntrospectionQuery".equals(request.get("operationName"))) {
      Map<String, Object> data = result.getData();
      return ResponseEntity.ok(Collections.singletonMap("data", data));
    }

    return ResponseEntity.status(statusCode(result)).body(result.toSpecification());
  }

  HttpStatus statusCode(ExecutionResult result) {
    if (result.isDataPresent() && !result.getErrors().isEmpty()) {
      return HttpStatus.BAD_REQUEST;
    } else {
      return HttpStatus.OK;
    }
  }
}
