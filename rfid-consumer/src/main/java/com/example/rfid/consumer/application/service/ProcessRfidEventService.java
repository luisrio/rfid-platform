package com.example.rfid.consumer.application.service;

import com.example.rfid.consumer.application.port.in.ProcessRfidEventUseCase;
import com.example.rfid.consumer.application.port.out.AlertPublisherPort;
import com.example.rfid.consumer.application.port.out.DeduplicationPort;
import com.example.rfid.consumer.application.port.out.PaidEpcStatePort;
import com.example.rfid.consumer.application.port.out.RfidMonitoringPort;
import com.example.rfid.consumer.domain.model.RfidEvent;
import com.example.rfid.consumer.domain.policy.TopicClassifier;
import com.example.rfid.consumer.infrastructure.config.RfidProperties;
import com.example.rfid.consumer.infrastructure.mapper.PaymentConfirmationJsonMapper;
import com.example.rfid.consumer.infrastructure.mapper.RfidEventJsonMapper;
import org.springframework.stereotype.Service;

@Service
public class ProcessRfidEventService implements ProcessRfidEventUseCase {

    private final RfidEventJsonMapper mapper;
    private final PaymentConfirmationJsonMapper paymentMapper;
    private final DeduplicationPort deduplicationPort;
    private final PaidEpcStatePort paidEpcStatePort;
    private final AlertPublisherPort alertPublisherPort;
    private final RfidMonitoringPort monitoringPort;
    private final RfidProperties properties;
    private final TopicClassifier topicClassifier;

    public ProcessRfidEventService(
            RfidEventJsonMapper mapper,
            PaymentConfirmationJsonMapper paymentMapper,
            DeduplicationPort deduplicationPort,
            PaidEpcStatePort paidEpcStatePort,
            AlertPublisherPort alertPublisherPort,
            RfidMonitoringPort monitoringPort,
            RfidProperties properties,
            TopicClassifier topicClassifier
    ) {
        this.mapper = mapper;
        this.paymentMapper = paymentMapper;
        this.deduplicationPort = deduplicationPort;
        this.paidEpcStatePort = paidEpcStatePort;
        this.alertPublisherPort = alertPublisherPort;
        this.monitoringPort = monitoringPort;
        this.properties = properties;
        this.topicClassifier = topicClassifier;
    }

    @Override
    public void process(String topic, String jsonPayload) {
        monitoringPort.onMessageReceived(topic);

        if (!topicClassifier.isKnownTopic(topic)) {
            handleIgnored(topic);
            return;
        }

        if (topicClassifier.isPaymentTopic(topic)) {
            processPayment(topic, jsonPayload);
            return;
        }

        RfidEvent event = mapper.tryParse(jsonPayload).orElse(null);
        if (event == null) {
            monitoringPort.onInvalidPayload(topic);
            return;
        }

        if (event.rssi().value() < properties.getFilter().getMinRssi()) {
            monitoringPort.onWeakSignal(topic);
            return;
        }

        String dedupKey = event.readerId().value() + "|" + event.epc().value();
        if (deduplicationPort.isDuplicate(dedupKey)) {
            monitoringPort.onDuplicate(topic);
            return;
        }

        handleRfidByTopic(topic, event);
        deduplicationPort.markProcessed(dedupKey);
    }

    private void processPayment(String topic, String jsonPayload) {
        monitoringPort.onPaymentMessageReceived(topic);

        PaymentConfirmationJsonMapper.PaymentConfirmationEvent payment = paymentMapper.tryParse(jsonPayload).orElse(null);
        if (payment == null) {
            monitoringPort.onInvalidPayload(topic);
            return;
        }

        String dedupKey = "paymentTx|" + payment.transactionId();
        if (deduplicationPort.isDuplicate(dedupKey)) {
            monitoringPort.onPaymentDuplicateTransaction(topic, payment.transactionId());
            return;
        }

        if (payment.paidEpcs().isEmpty()) {
            return;
        }

        int marked = 0;
        for (String epc : payment.paidEpcs()) {
            paidEpcStatePort.markPaid(epc);
            marked++;
        }
        deduplicationPort.markProcessed(dedupKey);
        monitoringPort.onPaymentEpcsMarkedPaid(topic, marked);
    }

    private void handleRfidByTopic(String topic, RfidEvent event) {
        if (topicClassifier.isCashierTopic(topic)) {
            handleCashier(topic, event);
            return;
        }

        if (topicClassifier.isExitTopic(topic)) {
            handleExit(topic, event);
            return;
        }

        if (topicClassifier.isFittingTopic(topic)) {
            handleFitting(topic, event);
            return;
        }
    }

    private void handleCashier(String topic, RfidEvent event) {
        // Keep as no-op for now; add monitoring later if needed.
    }

    private void handleExit(String topic, RfidEvent event) {
        boolean paid = paidEpcStatePort.isPaid(event.epc().value());
        monitoringPort.onExitChecked(topic, paid);

        if (!paid) {
            alertPublisherPort.publishUnpaidExitAlert(
                    event.epc().value(),
                    event.readerId().value(),
                    event.rssi().value(),
                    event.timestamp(),
                    topic
            );
            monitoringPort.onAlertPublished(topic, event.epc().value(), event.readerId().value());
        }
    }

    private void handleFitting(String topic, RfidEvent event) {
        monitoringPort.onFittingRoomSeen(topic, event.epc().value());
    }

    private void handleIgnored(String topic) {
        monitoringPort.onIgnoredTopic(topic);
    }
}
