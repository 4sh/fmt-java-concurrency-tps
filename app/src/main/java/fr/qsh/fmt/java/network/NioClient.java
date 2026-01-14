package fr.qsh.fmt.java.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;

public class NioClient {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;
    private static final String MESSAGE_FILE_PATH = "long_message.txt";
    private static final long SEND_INTERVAL_MS = 2000; // Envoyer toutes les 2 secondes

    public static void main(String[] args) throws IOException {
        // Lire le contenu du fichier
        byte[] fileContentBytes;
        String messageToSend;
        Path filePath = Paths.get(MESSAGE_FILE_PATH);
        try {
            fileContentBytes = Files.readAllBytes(filePath);
            messageToSend = new String(fileContentBytes, StandardCharsets.UTF_8);
            System.out.println("Client va envoyer le contenu du fichier: " + MESSAGE_FILE_PATH + " (taille: " + fileContentBytes.length + " octets)");
        } catch (IOException e) {
            System.err.println("Erreur de lecture du fichier " + MESSAGE_FILE_PATH + ": " + e.getMessage());
            return;
        }

        try (Selector selector = Selector.open();
             SocketChannel socketChannel = SocketChannel.open()) {

            socketChannel.configureBlocking(false); // Mode non-bloquant
            socketChannel.connect(new InetSocketAddress(HOST, PORT)); // Tenter de se connecter

            // Enregistrer le canal pour les événements de connexion et de lecture
            socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);

            System.out.println("Client NIO démarré, tentative de connexion à " + HOST + ":" + PORT);

            ByteBuffer writeBuffer = ByteBuffer.wrap(fileContentBytes);
            ByteBuffer readBuffer = ByteBuffer.allocate(1024); // Le buffer de lecture reste de 1KB
            long lastSendTime = 0;

            while (true) {
                selector.select(100); // Attend 100ms max

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isConnectable()) {
                        handleConnect(key);
                    } else if (key.isReadable()) {
                        handleRead(key, readBuffer);
                    }
                }

                // Si le canal est connecté et que le temps d'envoi est écoulé
                if (socketChannel.isConnected() && System.currentTimeMillis() - lastSendTime >= SEND_INTERVAL_MS) {
                    writeBuffer.rewind(); // Remet la position à 0 pour relire le message
                    try {
                        int bytesWritten = socketChannel.write(writeBuffer);
                        if (bytesWritten > 0) {
                            System.out.println("Client a envoyé (" + bytesWritten + " octets): " + formatMessageForLog(messageToSend));
                            lastSendTime = System.currentTimeMillis();
                        }
                    } catch (IOException e) {
                        System.err.println("Erreur lors de l'envoi au serveur: " + e.getMessage());
                        socketChannel.close();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur du client NIO: " + e.getMessage());
        }
        System.out.println("Client NIO arrêté.");
    }

    private static void handleConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.isConnectionPending()) {
            channel.finishConnect(); // Finalise la connexion non-bloquante
        }
        System.out.println("Client connecté au serveur.");
        key.interestOps(SelectionKey.OP_READ); // Une fois connecté, on s'intéresse à la lecture
    }

    private static void handleRead(SelectionKey key, ByteBuffer readBuffer) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        readBuffer.clear(); // Prépare le buffer pour la lecture

        int bytesRead = channel.read(readBuffer);

        if (bytesRead <= 0) { // Gère aussi le cas où read renvoie 0
            if (bytesRead == -1) {
                System.out.println("Serveur a fermé la connexion.");
                channel.close();
                key.cancel();
            }
            return;
        }

        readBuffer.flip(); // Prépare le buffer pour être lu
        String receivedMessage = StandardCharsets.UTF_8.decode(readBuffer).toString();
        System.out.println("Client a reçu du serveur (" + bytesRead + " octets): " + formatMessageForLog(receivedMessage));
    }

    private static String formatMessageForLog(String message) {
        String trimmedMessage = message.trim();
        if (trimmedMessage.length() <= 100) {
            return trimmedMessage;
        }
        return trimmedMessage.substring(0, 50) +
               "..." +
               trimmedMessage.substring(trimmedMessage.length() - 50);
    }
}

