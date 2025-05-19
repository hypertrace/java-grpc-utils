package org.hypertrace.ratelimiter.grpcutils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.function.BiFunction;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.junit.jupiter.api.Test;

class RateLimiterConfigurationTest {

  @Test
  void testDefaultValues() {
    // Build a minimal RateLimiterConfiguration using only required fields
    RateLimiterConfiguration configuration =
        RateLimiterConfiguration.builder()
            .method("testMethod")
            .rateLimit(
                RateLimiterConfiguration.RateLimit.builder()
                    .tokens(10)
                    .refreshPeriodSeconds(30)
                    .build())
            .build();

    // Assertions for default values
    assertFalse(configuration.isEnabled()); // `enabled` defaults to true
    assertEquals("testMethod", configuration.getMethod());
    assertNull(configuration.getMatchAttributes()); // `matchAttributes` defaults to null
    assertNotNull(configuration.getTokenCostFunction()); // Ensure tokenCostFunction is initialized
    assertEquals(
        1, configuration.getTokenCostFunction().apply(null, null)); // Default token cost value
  }

  @Test
  void testCustomMatchAttributes() {
    // Create matchAttributes map and build configuration
    Map<String, String> matchAttributes = Map.of("tenant_id", "traceable");
    RateLimiterConfiguration configuration =
        RateLimiterConfiguration.builder()
            .method("testMethod")
            .matchAttributes(matchAttributes)
            .rateLimit(
                RateLimiterConfiguration.RateLimit.builder()
                    .tokens(100)
                    .refreshPeriodSeconds(60)
                    .build())
            .build();

    // Verify matchAttributes are correctly set
    assertEquals(matchAttributes, configuration.getMatchAttributes());
  }

  @Test
  void testCustomTokenCostFunction() {
    // Define a custom tokenCostFunction
    BiFunction<RequestContext, Object, Integer> customTokenCostFunction =
        (ctx, req) -> (req instanceof Integer) ? (Integer) req : 5;

    // Build a configuration using the custom token cost function
    RateLimiterConfiguration configuration =
        RateLimiterConfiguration.builder()
            .method("computeTokenCost")
            .tokenCostFunction(customTokenCostFunction)
            .rateLimit(
                RateLimiterConfiguration.RateLimit.builder()
                    .tokens(50)
                    .refreshPeriodSeconds(15)
                    .build())
            .build();

    // Verify behavior of the custom token cost function
    assertEquals(10, configuration.getTokenCostFunction().apply(null, 10)); // Dynamic cost
    assertEquals(
        5, configuration.getTokenCostFunction().apply(null, "randomObject")); // Default cost
  }

  @Test
  void testAttributeExtractor() {
    // Define an attributeExtractor that extracts specific attributes from the request
    BiFunction<RequestContext, Object, Map<String, String>> customAttributeExtractor =
        (ctx, req) -> Map.of("attributeKey", "attributeValue");

    // Build the configuration with the custom attribute extractor
    RateLimiterConfiguration configuration =
        RateLimiterConfiguration.builder()
            .method("extractAttributes")
            .attributeExtractor(customAttributeExtractor)
            .rateLimit(
                RateLimiterConfiguration.RateLimit.builder()
                    .tokens(20)
                    .refreshPeriodSeconds(45)
                    .build())
            .build();

    // Verify the custom attribute extractor
    assertEquals(
        Map.of("attributeKey", "attributeValue"),
        configuration.getAttributeExtractor().apply(null, null));
  }

  @Test
  void testRateLimitConfiguration() {
    // Build a simple RateLimit configuration
    RateLimiterConfiguration.RateLimit rateLimit =
        RateLimiterConfiguration.RateLimit.builder().tokens(500).refreshPeriodSeconds(300).build();

    // Verify RateLimit configuration values
    assertEquals(500, rateLimit.getTokens());
    assertEquals(300, rateLimit.getRefreshPeriodSeconds());
  }

  @Test
  void testFullCustomConfiguration() {
    // Define custom token cost function and attribute extractor
    BiFunction<RequestContext, Object, Integer> tokenCostFunction = (ctx, req) -> 2;
    BiFunction<RequestContext, Object, Map<String, String>> attributeExtractor =
        (ctx, req) -> Map.of("tenant_id", "12345");

    // Build a complete custom configuration
    RateLimiterConfiguration configuration =
        RateLimiterConfiguration.builder()
            .method("fullCustomMethod")
            .enabled(false)
            .matchAttributes(Map.of("region", "us-west"))
            .attributeExtractor(attributeExtractor)
            .tokenCostFunction(tokenCostFunction)
            .rateLimit(
                RateLimiterConfiguration.RateLimit.builder()
                    .tokens(1000)
                    .refreshPeriodSeconds(60)
                    .build())
            .build();

    // Verify all custom configurations
    assertFalse(configuration.isEnabled()); // Verify `enabled` value
    assertEquals("fullCustomMethod", configuration.getMethod()); // Verify method
    assertEquals(
        Map.of("region", "us-west"), configuration.getMatchAttributes()); // Verify matchAttributes
    assertEquals(
        Map.of("tenant_id", "12345"),
        configuration.getAttributeExtractor().apply(null, null)); // Custom extractor
    assertEquals(2, configuration.getTokenCostFunction().apply(null, null)); // Custom token cost
    assertEquals(1000, configuration.getRateLimit().getTokens()); // RateLimit tokens
    assertEquals(
        60, configuration.getRateLimit().getRefreshPeriodSeconds()); // RateLimit refresh period
  }
}
