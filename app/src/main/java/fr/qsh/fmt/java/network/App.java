package fr.qsh.fmt.java.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class App {

    public static void main(String[] args) throws IOException {
        // 1. Création du Selector
        // Le Selector est le cœur de NIO, il permet de surveiller plusieurs canaux pour les opérations d'E/S.
        try (Selector selector = Selector.open();
             ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {

            // 2. Configuration du ServerSocketChannel
            serverSocketChannel.bind(new InetSocketAddress("localhost", 12345));
            serverSocketChannel.configureBlocking(false); // Crucial : passage en mode non-bloquant

            // 3. Enregistrement du canal auprès du Selector
            // On s'intéresse aux nouvelles connexions (OP_ACCEPT).
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Serveur NIO démarré et à l'écoute sur le port 12345...");

            // Le Buffer qui sera utilisé pour lire et écrire les données.
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            // 4. Boucle principale du serveur
            while (true) {
                // select() est bloquant : il attend qu'au moins un canal soit prêt pour une opération.
                int readyChannels = selector.select();
                if (readyChannels == 0) {
                    continue;
                }

                // Récupération des clés pour les canaux qui sont prêts
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    try {
                        // 5. Gérer les événements par type
                        if (key.isAcceptable()) {
                            // Un nouveau client se connecte
                            acceptConnection(key, selector);
                        } else if (key.isReadable()) {
                            // Un client a envoyé des données
                            readAndEcho(key, buffer);
                        }
                    } catch (IOException e) {
                        System.out.println("Connexion fermée par le client : " + e.getMessage());
                        key.cancel(); // Désenregistrer la clé
                        key.channel().close(); // Fermer le canal
                    }

                    // La clé doit être retirée manuellement de l'ensemble
                    keyIterator.remove();
                }
            }
        }
    }

    private static void acceptConnection(SelectionKey key, Selector selector) throws IOException {
        // Le canal de la clé est le ServerSocketChannel
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept(); // Accepter la connexion
        clientChannel.configureBlocking(false); // Le canal client doit aussi être non-bloquant

        // Enregistrer le nouveau canal client auprès du selector pour les opérations de lecture
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("Nouvelle connexion acceptée de : " + clientChannel.getRemoteAddress());
    }

    private static void readAndEcho(SelectionKey key, ByteBuffer buffer) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        buffer.clear(); // Préparer le buffer pour la lecture

        int bytesRead = clientChannel.read(buffer);

        if (bytesRead <= 0) { // Gère aussi le cas où read renvoie 0
            if (bytesRead == -1) {
                // Le client a fermé la connexion
                System.out.println("Client déconnecté : " + clientChannel.getRemoteAddress());
                clientChannel.close();
                key.cancel();
            }
            return;
        }

        // Préparer le buffer pour lire les données qui viennent d'être écrites dedans
        buffer.flip();
        String receivedMessage = StandardCharsets.UTF_8.decode(buffer).toString();
        System.out.println("Reçu de " + clientChannel.getRemoteAddress() + " (" + bytesRead + " octets): " + formatMessageForLog(receivedMessage));

        // Renvoyer les données au client (echo)
        // On utilise rewind() pour remettre la position à 0 et renvoyer les données qu'on vient de lire.
        buffer.rewind();
        clientChannel.write(buffer);
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
