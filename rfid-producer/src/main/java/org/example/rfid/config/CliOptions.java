package org.example.rfid.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

final class CliOptions {

    private static final List<OptionSpec> OPTIONS = List.of(
            value(
                    "profile",
                    "<low|medium|high>",
                    () -> "Traffic defaults: timings + behavior ratios (default: medium)"
            ),
            value(
                    "broker",
                    "<url>",
                    () -> "MQTT broker URL (default: " + ArgsParser.defaultBroker() + ")"
            ),
            value(
                    "qos",
                    "<0|1|2>",
                    () -> "MQTT QoS (default: " + ArgsParser.defaultQos() + ")"
            ),
            value(
                    "cashier-topic",
                    "<topic>",
                    () -> "Topic for cashier scenario (default: " + ArgsParser.defaultCashierTopic() + ")"
            ),
            value(
                    "fitting-topic",
                    "<topic>",
                    () -> "Topic for fitting room scenario (default: " + ArgsParser.defaultFittingTopic() + ")"
            ),
            value(
                    "gate-topic",
                    "<topic>",
                    () -> "Topic for exit gate scenario (default: " + ArgsParser.defaultGateTopic() + ")"
            ),
            value(
                    "payment-topic",
                    "<topic>",
                    () -> "Topic for paid EPC confirmations (default: " + ArgsParser.defaultPaymentTopic() + ")"
            ),
            value(
                    "cashier-reader",
                    "<readerId>",
                    () -> "Reader ID for cashier scenario (default: " + ArgsParser.defaultCashierReader() + ")"
            ),
            value(
                    "fitting-reader",
                    "<readerId>",
                    () -> "Reader ID for fitting room scenario (default: " + ArgsParser.defaultFittingReader() + ")"
            ),
            value(
                    "gate-reader",
                    "<readerId>",
                    () -> "Reader ID for exit gate scenario (default: " + ArgsParser.defaultGateReader() + ")"
            ),
            value("cashier-basket-size", "<n>", () -> "Max distinct EPCs read per cashier tick (profile default)"),
            value("new-product-rate", "<0..1>", () -> "Product arrival intensity (profile default, can override)"),
            value("fitting-visit-rate", "<0..1>", () -> "Chance a new product goes to fitting room first"),
            value("post-cashier-fitting-rate", "<0..1>", () -> "Chance paid product revisits fitting room"),
            value("skip-cashier-rate", "<0..1>", () -> "Chance a product reaches exit without visiting cashier"),
            value("payment-transaction-rate", "<0..1>", () -> "Chance cashier interaction ends with a payment attempt"),
            value("payment-item-rate", "<0..1>", () -> "Chance each EPC in attempted transaction is actually paid"),
            value("unpaid-exit-rate", "<0..1>", () -> "For unpaid EPCs, chance of going to exit instead of returning to stock"),
            value("max-active-products", "<n>", () -> "Max in-memory active EPC journeys"),
            value("scenario-runner-thread-count", "<n>", () -> "Scheduler threads for scenarios (profile default)"),
            value("payment-max-transactions-per-tick", "<n>", () -> "Max payment notifications emitted per tick (profile default)"),
            value("cashier-period-ms", "<ms>", () -> "Tick period cashier (profile default, can override)"),
            value("fitting-period-ms", "<ms>", () -> "Tick period fitting room (profile default, can override)"),
            value("gate-period-ms", "<ms>", () -> "Tick period exit gate (profile default, can override)"),
            value("payment-period-ms", "<ms>", () -> "Tick period payment scenario (profile default, can override)"),
            flag("help", () -> "Show this help")
    );

    private static final Set<String> VALUE_KEYS = collectKeys(false);
    private static final Set<String> FLAG_KEYS = collectKeys(true);
    private static final Set<String> KNOWN_KEYS = buildKnownKeys();

    private CliOptions() {
    }

    static List<OptionSpec> all() {
        return OPTIONS;
    }

    static Set<String> valueKeys() {
        return VALUE_KEYS;
    }

    static Set<String> flagKeys() {
        return FLAG_KEYS;
    }

    static Set<String> knownKeys() {
        return KNOWN_KEYS;
    }

    private static Set<String> collectKeys(boolean flag) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (OptionSpec option : OPTIONS) {
            if (option.flag() == flag) {
                keys.add(option.key());
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    private static Set<String> buildKnownKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<>(VALUE_KEYS);
        keys.addAll(FLAG_KEYS);
        return Collections.unmodifiableSet(keys);
    }

    private static OptionSpec value(String key, String valuePlaceholder, Supplier<String> descriptionSupplier) {
        return new OptionSpec(key, false, valuePlaceholder, descriptionSupplier);
    }

    private static OptionSpec flag(String key, Supplier<String> descriptionSupplier) {
        return new OptionSpec(key, true, "", descriptionSupplier);
    }

    record OptionSpec(
            String key,
            boolean flag,
            String valuePlaceholder,
            Supplier<String> descriptionSupplier
    ) {
        String syntax() {
            if (flag) {
                return "--" + key;
            }
            return "--" + key + " " + valuePlaceholder;
        }

        String description() {
            return descriptionSupplier.get();
        }
    }
}
