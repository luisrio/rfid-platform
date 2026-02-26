package com.example.rfid.consumer.application.service;

import com.example.rfid.consumer.application.port.out.AlertPublisherPort;
import com.example.rfid.consumer.application.port.out.DeduplicationPort;
import com.example.rfid.consumer.application.port.out.PaidEpcStatePort;
import com.example.rfid.consumer.application.port.out.RfidMonitoringPort;
import com.example.rfid.consumer.domain.policy.TopicClassifier;
import com.example.rfid.consumer.infrastructure.config.RfidProperties;
import com.example.rfid.consumer.infrastructure.mapper.PaymentConfirmationJsonMapper;
import com.example.rfid.consumer.infrastructure.mapper.RfidEventJsonMapper;
import com.example.rfid.consumer.support.TestPayloadFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class ProcessRfidEventServiceTest {

    private static final String FITTING_TOPIC = "tienda/lecturas/probador/PROBADOR-01";
    private static final String CASHIER_TOPIC = "tienda/lecturas/caja/CAJA-01";
    private static final String EXIT_TOPIC = "tienda/lecturas/salida/GATE-01";
    private static final String PAYMENT_TOPIC = "tienda/pagos/confirmados";
    private static final String ALERT_TOPIC = "tienda/alertas/seguridad";

    private DeduplicationPort deduplicationPort;
    private PaidEpcStatePort paidEpcStatePort;
    private AlertPublisherPort alertPublisherPort;
    private RfidMonitoringPort monitoringPort;
    private ProcessRfidEventService service;

    @BeforeEach
    void setUp() {
        deduplicationPort = Mockito.mock(DeduplicationPort.class);
        paidEpcStatePort = Mockito.mock(PaidEpcStatePort.class);
        alertPublisherPort = Mockito.mock(AlertPublisherPort.class);
        monitoringPort = Mockito.mock(RfidMonitoringPort.class);

        RfidProperties properties = new RfidProperties();
        properties.getFilter().setMinRssi(-70);
        properties.getTopics().setCashierPrefix("tienda/lecturas/caja/");
        properties.getTopics().setExitPrefix("tienda/lecturas/salida/");
        properties.getTopics().setFittingPrefix("tienda/lecturas/probador/");
        properties.getTopics().setPaymentPrefix("tienda/pagos/confirmados");

        RfidEventJsonMapper mapper = new RfidEventJsonMapper(new ObjectMapper());
        PaymentConfirmationJsonMapper paymentMapper = new PaymentConfirmationJsonMapper(new ObjectMapper());
        TopicClassifier topicClassifier = new TopicClassifier(properties);

        service = new ProcessRfidEventService(
                mapper,
                paymentMapper,
                deduplicationPort,
                paidEpcStatePort,
                alertPublisherPort,
                monitoringPort,
                properties,
                topicClassifier
        );
    }

    @Test
    void fittingTopic_tracksSeenProduct_andMarksDedup() {
        Mockito.when(deduplicationPort.isDuplicate("PROBADOR-01|EPC-13B4368C666B")).thenReturn(false);
        String payload = TestPayloadFactory.rfidEvent("EPC-13B4368C666B", "PROBADOR-01", -68, 1771698938939L);

        service.process(FITTING_TOPIC, payload);

        Mockito.verify(monitoringPort).onMessageReceived(FITTING_TOPIC);
        Mockito.verify(monitoringPort).onFittingRoomSeen(FITTING_TOPIC, "EPC-13B4368C666B");
        Mockito.verify(deduplicationPort).markProcessed("PROBADOR-01|EPC-13B4368C666B");
        Mockito.verifyNoInteractions(paidEpcStatePort, alertPublisherPort);
    }

    @Test
    void cashierTopic_isNoopBusinessFlow_butMarksDedup() {
        Mockito.when(deduplicationPort.isDuplicate("CAJA-01|EPC-E93556BE318C")).thenReturn(false);
        String payload = TestPayloadFactory.rfidEvent("EPC-E93556BE318C", "CAJA-01", -56, 1771698938379L);

        service.process(CASHIER_TOPIC, payload);

        Mockito.verify(monitoringPort).onMessageReceived(CASHIER_TOPIC);
        Mockito.verify(deduplicationPort).markProcessed("CAJA-01|EPC-E93556BE318C");
        Mockito.verifyNoInteractions(paidEpcStatePort, alertPublisherPort);
    }

    @Test
    void exitTopic_whenUnpaid_publishesAlert() {
        Mockito.when(deduplicationPort.isDuplicate("GATE-01|EPC-4843A4B3CF6E")).thenReturn(false);
        Mockito.when(paidEpcStatePort.isPaid("EPC-4843A4B3CF6E")).thenReturn(false);
        String payload = TestPayloadFactory.rfidEvent("EPC-4843A4B3CF6E", "GATE-01", -40, 1771698938831L);

        service.process(EXIT_TOPIC, payload);

        Mockito.verify(monitoringPort).onExitChecked(EXIT_TOPIC, false);
        Mockito.verify(alertPublisherPort).publishUnpaidExitAlert(
                "EPC-4843A4B3CF6E",
                "GATE-01",
                -40,
                1771698938831L,
                EXIT_TOPIC
        );
        Mockito.verify(monitoringPort).onAlertPublished(EXIT_TOPIC, "EPC-4843A4B3CF6E", "GATE-01");
        Mockito.verify(deduplicationPort).markProcessed("GATE-01|EPC-4843A4B3CF6E");
    }

    @Test
    void exitTopic_whenPaid_skipsAlert() {
        Mockito.when(deduplicationPort.isDuplicate("GATE-01|EPC-4843A4B3CF6E")).thenReturn(false);
        Mockito.when(paidEpcStatePort.isPaid("EPC-4843A4B3CF6E")).thenReturn(true);
        String payload = TestPayloadFactory.rfidEvent("EPC-4843A4B3CF6E", "GATE-01", -40, 1771698938831L);

        service.process(EXIT_TOPIC, payload);

        Mockito.verify(monitoringPort).onExitChecked(EXIT_TOPIC, true);
        Mockito.verifyNoInteractions(alertPublisherPort);
        Mockito.verify(deduplicationPort).markProcessed("GATE-01|EPC-4843A4B3CF6E");
    }

    @Test
    void paymentTopic_marksPaidEpcs_andMarksTransactionDedup() {
        Mockito.when(deduplicationPort.isDuplicate("paymentTx|TX-5BC1984EC46BAC01")).thenReturn(false);
        String payload = TestPayloadFactory.paymentConfirmation(
                "TX-5BC1984EC46BAC01",
                "CAJA-01",
                1771698938441L,
                List.of("EPC-4843A4B3CF6E")
        );

        service.process(PAYMENT_TOPIC, payload);

        Mockito.verify(monitoringPort).onPaymentMessageReceived(PAYMENT_TOPIC);
        Mockito.verify(paidEpcStatePort).markPaid("EPC-4843A4B3CF6E");
        Mockito.verify(deduplicationPort).markProcessed("paymentTx|TX-5BC1984EC46BAC01");
        Mockito.verify(monitoringPort).onPaymentEpcsMarkedPaid(PAYMENT_TOPIC, 1);
        Mockito.verifyNoInteractions(alertPublisherPort);
    }

    @Test
    void paymentTopic_skipsDuplicateTransaction() {
        Mockito.when(deduplicationPort.isDuplicate("paymentTx|TX-5BC1984EC46BAC01")).thenReturn(true);
        String payload = TestPayloadFactory.paymentConfirmation(
                "TX-5BC1984EC46BAC01",
                "CAJA-01",
                1771698938441L,
                List.of("EPC-4843A4B3CF6E")
        );

        service.process(PAYMENT_TOPIC, payload);

        Mockito.verify(monitoringPort).onPaymentMessageReceived(PAYMENT_TOPIC);
        Mockito.verify(monitoringPort).onPaymentDuplicateTransaction(PAYMENT_TOPIC, "TX-5BC1984EC46BAC01");
        Mockito.verifyNoInteractions(paidEpcStatePort, alertPublisherPort);
        Mockito.verify(deduplicationPort, Mockito.never()).markProcessed(Mockito.anyString());
    }

    @Test
    void paymentTopic_invalidPayload_tracksInvalid() {
        service.process(PAYMENT_TOPIC, "not-json");

        Mockito.verify(monitoringPort).onPaymentMessageReceived(PAYMENT_TOPIC);
        Mockito.verify(monitoringPort).onInvalidPayload(PAYMENT_TOPIC);
        Mockito.verifyNoInteractions(paidEpcStatePort, alertPublisherPort);
    }

    @Test
    void rfidPayload_weakSignal_skipsDedupAndPublishing() {
        String weakPayload = "{\"epc\":\"EPC-13B4368C666B\",\"readerId\":\"PROBADOR-01\",\"rssi\":-80,\"timestamp\":1771698938939}";

        service.process(FITTING_TOPIC, weakPayload);

        Mockito.verify(monitoringPort).onWeakSignal(FITTING_TOPIC);
        Mockito.verifyNoInteractions(deduplicationPort, paidEpcStatePort, alertPublisherPort);
    }

    @Test
    void rfidPayload_duplicate_skipsHandling() {
        Mockito.when(deduplicationPort.isDuplicate("PROBADOR-01|EPC-13B4368C666B")).thenReturn(true);
        String payload = TestPayloadFactory.rfidEvent("EPC-13B4368C666B", "PROBADOR-01", -68, 1771698938939L);

        service.process(FITTING_TOPIC, payload);

        Mockito.verify(monitoringPort).onDuplicate(FITTING_TOPIC);
        Mockito.verify(deduplicationPort, Mockito.never()).markProcessed(Mockito.anyString());
        Mockito.verifyNoInteractions(paidEpcStatePort, alertPublisherPort);
    }

    @Test
    void unknownTopic_isIgnoredBeforeParsingAndDedup() {
        String unknownTopic = "tienda/lecturas/bodega/BODEGA-01";
        String payload = TestPayloadFactory.rfidEvent("EPC-13B4368C666B", "PROBADOR-01", -68, 1771698938939L);

        service.process(unknownTopic, payload);

        Mockito.verify(monitoringPort).onIgnoredTopic(unknownTopic);
        Mockito.verifyNoInteractions(deduplicationPort, paidEpcStatePort, alertPublisherPort);
    }

    @Test
    void alertPayloadTopic_isIgnoredAsUnknownTopic() {
        String payload = TestPayloadFactory.securityAlert(
                "EPC-10B3C002D5FC",
                "GATE-01",
                -42,
                1771698931640L,
                EXIT_TOPIC,
                "2026-02-21T18:35:31.642011400Z"
        );

        service.process(ALERT_TOPIC, payload);

        Mockito.verify(monitoringPort).onIgnoredTopic(ALERT_TOPIC);
        Mockito.verifyNoInteractions(deduplicationPort, paidEpcStatePort, alertPublisherPort);
    }
}
