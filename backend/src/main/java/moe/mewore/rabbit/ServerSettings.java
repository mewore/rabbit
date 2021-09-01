package moe.mewore.rabbit;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;

@Getter
class ServerSettings {

    private static final Pattern SETTING_ARGUMENT_PATTERN = Pattern.compile("^--([^-=][^=]*)=([^=]+)$");

    private static final int DEFAULT_PORT = 8100;

    private static final String DEFAULT_EXTERNAL_STATIC_LOCATION = "static";

    private final int port;

    private final @Nullable String externalStaticLocation;

    ServerSettings(final String[] arguments, final Map<String, String> environmentVariables) {
        final Map<String, String> allProperties = new HashMap<>(environmentVariables);
        for (final String argument : arguments) {
            final Matcher matcher = SETTING_ARGUMENT_PATTERN.matcher(argument);
            if (matcher.matches()) {
                final String key = matcher.group(1);
                final String value = matcher.group(2);
                allProperties.put(key, value);
            }
        }

        port = determinePort(allProperties);
        externalStaticLocation = allProperties.get("rabbit.static.external");
    }

    private static int determinePort(final Map<String, String> properties) {
        final @Nullable String portProperty = properties.get("rabbit.port");
        try {
            return portProperty == null ? DEFAULT_PORT : Integer.parseUnsignedInt(portProperty);
        } catch (final NumberFormatException e) {
            e.printStackTrace();
            return DEFAULT_PORT;
        }
    }
}
