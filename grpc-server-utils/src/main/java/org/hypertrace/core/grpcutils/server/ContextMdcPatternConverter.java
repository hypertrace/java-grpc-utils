package org.hypertrace.core.grpcutils.server;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

@Plugin(name = "ctxParams", category = PatternConverter.CATEGORY)
@ConverterKeys({"ctxParams"})
public class ContextMdcPatternConverter extends LogEventPatternConverter {

  private static final String CTX_PREFIX = "x-ctx-";

  private ContextMdcPatternConverter() {
    super("ctxParams", "ctxParams");
  }

  public static ContextMdcPatternConverter newInstance(String[] options) {
    return new ContextMdcPatternConverter();
  }

  @Override
  public void format(LogEvent event, StringBuilder toAppendTo) {
    ReadOnlyStringMap contextData = event.getContextData();
    if (contextData == null || contextData.isEmpty()) return;

    StringBuilder sb = new StringBuilder();
    contextData.forEach(
        (key, value) -> {
          if (key.startsWith(CTX_PREFIX)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(key).append("=").append(value);
          }
        });
    if (sb.length() > 0) {
      toAppendTo.append("[").append(sb).append("]");
    }
  }
}
