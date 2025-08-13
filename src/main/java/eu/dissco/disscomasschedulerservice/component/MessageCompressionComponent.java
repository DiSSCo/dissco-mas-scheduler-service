package eu.dissco.disscomasschedulerservice.component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessageCompressionComponent implements MessageConverter {

  final MessageConverter simpleConverter = new SimpleMessageConverter();

  @Override
  public Message toMessage(final Object messageString, final MessageProperties messageProperties)
      throws MessageConversionException {

    if (!(messageString instanceof String)) {
      throw new MessageConversionException("Invalid message type: " + messageString.getClass());
    }
    final byte[] message = ((String) messageString).getBytes(StandardCharsets.UTF_8);

    return new Message(message, messageProperties);
  }

  @Override
  public Object fromMessage(final Message message) throws MessageConversionException {
    var useGzip = "gzip".equals(message.getMessageProperties().getContentEncoding());
    if (useGzip) {
      try {
        try (var reader = new BufferedReader(new InputStreamReader(
            new GZIPInputStream(new ByteArrayInputStream(message.getBody()))))) {
          return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
      } catch (IOException e) {
        throw new MessageConversionException(
            "Failed to decompress message " + new String(message.getBody()), e);
      }
    } else {
      return simpleConverter.fromMessage(message);
    }
  }

}
