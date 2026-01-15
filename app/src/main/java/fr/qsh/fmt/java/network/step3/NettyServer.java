package fr.qsh.fmt.java.network.step3;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Step 3 - Serveur TCP avec Netty (Event Loop + Pipeline)
 * <p>
 * Ce serveur démontre l'élégance de Netty pour gérer des milliers de connexions
 * avec un modèle Event Loop et une architecture Pipeline.
 * <p>
 * AVANTAGES par rapport à Step 2 (Java NIO brut) :
 * - Code beaucoup plus simple et lisible
 * - Abstraction élégante (Pipeline, Handlers)
 * - Gestion automatique des buffers
 * - Encoders/Decoders prêts à l'emploi
 * - Performance optimisée
 * <p>
 * AVANTAGES par rapport à Step 1 (thread-per-connection) :
 * - Quelques threads seulement (typiquement 2 × nb_cores)
 * - Scalabilité excellente (10k+ connexions sans problème)
 * - Consommation mémoire réduite
 * <p>
 */
public class NettyServer {

    private static final int PORT = 5044;
    private static final AtomicInteger activeConnections = new AtomicInteger(0);
    private static final AtomicInteger totalConnections = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        System.out.println("=== Step 3 : Serveur Netty (Event Loop) - CORRIGÉ ===");
        System.out.println("Port d'écoute : " + PORT);
        System.out.println("======================================================\n");


        // FIXME-S03-01 : Créer 1 event loop parent de 1 thread qui assurera les ACCEPT et une event loop enfant pour les opération I/0
        EventLoopGroup bossGroup = ;
        EventLoopGroup workerGroup = ;

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    // FIXME-S03-02 : associer les event loop au serveur

                    // FIXME-S03-03 : utiliser un canal NIO Serveur TCP

                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // FIXME-S03-04 : définir le pipeline pour
                            // FIXME-S03-04 : - découper flux entrant en fonction de retour à la ligne
                            // FIXME-S03-04 : - décoder le flux entrant en String
                            // FIXME-S03-04 : - Traiter le flux entrant par EchoHandler
                            // FIXME-S03-04 : - encoder le flux sortant vers du Byte

                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Afficher les statistiques toutes les 5 secondes
            startStatsThread();

            System.out.println("✓ Serveur Netty démarré et en écoute...\n");

            // FIXME-S03-06 écouter PORT et attendre que l'écoute soit effective

            // FIXME-S03-07 attendre que le canal serveur se ferme


        } finally {
            // Arrêter proprement les EventLoopGroup
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    /**
     * SOLUTION 4 : Handler d'écho
     */
    private static class EchoHandler extends SimpleChannelInboundHandler<String> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            int connId = totalConnections.incrementAndGet();
            activeConnections.incrementAndGet();
            System.out.println("[Connexion #" + connId + "] Client connecté depuis " +
                    ctx.channel().remoteAddress());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String message) {
            // FIXME-S03-05 si le message n'est pas vide, écrire le même message en réponse

        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            activeConnections.decrementAndGet();
            System.out.println("[Connexion] Client déconnecté : " + ctx.channel().remoteAddress());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("Erreur : " + cause.getMessage());
            ctx.close();
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
                    System.out.println("\n[Stats Netty] Connexions actives: " + active +
                            " | Total acceptées: " + total +
                            " | Threads actifs: " + threads + " (quelques-uns suffisent !)\n");
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statsThread.setDaemon(true);
        statsThread.start();
    }
}
