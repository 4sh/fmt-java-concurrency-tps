package fr.qsh.fmt.java.network;

import com.sun.management.UnixOperatingSystemMXBean;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.EventExecutor;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Client de charge Netty qui ouvre plusieurs connexions TCP en parallèle.
 * Chaque connexion :
 * - reste ouverte pendant toute la durée du test
 * - envoie périodiquement des messages (ping)
 * - peut recevoir des réponses (optionnel)
 * <p>
 * Ce client permet de tester la scalabilité d'un serveur sous charge.
 *
 * Métriques mesurées :
 * - Débit d'envoi (msg/sec)
 * - Débit de réception (msg/sec)
 * - Latence moyenne (RTT)
 * - Taux de réponse (%)
 */
public class LoadClient {

    // Configuration par défaut
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5042;
    private static final int DEFAULT_CONNECTIONS = 100;
    private static final int DEFAULT_SEND_INTERVAL_MS = 1000; // 1 seconde
    private static final int STATS_INTERVAL_MS = 5000; // Intervalle d'affichage des stats

    private final String host;
    private final int port;
    private final int connectionCount;
    private final int sendIntervalMs;

    // Compteurs globaux
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger failedConnections = new AtomicInteger(0);
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);

    // Métriques de performance
    private final LongAdder totalLatencyNanos = new LongAdder();
    private final LongAdder latencySampleCount = new LongAdder();
    private final AtomicLong minLatencyNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyNanos = new AtomicLong(0);

    // Pour calculer le débit par intervalle
    private final AtomicLong lastStatsSent = new AtomicLong(0);
    private final AtomicLong lastStatsReceived = new AtomicLong(0);
    private final AtomicLong lastStatsTime = new AtomicLong(System.nanoTime());

    // Map pour stocker les timestamps d'envoi (pour calcul latence)
    private final ConcurrentHashMap<String, Long> pendingPings = new ConcurrentHashMap<>();
    private final AtomicLong pingIdGenerator = new AtomicLong(0);

    public LoadClient(String host, int port, int connectionCount, int sendIntervalMs) {
        this.host = host;
        this.port = port;
        this.connectionCount = connectionCount;
        this.sendIntervalMs = sendIntervalMs;
    }

    public void start() throws InterruptedException {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // Décodage par ligne (messages terminés par \n)
                            pipeline.addLast(new LineBasedFrameDecoder(1024));
                            pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                            pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));

                            // Handler applicatif
                            pipeline.addLast(new ClientHandler(
                                    sendIntervalMs, messagesSent, messagesReceived,
                                    pendingPings, pingIdGenerator,
                                    totalLatencyNanos, latencySampleCount,
                                    minLatencyNanos, maxLatencyNanos));
                        }
                    });

            System.out.println("=== Client de charge Netty ===");
            System.out.println("Serveur cible    : " + host + ":" + port);
            System.out.println("Connexions       : " + connectionCount);
            System.out.println("Intervalle envoi : " + sendIntervalMs + " ms");
            System.out.println("================================\n");

            // Ouvrir les connexions en parallèle
            System.out.println("Ouverture des connexions...");
            for (int i = 0; i < connectionCount; i++) {
                final int connId = i + 1;
                bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        activeConnections.incrementAndGet();
                        if (connId % 100 == 0 || connId == connectionCount) {
                            System.out.println("  → " + connId + " connexions établies");
                        }
                    } else {
                        failedConnections.incrementAndGet();
                        if (failedConnections.get() <= 10) {
                            System.err.println("  ✗ Échec connexion #" + connId + ": " + future.cause().getMessage());
                        } else if (failedConnections.get() == 11) {
                            System.err.println("  ... (autres échecs masqués)");
                        }
                    }
                });

                // Petit délai pour éviter de surcharger le serveur au démarrage
                if (i % 100 == 0 && i > 0) {
                    Thread.sleep(50);
                }
            }

            // Attendre un peu que les connexions s'établissent
            Thread.sleep(2000);
            System.out.println();
            System.out.println("✓ " + activeConnections.get() + " connexions actives");
            if (failedConnections.get() > 0) {
                System.out.println("✗ " + failedConnections.get() + " connexions échouées");
            }
            System.out.println();

            // Afficher les statistiques périodiquement
            Thread statsThread = createStatsThread();
            statsThread.start();

            // Garder le client en vie
            System.out.println("Client actif. Appuyez sur Ctrl+C pour arrêter.\n");
            Thread.currentThread().join();

        } finally {
            group.shutdownGracefully();
            System.out.println();
            System.out.println("════════════════════════════════════════════════════════════════");
            System.out.println("                    STATISTIQUES FINALES                        ");
            System.out.println("════════════════════════════════════════════════════════════════");
            System.out.println();
            System.out.println("  Connexions:");
            System.out.printf("    Établies : %d%n", activeConnections.get());
            System.out.printf("    Échouées : %d%n", failedConnections.get());
            System.out.println();
            System.out.println("  Messages:");
            System.out.printf("    Envoyés  : %,d%n", messagesSent.get());
            System.out.printf("    Reçus    : %,d%n", messagesReceived.get());
            double responseRate = messagesSent.get() > 0 ? (messagesReceived.get() * 100.0 / messagesSent.get()) : 0;
            System.out.printf("    Taux rép.: %.1f%%%n", responseRate);
            System.out.println();
            System.out.println("  Latence (RTT):");
            long samples = latencySampleCount.sum();
            if (samples > 0) {
                double avgLatencyMs = (totalLatencyNanos.sum() / samples) / 1_000_000.0;
                double minLatencyMs = minLatencyNanos.get() == Long.MAX_VALUE ? 0 : minLatencyNanos.get() / 1_000_000.0;
                double maxLatencyMs = maxLatencyNanos.get() / 1_000_000.0;
                System.out.printf("    Moyenne  : %.2f ms%n", avgLatencyMs);
                System.out.printf("    Minimum  : %.2f ms%n", minLatencyMs);
                System.out.printf("    Maximum  : %.2f ms%n", maxLatencyMs);
                System.out.printf("    Échant.  : %,d%n", samples);
            } else {
                System.out.println("    (pas de données de latence)");
            }
            System.out.println();
            System.out.println("════════════════════════════════════════════════════════════════");
        }
    }

    private Thread createStatsThread() {
        Thread statsThread = new Thread(() -> {
            System.out.println("┌─────────────────────────────────────────────────────────────────────────────────────┐");
            System.out.println("│  TEMPS  │  CONN  │  ENVOI/s  │  RECEPT/s │  LATENCE (ms)          │  TAUX REP  │");
            System.out.println("│         │        │           │           │  moy    min    max     │            │");
            System.out.println("├─────────────────────────────────────────────────────────────────────────────────────┤");

            long startTime = System.currentTimeMillis();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(STATS_INTERVAL_MS);

                    // Calculer le temps écoulé
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    String timeStr = String.format("%02d:%02d", elapsed / 60, elapsed % 60);

                    // Calculer le débit sur l'intervalle
                    long now = System.nanoTime();
                    long currentSent = messagesSent.get();
                    long currentReceived = messagesReceived.get();

                    long prevSent = lastStatsSent.getAndSet(currentSent);
                    long prevReceived = lastStatsReceived.getAndSet(currentReceived);
                    long prevTime = lastStatsTime.getAndSet(now);

                    double intervalSec = (now - prevTime) / 1_000_000_000.0;
                    double sendRate = (currentSent - prevSent) / intervalSec;
                    double recvRate = (currentReceived - prevReceived) / intervalSec;

                    // Calculer la latence
                    long samples = latencySampleCount.sum();
                    double avgLatencyMs = samples > 0 ? (totalLatencyNanos.sum() / samples) / 1_000_000.0 : 0;
                    double minLatencyMs = minLatencyNanos.get() == Long.MAX_VALUE ? 0 : minLatencyNanos.get() / 1_000_000.0;
                    double maxLatencyMs = maxLatencyNanos.get() / 1_000_000.0;

                    // Calculer le taux de réponse
                    double responseRate = currentSent > 0 ? (currentReceived * 100.0 / currentSent) : 0;

                    // Afficher la ligne de stats
                    System.out.printf("│  %5s  │  %4d  │  %7.0f  │  %7.0f  │  %5.1f  %5.1f  %6.1f  │  %6.1f%%  │%n",
                            timeStr,
                            activeConnections.get(),
                            sendRate,
                            recvRate,
                            avgLatencyMs,
                            minLatencyMs,
                            maxLatencyMs,
                            responseRate);

                } catch (InterruptedException e) {
                    break;
                }
            }
            System.out.println("└─────────────────────────────────────────────────────────────────────────────────────┘");
        });
        statsThread.setDaemon(true);
        return statsThread;
    }

    /**
     * Handler qui gère la logique d'envoi/réception pour chaque connexion
     */
    private static class ClientHandler extends SimpleChannelInboundHandler<String> {
        private final int sendIntervalMs;
        private final AtomicLong messagesSent;
        private final AtomicLong messagesReceived;
        private final ConcurrentHashMap<String, Long> pendingPings;
        private final AtomicLong pingIdGenerator;
        private final LongAdder totalLatencyNanos;
        private final LongAdder latencySampleCount;
        private final AtomicLong minLatencyNanos;
        private final AtomicLong maxLatencyNanos;

        public ClientHandler(int sendIntervalMs,
                             AtomicLong messagesSent,
                             AtomicLong messagesReceived,
                             ConcurrentHashMap<String, Long> pendingPings,
                             AtomicLong pingIdGenerator,
                             LongAdder totalLatencyNanos,
                             LongAdder latencySampleCount,
                             AtomicLong minLatencyNanos,
                             AtomicLong maxLatencyNanos) {
            this.sendIntervalMs = sendIntervalMs;
            this.messagesSent = messagesSent;
            this.messagesReceived = messagesReceived;
            this.pendingPings = pendingPings;
            this.pingIdGenerator = pingIdGenerator;
            this.totalLatencyNanos = totalLatencyNanos;
            this.latencySampleCount = latencySampleCount;
            this.minLatencyNanos = minLatencyNanos;
            this.maxLatencyNanos = maxLatencyNanos;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            // Démarrer l'envoi périodique de messages avec ID pour mesurer la latence
            EventExecutor executor = ctx.executor();
            executor.scheduleAtFixedRate(() -> {
                if (ctx.channel().isActive()) {
                    long pingId = pingIdGenerator.incrementAndGet();
                    String pingKey = pingId + "";
                    pendingPings.put(pingKey, System.nanoTime());
                    ctx.writeAndFlush("ping:" + pingKey + "\n");
                    messagesSent.incrementAndGet();
                }
            }, 0, sendIntervalMs, TimeUnit.MILLISECONDS);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            messagesReceived.incrementAndGet();

            // Extraire l'ID du ping pour calculer la latence
            String pingKey = null;
            if (msg.startsWith("ping:")) {
                pingKey = msg.substring(5).trim();
            } else if (msg.startsWith("pong:")) {
                pingKey = msg.substring(5).trim();
            }

            if (pingKey != null) {
                Long sendTime = pendingPings.remove(pingKey);
                if (sendTime != null) {
                    long latencyNanos = System.nanoTime() - sendTime;
                    totalLatencyNanos.add(latencyNanos);
                    latencySampleCount.increment();

                    // Mise à jour min/max avec CAS
                    updateMin(minLatencyNanos, latencyNanos);
                    updateMax(maxLatencyNanos, latencyNanos);
                }
            }
        }

        private void updateMin(AtomicLong min, long value) {
            long current;
            while ((current = min.get()) > value) {
                if (min.compareAndSet(current, value)) break;
            }
        }

        private void updateMax(AtomicLong max, long value) {
            long current;
            while ((current = max.get()) < value) {
                if (max.compareAndSet(current, value)) break;
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // En cas d'erreur, fermer silencieusement la connexion
            ctx.close();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Parsing des arguments
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        int connections = DEFAULT_CONNECTIONS;
        int intervalMs = DEFAULT_SEND_INTERVAL_MS;

        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            connections = Integer.parseInt(args[2]);
        }
        if (args.length > 3) {
            intervalMs = Integer.parseInt(args[3]);
        }

        // Vérification via l'API Java
        UnixOperatingSystemMXBean os = (UnixOperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        System.out.println("=== Vérification (API Java) ===");
        System.out.println("Max file descriptors : " + os.getMaxFileDescriptorCount());
        System.out.println("Open file descriptors: " + os.getOpenFileDescriptorCount());
        System.out.println("===============================\n");

        if (os.getMaxFileDescriptorCount() < connections + 100) {
            System.err.println("ATTENTION: Limite FD (" + os.getMaxFileDescriptorCount() +
                    ") < connexions demandées (" + connections + ")");
            System.err.println("Le test risque d'échouer.\n");
        }

        LoadClient client = new LoadClient(host, port, connections, intervalMs);
        client.start();
    }
}
