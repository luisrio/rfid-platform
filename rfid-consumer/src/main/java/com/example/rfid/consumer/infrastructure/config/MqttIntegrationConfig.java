package com.example.rfid.consumer.infrastructure.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.List;

@Configuration
public class MqttIntegrationConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttIntegrationConfig.class);

    @Bean
    public MessageChannel mqttIn() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOut() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory(RfidProperties properties) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{properties.getMqtt().getUrl()});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageProducer inboundAdapter(
            MqttPahoClientFactory factory,
            RfidProperties properties,
            MessageChannel mqttIn
    ) {
        String[] topicFilters = resolveTopicFilters(properties.getMqtt());
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        properties.getMqtt().getClientId(),
                        factory,
                        topicFilters);
        adapter.setQos(properties.getMqtt().getQos());
        adapter.setOutputChannel(mqttIn);
        log.info(
                "Configuring MQTT inbound clientId={} broker={} qos={} topics={}",
                properties.getMqtt().getClientId(),
                properties.getMqtt().getUrl(),
                properties.getMqtt().getQos(),
                String.join(",", topicFilters)
        );
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOut")
    public MessageHandler outboundHandler(MqttPahoClientFactory factory, RfidProperties properties) {
        String publisherClientId = properties.getMqtt().getClientId() + "-pub";
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(publisherClientId, factory);
        handler.setAsync(true);
        handler.setDefaultQos(properties.getMqtt().getQos());
        return handler;
    }

    private String[] resolveTopicFilters(RfidProperties.Mqtt mqtt) {
        List<String> explicitTopics = mqtt.getTopics();
        if (explicitTopics != null && !explicitTopics.isEmpty()) {
            return explicitTopics.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .toArray(String[]::new);
        }
        return new String[]{mqtt.getTopic()};
    }
}
