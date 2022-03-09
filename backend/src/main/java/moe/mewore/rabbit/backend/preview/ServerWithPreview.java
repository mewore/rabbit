package moe.mewore.rabbit.backend.preview;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.Server;
import moe.mewore.rabbit.backend.ServerSettings;

@RequiredArgsConstructor
public class ServerWithPreview {

    private final String[] args;

    public static void main(final String[] args) throws IOException {
        new ServerWithPreview(args).open();
    }

    private void open() throws IOException {
        final Server server = Server.create(new ServerSettings(args, System.getenv()));

        final JFrame frame = new JFrame("Rabbit");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        final JMenuBar mb = new JMenuBar();
        frame.getContentPane().add(BorderLayout.NORTH, mb);

        final ServerPreviewCanvas previewCanvas = new ServerPreviewCanvas(server.getMap(), server.getWorldState());
        server.onWorldUpdate(state -> previewCanvas.updatePlayerOverlay());

        frame.getContentPane().add(BorderLayout.CENTER, previewCanvas);

        frame.setVisible(true);

        server.start();
    }
}
