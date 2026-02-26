package org.example.rfid.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ArgsParser {

    private static final String DEFAULT_BROKER = "tcp://localhost:1883";
    private static final String DEFAULT_CASHIER_TOPIC = "tienda/lecturas/caja/CAJA-01";
    private static final String DEFAULT_FITTING_TOPIC = "tienda/lecturas/probador/PROBADOR-01";
    private static final String DEFAULT_GATE_TOPIC = "tienda/lecturas/salida/GATE-01";
    private static final String DEFAULT_PAYMENT_TOPIC = "tienda/pagos/confirmados";
    private static final String DEFAULT_CASHIER_READER = "CAJA-01";
    private static final String DEFAULT_FITTING_READER = "PROBADOR-01";
    private static final String DEFAULT_GATE_READER = "GATE-01";
    private static final int DEFAULT_QOS = 1;
    private static final int MIN_QOS = 0;
    private static final int MAX_QOS = 2;
    private static final int MIN_SCENARIO_RUNNER_THREAD_COUNT = 1;
    private static final int MAX_SCENARIO_RUNNER_THREAD_COUNT = 32;
    private static final int MIN_PAYMENT_MAX_TRANSACTIONS_PER_TICK = 1;
    private static final int MAX_PAYMENT_MAX_TRANSACTIONS_PER_TICK = 1000;
    private static final Set<String> VALUE_OPTIONS = CliOptions.valueKeys();
    private static final Set<String> FLAG_OPTIONS = CliOptions.flagKeys();
    private static final Set<String> KNOWN_OPTIONS = CliOptions.knownKeys();

    private ArgsParser() {
    }

    public static boolean containsHelp(String[] args) {
        for (String arg : args) {
            if ("--help".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    public static ProducerConfig parse(String[] args) {
        Map<String, String> values = parseArgs(args);

        Profile profile = Profile.fromArg(values.get("profile"));
        String brokerUrl = values.getOrDefault("broker", DEFAULT_BROKER);
        int qos = parseInt(values, "qos", DEFAULT_QOS, MIN_QOS, MAX_QOS);

        String cashierTopic = values.getOrDefault("cashier-topic", DEFAULT_CASHIER_TOPIC);
        String fittingTopic = values.getOrDefault("fitting-topic", DEFAULT_FITTING_TOPIC);
        String gateTopic = values.getOrDefault("gate-topic", DEFAULT_GATE_TOPIC);
        String paymentTopic = values.getOrDefault("payment-topic", DEFAULT_PAYMENT_TOPIC);

        String cashierReader = values.getOrDefault("cashier-reader", DEFAULT_CASHIER_READER);
        String fittingReader = values.getOrDefault("fitting-reader", DEFAULT_FITTING_READER);
        String gateReader = values.getOrDefault("gate-reader", DEFAULT_GATE_READER);

        int cashierBasketSize = parseInt(values, "cashier-basket-size", profile.cashierBasketSize(), 1, 100);
        double newProductRate = parseDouble(values, "new-product-rate", profile.newProductRate(), 0.0, 1.0);
        double fittingVisitRate = parseDouble(values, "fitting-visit-rate", profile.fittingVisitRate(), 0.0, 1.0);
        double postCashierFittingRate = parseDouble(values, "post-cashier-fitting-rate", profile.postCashierFittingRate(), 0.0, 1.0);
        double skipCashierRate = parseDouble(values, "skip-cashier-rate", profile.skipCashierRate(), 0.0, 1.0);
        double paymentTransactionRate = parseDouble(values, "payment-transaction-rate", profile.paymentTransactionRate(), 0.0, 1.0);
        double paymentItemRate = parseDouble(values, "payment-item-rate", profile.paymentItemRate(), 0.0, 1.0);
        double unpaidExitRate = parseDouble(values, "unpaid-exit-rate", profile.unpaidExitRate(), 0.0, 1.0);
        int maxActiveProducts = parseInt(values, "max-active-products", profile.maxActiveProducts(), 1, 10_000);
        int scenarioRunnerThreadCount = parseInt(
                values,
                "scenario-runner-thread-count",
                profile.scenarioRunnerThreadCount(),
                MIN_SCENARIO_RUNNER_THREAD_COUNT,
                MAX_SCENARIO_RUNNER_THREAD_COUNT
        );
        int paymentMaxTransactionsPerTick = parseInt(
                values,
                "payment-max-transactions-per-tick",
                profile.paymentMaxTransactionsPerTick(),
                MIN_PAYMENT_MAX_TRANSACTIONS_PER_TICK,
                MAX_PAYMENT_MAX_TRANSACTIONS_PER_TICK
        );

        Duration cashierPeriod = Duration.ofMillis(parseInt(values, "cashier-period-ms", profile.cashierPeriodMs(), 1, 60_000));
        Duration fittingPeriod = Duration.ofMillis(parseInt(values, "fitting-period-ms", profile.fittingPeriodMs(), 1, 60_000));
        Duration gatePeriod = Duration.ofMillis(parseInt(values, "gate-period-ms", profile.gatePeriodMs(), 1, 60_000));
        Duration paymentPeriod = Duration.ofMillis(parseInt(values, "payment-period-ms", profile.paymentPeriodMs(), 1, 60_000));

        return new ProducerConfig(
                profile,
                brokerUrl,
                qos,
                cashierTopic,
                fittingTopic,
                gateTopic,
                paymentTopic,
                cashierReader,
                fittingReader,
                gateReader,
                cashierBasketSize,
                newProductRate,
                fittingVisitRate,
                postCashierFittingRate,
                skipCashierRate,
                paymentTransactionRate,
                paymentItemRate,
                unpaidExitRate,
                maxActiveProducts,
                scenarioRunnerThreadCount,
                paymentMaxTransactionsPerTick,
                cashierPeriod,
                fittingPeriod,
                gatePeriod,
                paymentPeriod
        );
    }

    private static int parseInt(Map<String, String> args, String key, int defaultValue, int min, int max) {
        String raw = args.get(key);
        if (raw == null) {
            return defaultValue;
        }

        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for --" + key + ": " + raw, e);
        }

        if (value < min || value > max) {
            throw new IllegalArgumentException("--" + key + " must be between " + min + " and " + max + ", got " + value);
        }

        return value;
    }

    private static double parseDouble(Map<String, String> args, String key, double defaultValue, double min, double max) {
        String raw = args.get(key);
        if (raw == null) {
            return defaultValue;
        }

        double value;
        try {
            value = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for --" + key + ": " + raw, e);
        }

        if (value < min || value > max) {
            throw new IllegalArgumentException("--" + key + " must be between " + min + " and " + max + ", got " + value);
        }

        return value;
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> result = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + arg + ". Options must start with --.");
            }

            String key = arg.substring(2);
            if (!KNOWN_OPTIONS.contains(key)) {
                throw new IllegalArgumentException("Unknown option: --" + key + ". Use --help to see valid options.");
            }
            if (result.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate option: --" + key);
            }

            if (FLAG_OPTIONS.contains(key)) {
                result.put(key, "true");
                continue;
            }

            if (!VALUE_OPTIONS.contains(key)) {
                throw new IllegalArgumentException("Option configuration error for --" + key);
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for --" + key);
            }

            result.put(key, args[i + 1]);
            i++;
        }

        return result;
    }

    static String defaultBroker() {
        return DEFAULT_BROKER;
    }

    static int defaultQos() {
        return DEFAULT_QOS;
    }

    static String defaultCashierTopic() {
        return DEFAULT_CASHIER_TOPIC;
    }

    static String defaultFittingTopic() {
        return DEFAULT_FITTING_TOPIC;
    }

    static String defaultGateTopic() {
        return DEFAULT_GATE_TOPIC;
    }

    static String defaultPaymentTopic() {
        return DEFAULT_PAYMENT_TOPIC;
    }

    static String defaultCashierReader() {
        return DEFAULT_CASHIER_READER;
    }

    static String defaultFittingReader() {
        return DEFAULT_FITTING_READER;
    }

    static String defaultGateReader() {
        return DEFAULT_GATE_READER;
    }
}
