package org.example.rfid.config;

import java.util.Locale;

public final class HelpPrinter {

    private HelpPrinter() {
    }

    public static void print() {
        StringBuilder help = new StringBuilder();
        help.append("Usage: java -jar rfid-producer-<version>.jar [options]").append(System.lineSeparator());
        help.append(System.lineSeparator());
        help.append("Options:").append(System.lineSeparator());

        for (CliOptions.OptionSpec option : CliOptions.all()) {
            help.append(String.format(Locale.ROOT, "  %-44s %s%n", option.syntax(), option.description()));
        }

        help.append(System.lineSeparator());
        help.append("Profile defaults:").append(System.lineSeparator());
        help.append(profileDefaultsLine("low", Profile.LOW)).append(System.lineSeparator());
        help.append(profileDefaultsLine("medium", Profile.MEDIUM)).append(System.lineSeparator());
        help.append(profileDefaultsLine("high", Profile.HIGH)).append(System.lineSeparator());

        System.out.println(help);
    }

    private static String profileDefaultsLine(String name, Profile profile) {
        return String.format(
                Locale.ROOT,
                "  %-6s new=%.2f fitting=%.2f postCashierFitting=%.2f skipCashier=%.2f paymentTx=%.2f paymentItem=%.2f unpaidExit=%.2f threads=%d paymentMaxTxPerTick=%d",
                name + ":",
                profile.newProductRate(),
                profile.fittingVisitRate(),
                profile.postCashierFittingRate(),
                profile.skipCashierRate(),
                profile.paymentTransactionRate(),
                profile.paymentItemRate(),
                profile.unpaidExitRate(),
                profile.scenarioRunnerThreadCount(),
                profile.paymentMaxTransactionsPerTick()
        );
    }
}
