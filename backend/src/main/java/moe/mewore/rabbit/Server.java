package moe.mewore.rabbit;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Server {

    public static void main(final String[] args) {
        start(new ServerSettings(args, System.getenv()));
    }

    static Javalin start(final ServerSettings settings) {
        final Javalin javalin = Javalin.create(config -> {
            config.addStaticFiles("static");
            final @Nullable String externalStaticLocation = settings.getExternalStaticLocation();
            if (externalStaticLocation != null) {
                config.addStaticFiles(settings.getExternalStaticLocation(), Location.EXTERNAL);
            }
        });
        return javalin.start(settings.getPort());
    }
}
