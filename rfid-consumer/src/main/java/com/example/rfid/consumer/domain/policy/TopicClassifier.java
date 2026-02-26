package com.example.rfid.consumer.domain.policy;

import com.example.rfid.consumer.infrastructure.config.RfidProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public final class TopicClassifier {

    private final String cashierPrefix;
    private final String exitPrefix;
    private final String fittingPrefix;
    private final String paymentPrefix;
    private final List<String> knownRfidPrefixes;

    public TopicClassifier(RfidProperties properties) {
        Objects.requireNonNull(properties, "properties must not be null");
        this.cashierPrefix = normalizePrefix(properties.getTopics().getCashierPrefix());
        this.exitPrefix = normalizePrefix(properties.getTopics().getExitPrefix());
        this.fittingPrefix = normalizePrefix(properties.getTopics().getFittingPrefix());
        this.paymentPrefix = normalizePrefix(properties.getTopics().getPaymentPrefix());
        knownRfidPrefixes = Stream.of(cashierPrefix, exitPrefix, fittingPrefix, paymentPrefix)
                .filter(Objects::nonNull)
                .toList();
    }

    public boolean isCashierTopic(String topic) {
        return startsWith(topic, cashierPrefix);
    }

    public boolean isExitTopic(String topic) {
        return startsWith(topic, exitPrefix);
    }

    public boolean isFittingTopic(String topic) {
        return startsWith(topic, fittingPrefix);
    }

    public boolean isPaymentTopic(String topic) {
        return startsWith(topic, paymentPrefix);
    }

    public boolean isKnownTopic(String topic) {
        return knownRfidPrefixes.stream().anyMatch(prefix -> startsWith(topic, prefix));
    }

    private static boolean startsWith(String topic, String prefix) {
        return topic != null && !topic.isBlank() && prefix != null && !prefix.isBlank() && topic.startsWith(prefix);
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null) {
            return null;
        }
        String trimmed = prefix.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
