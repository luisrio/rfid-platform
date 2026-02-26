package com.example.rfid.consumer.domain.policy;

import com.example.rfid.consumer.infrastructure.config.RfidProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicClassifierTest {

    private TopicClassifier classifier;

    @BeforeEach
    void setUp() {
        RfidProperties properties = new RfidProperties();
        properties.getTopics().setCashierPrefix("tienda/lecturas/caja/");
        properties.getTopics().setExitPrefix("tienda/lecturas/salida/");
        properties.getTopics().setFittingPrefix("tienda/lecturas/probador/");
        properties.getTopics().setPaymentPrefix("tienda/pagos/confirmados");
        classifier = new TopicClassifier(properties);
    }

    @Test
    void matchesTopicByPrefix_forAllSupportedTypes() {
        assertTrue(classifier.isCashierTopic("tienda/lecturas/caja/CAJA-01"));
        assertTrue(classifier.isExitTopic("tienda/lecturas/salida/GATE-01"));
        assertTrue(classifier.isFittingTopic("tienda/lecturas/probador/PROBADOR-01"));
        assertTrue(classifier.isPaymentTopic("tienda/pagos/confirmados"));
    }

    @Test
    void doesNotMatchWhenTopicIsBlankOrNull() {
        assertFalse(classifier.isCashierTopic(null));
        assertFalse(classifier.isCashierTopic(" "));
    }

    @Test
    void doesNotMatchDifferentPrefix() {
        assertFalse(classifier.isExitTopic("tienda/lecturas/caja/CAJA-01"));
    }

    @Test
    void doesNotMatchWhenConfiguredPrefixIsBlankOrNull() {
        RfidProperties nullPrefixProperties = new RfidProperties();
        nullPrefixProperties.getTopics().setCashierPrefix(null);
        TopicClassifier nullPrefixClassifier = new TopicClassifier(nullPrefixProperties);
        assertFalse(nullPrefixClassifier.isCashierTopic("tienda/lecturas/caja/CAJA-01"));

        RfidProperties blankPrefixProperties = new RfidProperties();
        blankPrefixProperties.getTopics().setCashierPrefix(" ");
        TopicClassifier blankPrefixClassifier = new TopicClassifier(blankPrefixProperties);
        assertFalse(blankPrefixClassifier.isCashierTopic("tienda/lecturas/caja/CAJA-01"));
    }
}
