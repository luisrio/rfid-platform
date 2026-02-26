package com.example.rfid.consumer.infrastructure.monitoring;

import com.example.rfid.consumer.application.port.out.RfidMonitoringPort;
import com.example.rfid.consumer.infrastructure.config.RfidProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Component
public class ThrottledRfidMonitoringAdapter implements RfidMonitoringPort {

    private static final Logger log = LoggerFactory.getLogger(ThrottledRfidMonitoringAdapter.class);

    private final RfidProperties properties;
    private final AtomicLong received = new AtomicLong();
    private final LongAdder invalidPayload = new LongAdder();
    private final LongAdder weakSignal = new LongAdder();
    private final LongAdder duplicates = new LongAdder();
    private final LongAdder paymentMessages = new LongAdder();
    private final LongAdder paymentDuplicateTransactions = new LongAdder();
    private final LongAdder paymentEpcsMarkedPaid = new LongAdder();
    private final LongAdder exitChecks = new LongAdder();
    private final LongAdder exitPaid = new LongAdder();
    private final LongAdder exitUnpaid = new LongAdder();
    private final LongAdder alerts = new LongAdder();
    private final LongAdder fittingReads = new LongAdder();
    private final LongAdder fittingUniqueProducts = new LongAdder();
    private final LongAdder ignoredTopics = new LongAdder();
    private final Map<String, LongAdder> ignoredByTopic = new ConcurrentHashMap<>();
    private final Cache<String, Boolean> fittingSeenCache;

    public ThrottledRfidMonitoringAdapter(RfidProperties properties) {
        this.properties = properties;
        this.fittingSeenCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(Math.max(1, properties.getMonitoring().getFittingSeenTtlMinutes())))
                .maximumSize(1_000_000)
                .build();
    }

    @Override
    public void onMessageReceived(String topic) {
        received.incrementAndGet();
    }

    @Override
    public void onInvalidPayload(String topic) {
        invalidPayload.increment();
        logSummaryIfNeeded(topic);
    }

    @Override
    public void onWeakSignal(String topic) {
        weakSignal.increment();
        logSummaryIfNeeded(topic);
    }

    @Override
    public void onDuplicate(String topic) {
        duplicates.increment();
        logSummaryIfNeeded(topic);
    }

    @Override
    public void onPaymentMessageReceived(String topic) {
        paymentMessages.increment();
        logSummaryIfNeeded(topic);
    }

    @Override
    public void onPaymentDuplicateTransaction(String topic, String transactionId) {
        paymentDuplicateTransactions.increment();
        logSummaryIfNeeded(topic);
    }

    @Override
    public void onPaymentEpcsMarkedPaid(String topic, int count) {
        if (count > 0) {
            paymentEpcsMarkedPaid.add(count);
        }
        logSummaryIfNeeded(topic);
    }

    @Override
    public void onExitChecked(String topic, boolean paid) {
        exitChecks.increment();
        if (paid) {
            exitPaid.increment();
        } else {
            exitUnpaid.increment();
        }
        logSummaryIfNeeded(topic);
    }

    @Override
    public void onAlertPublished(String topic, String epc, String readerId) {
        alerts.increment();
        log.info("Unpaid EPC detected at exit. epc={} readerId={} topic={}", epc, readerId, topic);
        logSummaryIfNeeded(topic);
    }

    @Override
    public void onFittingRoomSeen(String topic, String epc) {
        fittingReads.increment();
        if (epc != null && !epc.isBlank()) {
            boolean firstSeen = fittingSeenCache.asMap().putIfAbsent(epc.trim(), Boolean.TRUE) == null;
            if (firstSeen) {
                fittingUniqueProducts.increment();
            }
        }
        logSummaryIfNeeded(topic);
    }

    @Override
    public void onIgnoredTopic(String topic) {
        ignoredTopics.increment();
        String key = topic == null || topic.isBlank() ? "(null)" : topic;
        ignoredByTopic.computeIfAbsent(key, ignored -> new LongAdder()).increment();
        logSummaryIfNeeded(topic);
    }

    private void logSummaryIfNeeded(String topic) {
        long total = received.get();
        long logEvery = properties.getMonitoring().getLogEveryMessages();
        if (logEvery <= 0 || (total % logEvery) != 0) {
            return;
        }
        log.info(
                "RFID summary total={} topic={} invalidPayload={} weakSignal={} duplicates={} paymentMessages={} paymentDuplicateTransactions={} paymentEpcsMarkedPaid={} fittingReads={} fittingUniqueProducts={} exitChecks={} exitPaid={} exitUnpaid={} alerts={} ignoredTopics={} ignoredTopicNames={}",
                total,
                topic,
                invalidPayload.sum(),
                weakSignal.sum(),
                duplicates.sum(),
                paymentMessages.sum(),
                paymentDuplicateTransactions.sum(),
                paymentEpcsMarkedPaid.sum(),
                fittingReads.sum(),
                fittingUniqueProducts.sum(),
                exitChecks.sum(),
                exitPaid.sum(),
                exitUnpaid.sum(),
                alerts.sum(),
                ignoredTopics.sum(),
                formatIgnoredTopicNames()
        );
    }

    private String formatIgnoredTopicNames() {
        if (ignoredByTopic.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, LongAdder> entry : ignoredByTopic.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(":").append(entry.getValue().sum());
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}
