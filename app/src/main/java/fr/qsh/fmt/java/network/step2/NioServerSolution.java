package fr.qsh.fmt.java.network.step2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Step 2 - CORRIGÉ - Serveur TCP avec Java NIO (Selector + non-blocking I/O)
 *
 * Ce serveur démontre l'utilisation de Java NIO pour gérer des milliers de connexions
 * avec un seul thread grâce aux I/O non-bloquants.
 *
 * AVANTAGES par rapport à Step 1 :
 * - Un seul thread pour toutes les connexions
 * - Pas de context switching coûteux
 * - Consommation mémoire réduite
 * - Scalabilité bien meilleure
 *
 * POUR TESTER :
 * 1. Lancer ce serveur
 * 2. Lancer LoadClient sur le port 5043 avec 1000, 5000, ou 10000 connexions
 * 3. Observer qu'un seul thread suffit !
 */
public class NioServerSolution {

    private static final int PORT = 5043;
    private static final AtomicInteger activeConnections = new AtomicInteger(0);
    private static final AtomicInteger totalConnections = new AtomicInteger(0);

    /**
     * Buffer helper pour stocker les données en cours de lecture
     */
    private static class ClientState {
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        StringBuilder lineBuffer = new StringBuilder();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("=== Step 2 : Serveur Java NIO (Selector) - CORRIGÉ ===");
        System.out.println("Port d'écoute : " + PORT);
        System.out.println("Thread unique : " + Thread.currentThread().getName());
        System.out.println("=======================================================\n");

        // Créer le Selector
        Selector selector = Selector.open();

        // Créer et configurer le ServerSocketChannel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        // FIXME-S02-01 Ecouter sur PORT et passer en mode non-bloquant
        serverChannel.bind(new InetSocketAddress(PORT));
        serverChannel.configureBlocking(false);

        // FIXME-S02-02 Enregistrer l'opération ACCEPT sur le Selector
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("✓ Serveur NIO démarré et en écoute...\n");

        // Afficher les statistiques toutes les 5 secondes
        startStatsThread();

        // Boucle principale du serveur
        while (true) {
            // Attendre qu'au moins un canal soit prêt
            selector.select(); // Bloque jusqu'à ce qu'un événement se produise

            // Récupérer les clés sélectionnées (canaux prêts)
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove(); // IMPORTANT : retirer la clé de l'ensemble

                if (!key.isValid()) {
                    continue;
                }

                try {
                    // FIXME-S02-02 Si un accept s'est produit, appeler handleAccept
                    // FIXME-S02-02 Si un read s'est produit, appeler handleRead
                    // FIXME-S02-02 Si un write s'est produit, appeler handleWrite
                    if (key.isAcceptable()) {
                        handleAccept(key, selector);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                } catch (IOException e) {
                    System.err.println("Erreur lors du traitement : " + e.getMessage());
                    closeChannel(key);
                }
            }
        }
    }

    /**
     * Accepter une nouvelle connexion
     */
    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        // FIXME-S02-03 Récupérer le canal serveur et accepter la connexion afin de récupérer le canal client
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel != null) {
            int connId = totalConnections.incrementAndGet();
            activeConnections.incrementAndGet();

            System.out.println("[Connexion #" + connId + "] Client connecté depuis " +
                    clientChannel.getRemoteAddress());

            // FIXME-S02-04 configurer le canal client en non bloquant
            clientChannel.configureBlocking(false);

            // FIXME-S02-05 enregistrer l'opération READ sur le Selector
            SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);

            // Attacher un état pour ce client
            clientKey.attach(new ClientState());
        }
    }

    /**
     * Lire les données d'un client
     */
    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ClientState state = (ClientState) key.attachment();

        // FIXME-S02-06 lire les données du canal client dans le buffer
        int bytesRead = clientChannel.read(state.readBuffer);

        if (bytesRead == -1) {
            // Client déconnecté
            System.out.println("[Connexion] Client déconnecté : " + clientChannel.getRemoteAddress());
            closeChannel(key);
            return;
        }

        if (bytesRead > 0) {
            // FIXME-S02-07 le buffer doit pouvoir faire de la lecture
            state.readBuffer.flip();

            // Décoder les données reçues
            while (state.readBuffer.hasRemaining()) {
                char c = (char) state.readBuffer.get();
                if (c == '\n') {
                    // Ligne complète reçue
                    String line = state.lineBuffer.toString();
                    state.lineBuffer.setLength(0); // Reset

                    if (!line.isEmpty()) {
                        // Préparer la réponse (écho)
                        String response = line + "\n";
                        ByteBuffer writeBuffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));

                        // Attacher le buffer de réponse et passer en mode WRITE
                        key.attach(writeBuffer);
                        // FIXME-S02-08 configurer la key pour être informé de la disponibilité de WRITE
                        key.interestOps(SelectionKey.OP_WRITE);
                        return; // Sortir pour traiter l'écriture plus tard
                    }
                } else {
                    state.lineBuffer.append(c);
                }
            }

            // Préparer le buffer pour la prochaine lecture
            state.readBuffer.compact();
        }
    }

    /**
     * SOLUTION : Écrire les données à un client
     */
    private static void handleWrite(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer writeBuffer = (ByteBuffer) key.attachment();

        // FIXME-S02-09 écrire les données du canal client dans le buffer
        clientChannel.write(writeBuffer);

        if (!writeBuffer.hasRemaining()) {
            // Tout est écrit, revenir en mode READ
            ClientState state = new ClientState();
            key.attach(state);
            // FIXME-S02-10 configurer la key pour être informé de la disponibilité de READ
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    /**
     * Ferme proprement un canal
     */
    private static void closeChannel(SelectionKey key) {
        try {
            key.channel().close();
            activeConnections.decrementAndGet();
        } catch (IOException e) {
            // Ignorer
        }
    }

    /**
     * Thread d'affichage des statistiques
     */
    private static void startStatsThread() {
        Thread statsThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    int active = activeConnections.get();
                    int total = totalConnections.get();
                    int threads = Thread.activeCount();
                    System.out.println("\n[Stats NIO] Connexions actives: " + active +
                            " | Total acceptées: " + total +
                            " | Threads actifs: " + threads + " (un seul suffit !)\n");
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statsThread.setDaemon(true);
        statsThread.start();
    }
}
