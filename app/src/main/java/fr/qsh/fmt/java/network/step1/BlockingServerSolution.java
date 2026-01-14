package fr.qsh.fmt.java.network.step1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TP1 - CORRIGÉ - Serveur TCP naïf avec approche "thread-per-connection"
 * <p>
 * Ce serveur démontre l'approche classique mais non scalable :
 * - Un thread est créé pour chaque connexion cliente
 * - Chaque thread lit et écrit de manière bloquante
 * <p>
 * LIMITES OBSERVABLES :
 * - Avec 1000+ connexions : plusieurs milliers de threads créés
 * - Consommation mémoire importante (chaque thread ~ 1 MB de stack)
 * - Overhead de context switching important
 * - Temps de réponse qui se dégrade
 * <p>
 * POUR TESTER :
 * 1. Lancer ce serveur
 * 2. Lancer LoadClient avec 100 connexions : OK
 * 3. Lancer LoadClient avec 1000 connexions : observable mais lent
 * 4. Lancer LoadClient avec 5000+ connexions : le système souffre
 */
public class BlockingServerSolution {

    private static final int PORT = 5042;
    private static final AtomicInteger activeConnections = new AtomicInteger(0);
    private static final AtomicInteger totalConnections = new AtomicInteger(0);

    public static void main(String[] args) throws IOException {
        System.out.println("=== TP1 : Serveur Naïf (Thread-per-Connection) - CORRIGÉ ===");
        System.out.println("Port d'écoute : " + PORT);
        System.out.println("=============================================================\n");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✓ Serveur démarré et en écoute...\n");

            // Afficher les statistiques toutes les 5 secondes
            startStatsThread();

            while (true) {
                /* FIXME-S01-01 Accepter une nouvelle connexion */
                Socket clientSocket = serverSocket.accept();
                int connId = totalConnections.incrementAndGet();
                activeConnections.incrementAndGet();

                System.out.println("[Connexion #" + connId + "] Client connecté depuis " +
                        clientSocket.getRemoteSocketAddress());

                new Thread(() -> handleClient(clientSocket, connId)).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur du serveur : " + e.getMessage());
        }
    }

    /**
     * Gère une connexion cliente dans un thread dédié.
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
            System.out.println("[Connexion #" + connId + "] Thread créé : " + Thread.currentThread().getName());

            /* FIXME-S01-02 lire le message depuis la socket et le renvoyer dans la socket */
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
        Thread statsThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    int active = activeConnections.get();
                    int total = totalConnections.get();
                    int currentThreads = Thread.activeCount();
                    System.out.println("\n[Stats] Connexions actives: " + active +
                            " | Total acceptées: " + total +
                            " | Threads actifs: " + currentThreads + "\n");
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statsThread.setDaemon(true);
        statsThread.start();
    }
}
