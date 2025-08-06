package com.airbnb.viaduct.demoapp.helloworld;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
    classes = Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HelloWorldTest {

  @Autowired private TestRestTemplate restTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private void assertJsonEquals(String expected, String actual) throws Exception {
    JsonNode expectedNode = objectMapper.readTree(expected);
    JsonNode actualNode = objectMapper.readTree(actual);
    assertEquals(expectedNode, actualNode);
  }

  @Test
  public void queryHelloWorld() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);

    String requestBody = "{\"query\":\"query HelloWorld { helloWorld }\"}";
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response = restTemplate.postForEntity("/graphql", request, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    String expectedJson =
        "{\n" + "  \"data\": {\n" + "    \"helloWorld\": \"Hello World!\"\n" + "  }\n" + "}";

    assertJsonEquals(expectedJson, response.getBody());
  }

  @Test
  public void errorInQueryEmptyBody() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("accept", "application/json");
    headers.set("content-type", "application/json");

    String requestBody = "{\"query\":\"query HelloWorld { }\"}";
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response = restTemplate.postForEntity("/graphql", request, String.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    String expectedJson =
        "{\n"
            + "  \"errors\": [\n"
            + "    {\n"
            + "      \"message\": \"Invalid syntax with offending token '}' at line 1 column"
            + " 20\",\n"
            + "      \"locations\": [\n"
            + "        {\n"
            + "          \"line\": 1,\n"
            + "          \"column\": 20\n"
            + "        }\n"
            + "      ],\n"
            + "      \"extensions\": {\n"
            + "        \"classification\": \"InvalidSyntax\"\n"
            + "      }\n"
            + "    }\n"
            + "  ],\n"
            + "  \"data\": null\n"
            + "}";

    assertJsonEquals(expectedJson, response.getBody());
  }

  @Test
  public void errorInQueryWithNonExistingField() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("accept", "application/json");
    headers.set("content-type", "application/json");

    String requestBody = "{\"query\":\" \"}";
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response = restTemplate.postForEntity("/graphql", request, String.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    String expectedJson =
        "{\n"
            + "  \"errors\": [\n"
            + "    {\n"
            + "      \"message\": \"Invalid syntax with offending token '<EOF>' at line 1 column"
            + " 2\",\n"
            + "      \"locations\": [\n"
            + "        {\n"
            + "          \"line\": 1,\n"
            + "          \"column\": 2\n"
            + "        }\n"
            + "      ],\n"
            + "      \"extensions\": {\n"
            + "        \"classification\": \"InvalidSyntax\"\n"
            + "      }\n"
            + "    }\n"
            + "  ],\n"
            + "  \"data\": null\n"
            + "}";

    assertJsonEquals(expectedJson, response.getBody());
  }

  @Test
  public void errorMissingQuery() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("accept", "application/json");
    headers.set("content-type", "application/json");

    String requestBody = "{\"query\":\"query HelloWorld { thisIsNotAQuery }\"}";
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response = restTemplate.postForEntity("/graphql", request, String.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    String expectedJson =
        "{\n"
            + "  \"errors\": [\n"
            + "    {\n"
            + "      \"message\": \"Validation error (FieldUndefined@[thisIsNotAQuery]) : Field"
            + " 'thisIsNotAQuery' in type 'Query' is undefined\",\n"
            + "      \"locations\": [\n"
            + "        {\n"
            + "          \"line\": 1,\n"
            + "          \"column\": 20\n"
            + "        }\n"
            + "      ],\n"
            + "      \"extensions\": {\n"
            + "        \"classification\": \"ValidationError\"\n"
            + "      }\n"
            + "    }\n"
            + "  ],\n"
            + "  \"data\": null\n"
            + "}";

    assertJsonEquals(expectedJson, response.getBody());
  }

  @Test
  public void errorThrownFromTenantResolver() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("accept", "application/json");
    headers.set("content-type", "application/json");

    String requestBody = "{\"query\":\"query ThrowException { throwException }\"}";
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response = restTemplate.postForEntity("/graphql", request, String.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    String expectedJson =
        "{\n"
            + "  \"errors\": [\n"
            + "    {\n"
            + "      \"message\": \"Exception while fetching data (/throwException) :"
            + " java.lang.IllegalStateException: This is a resolver error\",\n"
            + "      \"locations\": [\n"
            + "        {\n"
            + "          \"line\": 1,\n"
            + "          \"column\": 24\n"
            + "        }\n"
            + "      ],\n"
            + "      \"path\": [\n"
            + "        \"throwException\"\n"
            + "      ],\n"
            + "      \"extensions\": {\n"
            + "        \"classification\": \"DataFetchingException\"\n"
            + "      }\n"
            + "    }\n"
            + "  ],\n"
            + "  \"data\": {\n"
            + "    \"throwException\": null\n"
            + "  }\n"
            + "}";

    assertJsonEquals(expectedJson, response.getBody());
  }
}
