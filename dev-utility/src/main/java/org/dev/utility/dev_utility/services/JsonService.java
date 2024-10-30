package org.dev.utility.dev_utility.services;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

public class JsonService {

  private static Configuration jsonReadConfig = Configuration.builder()
      .jsonProvider(new JacksonJsonNodeJsonProvider()).mappingProvider(new JacksonMappingProvider())
      .build();
  private static Configuration jsonWriteConfig = Configuration.builder()
      .jsonProvider(new JacksonJsonNodeJsonProvider()).mappingProvider(new JacksonMappingProvider())
      .build();

  public static String executeJsonPath(String json, String expression) {

    try {

      Object result = JsonPath.using(jsonReadConfig).parse(json).read(expression);
      JsonNode resultNode = null;
      if (result == null)
        return null;
      else if (result instanceof JsonNode) {
        resultNode = (JsonNode) result;
        return resultNode.toPrettyString();
      }
      return String.valueOf(result);
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public static String formatJson(String jsonString)
      throws JsonMappingException, JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    Object json = mapper.readValue(jsonString, Object.class);
    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
    return writer.writeValueAsString(json);
  }

  public static String formatJsonString(Object result) {
    JsonNode resultNode;
    if (result == null)
      return null;
    else if (result instanceof JsonNode) {
      resultNode = (JsonNode) result;
      return resultNode.toPrettyString();
    }
    return String.valueOf(result);
  }

  /**
   * Function to check if a JsonNode is effectively empty Handles all of its
   * subclass (ObjectNode, ArrayNode, TextNode, etc.)
   */
  public static boolean isJsonNodeEffectivelyEmpty(JsonNode jsonNode) {
    return jsonNode == null || jsonNode.isNull()
        || (jsonNode.isValueNode() && jsonNode.asText().isEmpty())
        || (jsonNode.isContainerNode() && jsonNode.isEmpty());
  }

  /**
   * Private method: Parses json to find the jsonPath
   * 
   * @param json     Json input. Type must be either String or JsonNode
   * @param jsonPath Json path string to find in json. Ex: $.stores[1].name
   * @return Matched json as JsonNode. Else null
   */
  public static String parseJson(Object json, String jsonPath, String parentRegex) {

    JsonNode ret = null;

    // Error on unknown type
    if (!(json instanceof String || json instanceof JsonNode)) {
      throw new IllegalArgumentException("Input JSON must be either a String or JsonNode!");
    }

    try {
      // Has parent separator?
      if (parentRegex != null && !parentRegex.isBlank()) {
        // Figure out parentJsonPath
        String parentJsonPath = null;
        // String childJsonPath = null;
        ArrayNode childJsonPathsMatched = null;
        // String parentRegex = null;
        // String[] jsonPathSplitted = null;
        com.jayway.jsonpath.Configuration conf = jsonWriteConfig.addOptions(Option.AS_PATH_LIST);

        // Analyze jsonPath
        // jsonPathSplitted = jsonPath.split(Pattern.quote(parentSeparator), 2);
        // childJsonPath = jsonPathSplitted[0];
        // parentRegex = jsonPathSplitted[1];

        // Find childJsonPath
        if (json instanceof String) {
          childJsonPathsMatched = JsonPath.using(conf).parse((String) json).read(jsonPath);
        } else if (json instanceof JsonNode) {
          childJsonPathsMatched = JsonPath.using(conf).parse((JsonNode) json).read(jsonPath);
        }

        // Child unmatched?
        if (isJsonNodeEffectivelyEmpty(childJsonPathsMatched)) {
          return formatJsonString(ret);
        }

        // Regex find parentJsonPath from childJsonPathMatched
        Pattern pattern = Pattern.compile(parentRegex);
        Matcher matcher = pattern.matcher(childJsonPathsMatched.get(0).asText());
        if (!matcher.find()) {

          return "Parent regex unmatched (" + parentRegex
              + ")! Please verify the escaped regex pattern";
        }
        parentJsonPath = matcher.group();

        // parentRegex failed?
        if (parentJsonPath == null) {
          return formatJsonString(ret);
        }

        jsonPath = parentJsonPath;
      }

      // Find jsonPath in json
      if (json instanceof String) {
        ret = JsonPath.using(jsonWriteConfig).parse((String) json).read(jsonPath);
      } else if (json instanceof JsonNode) {
        ret = JsonPath.using(jsonWriteConfig).parse((JsonNode) json).read(jsonPath);
      }

      // Unmatched?
      if (isJsonNodeEffectivelyEmpty(ret)) { // Unmatched is null
        ret = null;
      }
    } catch (PathNotFoundException e) {
      return e.getMessage();
    }

    return formatJsonString(ret);
  }

}
