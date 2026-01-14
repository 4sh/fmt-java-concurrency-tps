package fr.qsh.fmt.java.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CodeSnippet {

    static class Ip {
        public static void main(String[] args) throws UnknownHostException {

            InetAddress dest = InetAddress.getByName("216.58.215.36");
            System.out.println(dest.getHostAddress());

        }
    }

    static class Connect {
        public static void main(String[] args) throws IOException {

            InetAddress dest = InetAddress.getByName("216.58.215.36");

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(dest, 80));
            }

        }
    }

    static class Dns {
        public static void main(String[] args) throws UnknownHostException {

            InetAddress dest = InetAddress.getByName("www.google.com");
            System.out.println(dest.getHostAddress()); // 216.58.215.36

        }
    }

    static class Stream {
        public static void main(String[] args) throws IOException {

            InetAddress dest = InetAddress.getByName("tcpbin.com");

            try (Socket socket = new Socket(dest, 4242)) {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write("Hello World !\n".getBytes());
                outputStream.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.println(reader.readLine());
            }

        }
    }

    static class Udp {
        public static void main(String[] args) throws IOException {

            DatagramSocket socket = new DatagramSocket();
            byte[] data = "Hello UDP".getBytes();

            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(data, data.length, address, 5001);
            socket.send(packet);
            socket.close();

        }
    }

    static class IoServer {
        public static void main(String[] args) throws IOException {

            ServerSocket server = new ServerSocket(5000);
            Socket client = server.accept();

            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            while (true) {
                String msg = in.readLine();
                System.out.println("Client 1 : " + msg);
            }

        }
    }

    static class BlockingServer {
        public static void main(String[] args) throws IOException {

            ServerSocket server = new ServerSocket(5000);
            Socket client1 = server.accept();
            Socket client2 = server.accept();
            BufferedReader in1 = bufferReader(client1);
            BufferedReader in2 = bufferReader(client2);
            while (true) {
                String msg1 = in1.readLine(); // Bloquant si pas de msg de 1
                System.out.println("Client 1 : " + msg1);

                String msg2 = in2.readLine();
                System.out.println("Client 2 : " + msg2);
            }

        }

        private static BufferedReader bufferReader(Socket client1) throws IOException {
            return new BufferedReader(new InputStreamReader(client1.getInputStream()));
        }
    }

    static class BlockingThreadedServer {
        public static void main(String[] args) throws IOException {
            ServerSocket server = new ServerSocket(5000);

            while (true) {
                Socket client = server.accept();

                new Thread(() -> {
                    try (BufferedReader in = bufferReader(client)) {
                        String msg;
                        while ((msg = in.readLine()) != null) {
                            System.out.println(client + " : " + msg);
                        }
                    } catch (IOException e) {
                        System.out.println("bye : " + client);
                    }
                }).start();
            }

        }

        private static BufferedReader bufferReader(Socket client1) throws IOException {
            return new BufferedReader(new InputStreamReader(client1.getInputStream()));
        }
    }

    static class NioConnect {
        public static void main(String[] args) throws IOException {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(5042));
            server.configureBlocking(false);

            while (true) {
                // non bloquant
                SocketChannel client = server.accept();

                if (client != null) {
                    System.out.println("Client connecté");
                } else {
                    System.out.println("Pas de connection");
                }
            }

        }
    }

    static class NioClient {
        public static void main(String[] args) throws IOException {
            SocketChannel channel = SocketChannel.open();
            channel.connect(new InetSocketAddress("localhost", 5042));

            ByteBuffer buffer = ByteBuffer.wrap("Hello World".getBytes());

            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }

            channel.close();

        }
    }

    static class NioEcho1 {
        public static void main(String[] args) throws IOException {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(5042));
            server.configureBlocking(false);
            SocketChannel client = null;
            while (true) {
                if (client == null) {
                    client = server.accept();
                }
                if (client != null) {
                    client.configureBlocking(false);
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int nbRead = client.read(buffer);
                    switch (Integer.compare(nbRead, 0)) {
                        case -1 -> {
                            System.out.println("Fin de connection");
                            System.exit(0);
                        }
                        case 0 -> System.out.println("Rien de reçu");
                        case 1 -> {
                            buffer.flip();
                            System.out.println(
                                    UTF_8.decode(buffer));
                        }
                    }
                }
            }

        }
    }

    static class NioEcho2 {
        public static void main(String[] args) throws IOException {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(5042));
            server.configureBlocking(false);
            SocketChannel client = null;
            while (true) {
                if (client == null) {
                    client = server.accept();
                }
                if (client != null) {
                    client.configureBlocking(false);
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int nbRead = client.read(buffer);
                    switch (Integer.compare(nbRead, 0)) {
                        case -1 -> {
                            System.out.println("Fin de connection");
                            System.exit(0);
                        }
                        case 0 -> System.out.println("Rien de reçu");
                        case 1 -> {
                            buffer.flip();
                            String msg = UTF_8.decode(buffer).toString();
                            buffer.clear();

                            if (msg.contains("\n")) {
                                byte[] bytes = msg.getBytes(UTF_8);
                                ByteBuffer out = ByteBuffer.wrap(bytes);
                                while (out.hasRemaining()) {
                                    client.write(out);
                                }
                            } else {
                                System.out.println("attente de la suite...");
                            }
                        }
                    }
                }
            }

        }
    }

    static class SelectorAccept {
        public static void main(String[] args) throws IOException {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(5042));
            server.configureBlocking(false);

            Selector selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                System.out.println("wait...");
                selector.select();

                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isAcceptable()) {
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        System.out.println("Client connecté");
                    }

                    if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        client.read(buffer);
                        buffer.flip();
                        System.out.println("Reçu " + client + " : " + UTF_8.decode(buffer));
                    }
                }
            }
        }

    }

    static class NioEchoComplete {
        public static void main(String[] args) throws IOException {
            int port = 5042;
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(port));
            server.configureBlocking(false);
            Selector selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("NIO Echo Server started on port " + port);
            Map<SocketChannel, ByteBuffer> clientBuffers = new HashMap<>();
            while (true) {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        clientBuffers.put(client, ByteBuffer.allocate(1024));
                    }
                    if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer buf = clientBuffers.get(client);
                        int bytesRead = client.read(buf);
                        if (bytesRead == -1) {
                            clientBuffers.remove(client);
                            key.cancel();
                            client.close();
                            System.out.println("Client disconnected");
                            continue;
                        }
                        buf.flip();
                        while (buf.hasRemaining()) {
                            byte b = buf.get();
                            if (b == '\n') {
                                int pos = buf.position();
                                int limit = buf.limit();
                                buf.position(0);
                                buf.limit(pos);
                                CharBuffer line = Charset.forName("UTF-8").decode(buf);
                                System.out.println("=>" + line.toString().trim());
                                ByteBuffer echoBuf = Charset.forName("UTF-8").encode(line);
                                while (echoBuf.hasRemaining()) {
                                    client.write(echoBuf);
                                }
                                buf.position(pos);
                                buf.limit(limit);
                                ByteBuffer remaining = ByteBuffer.allocate(1024);
                                remaining.put(buf);
                                buf.clear();
                                buf.put(remaining);
                                break;
                            }
                        }
                        buf.compact();
                    }
                }
            }
        }
    }

    static class NioEchoComplete2 {
        public static void main(String[] args) throws IOException {
            int port = 5042;
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(port));
            server.configureBlocking(false);
            Selector selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("NIO Echo Server (pooled buffers) started on port " + port);
            Queue<ByteBuffer> bufferPool = new ArrayDeque<>();
            Charset utf8 = Charset.forName("UTF-8");
            Map<SocketChannel, ByteBuffer> clientBuffers = new HashMap<>();
            while (true) {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        ByteBuffer buf = bufferPool.poll();
                        if (buf == null) buf = ByteBuffer.allocate(1024);
                        else buf.clear();
                        clientBuffers.put(client, buf);
                    }
                    if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer buf = clientBuffers.get(client);
                        int bytesRead = client.read(buf);
                        if (bytesRead == -1) {
                            clientBuffers.remove(client);
                            key.cancel();
                            client.close();
                            bufferPool.offer(buf);
                            continue;
                        }
                        buf.flip();
                        int start = 0;
                        for (int i = 0; i < buf.limit(); i++) {
                            if (buf.get(i) == '\n') {
                                int end = i + 1;
                                ByteBuffer lineBuf = buf.duplicate();
                                lineBuf.position(start);
                                lineBuf.limit(end);
                                CharBuffer line = utf8.decode(lineBuf);
                                System.out.println("=>" + line.toString().trim());
                                ByteBuffer echoBuf = utf8.encode(line.toString() + "\n");
                                while (echoBuf.hasRemaining()) {
                                    client.write(echoBuf);
                                }
                                start = end;
                            }
                        }
                        buf.position(start);
                        buf.compact();
                    }
                }
            }
        }
    }


    static class AsyncServer {
        public static void main(String[] args) throws IOException {
            AsynchronousServerSocketChannel server =
                    AsynchronousServerSocketChannel.open()
                            .bind(new InetSocketAddress(5042));

            server.accept(null,
                    new CompletionHandler<AsynchronousSocketChannel, Void>() {
                        @Override
                        public void completed(AsynchronousSocketChannel client, Void attachment) {
                            System.out.println("Client connected: " + client);
                            //server.accept(null, this);
                        }

                        @Override
                        public void failed(Throwable exc, Void attachment) {
                            exc.printStackTrace();
                        }
                    });

            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}



