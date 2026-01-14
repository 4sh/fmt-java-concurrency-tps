package fr.qsh.fmt.java.network.step4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Step 4 - CORRIGÉ - Serveur TCP avec Java Loom (Virtual Threads)
 *
 * Ce serveur démontre la puissance des Virtual Threads de Java 21+ :
 * on revient à un style simple "thread-per-connection" avec du code synchrone,
 * MAIS sans les limitations des threads traditionnels.
 *
 * AVANTAGES des Virtual Threads :
 * - Code simple et synchrone (comme Step 1)
 * - Pas de callback hell, pas de CompletableFuture complexes
 * - Scalabilité excellente (millions de virtual threads possibles)
 * - Les I/O bloquants sont automatiquement optimisés par la JVM
 * - Coût mémoire très faible (~1 KB par virtual thread vs ~1 MB pour platform thread)
 *
 * COMPARAISON :
 * - Step 1 (platform threads) : max ~1000-2000 connexions
 * - Step 2 (Java NIO) : max 10k+ connexions, mais code complexe
 * - Step 3 (Netty) : max 10k+ connexions, abstraction élégante
 * - Step 4 (Loom) : max 10k+ connexions, code simple ET performant
 *
 * POUR TESTER :
 * 1. Lancer ce serveur (nécessite Java 21+)
 * 2. Lancer LoadClient sur le port 5045 avec 1000, 5000, ou 10000 connexions
 * 3. Observer : peu de platform threads, beaucoup de virtual threads, excellente performance
 */
public class LoomServerSolution {

    private static final int PORT = 5045;
    private static final AtomicInteger activeConnections = new AtomicInteger(0);
    private static final AtomicInteger totalConnections = new AtomicInteger(0);

    public static void main(String[] args) throws IOException {
        System.out.println("=== Step 4 : Serveur Java Loom (Virtual Threads) - CORRIGÉ ===");
        System.out.println("Port d'écoute : " + PORT);
        System.out.println("===============================================================\n");

        if (!isVirtualThreadsSupported()) {
            System.err.println("⚠️  Les Virtual Threads nécessitent Java 21 ou supérieur !");
            System.err.println("Version actuelle : " + Runtime.version());
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✓ Serveur Loom démarré et en écoute...\n");

            // Afficher les statistiques toutes les 5 secondes
            startStatsThread();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int connId = totalConnections.incrementAndGet();
                activeConnections.incrementAndGet();

                System.out.println("[Connexion #" + connId + "] Client connecté depuis " +
                        clientSocket.getRemoteSocketAddress());

                /* FIXME-S04-01 Utiliser un thread vrtuel */
               Thread.startVirtualThread(() -> handleClient(clientSocket, connId));
            }
        } catch (IOException e) {
            System.err.println("Erreur du serveur : " + e.getMessage());
        }
    }

    /**
     * Gère une connexion cliente dans un virtual thread.
     * Cette méthode doit :
     * 1. Lire les messages envoyés par le client ligne par ligne
     * 2. Renvoyer chaque message en écho au client
     * 3. Gérer proprement la fermeture de la connexion
     *
     * @param clientSocket le socket de la connexion cliente
     * @param connId       l'identifiant unique de la connexion
     */
    private static void handleClient(Socket clientSocket, int connId) {
        try (
                Socket socket = clientSocket;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            System.out.println("[Connexion #" + connId + "] Virtual Thread créé : " + Thread.currentThread().getName());

            in.lines()
                    .filter(message -> !message.isEmpty())
                    .forEach(out::println);

            System.out.println("[Connexion #" + connId + "] Client déconnecté");

        } catch (IOException e) {
            System.err.println("[Connexion #" + connId + "] Erreur I/O : " + e.getMessage());
        } finally {
            activeConnections.decrementAndGet();
            System.out.println("[Connexion #" + connId + "] Connexions actives : " +
                    activeConnections.get());
        }
    }

    /**
     * Thread d'affichage des statistiques toutes les 5 secondes
     */
    private static void startStatsThread() {
        Thread.startVirtualThread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    int active = activeConnections.get();
                    int total = totalConnections.get();
                    int platformThreads = Thread.activeCount();

                    System.out.println("\n[Stats Loom] Connexions actives: " + active +
                            " | Total acceptées: " + total +
                            " | Platform threads: " + platformThreads +
                            " | Virtual threads: ~" + active + " (un par connexion !)\n");
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    /**
     * Vérifie si les Virtual Threads sont supportés (Java 21+)
     */
    private static boolean isVirtualThreadsSupported() {
        try {
            Thread.class.getMethod("startVirtualThread", Runnable.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
