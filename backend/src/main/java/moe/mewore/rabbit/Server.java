package moe.mewore.rabbit;

import lombok.RequiredArgsConstructor;
import spark.Service;

import static spark.Service.ignite;

@RequiredArgsConstructor
public class Server {

    private final ServerSettings settings;

    private final Service sparkService;

    public static void main(final String[] args) {
        new Server(new ServerSettings(args, System.getenv()), ignite()).initialize();
    }

    public void initialize() {
        sparkService.port(settings.getPort());
        sparkService.staticFiles.externalLocation("static");
        sparkService.staticFiles.location("static");
        sparkService.init();
    }
}
