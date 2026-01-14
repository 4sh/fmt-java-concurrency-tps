package fr.qsh.fmt.java.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BlockingClient {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;
    private static final String MESSAGE_FILE_PATH = "long_message.txt";
    private static final long SEND_INTERVAL_MS = 2000; // Envoyer toutes les 2 secondes

    public static void main(String[] args) {
        // Lire le contenu du fichier
        byte[] fileContentBytes;
        try {
            Path filePath = Paths.get(MESSAGE_FILE_PATH);
            fileContentBytes = Files.readAllBytes(filePath);
            System.out.println("Client va envoyer le contenu du fichier: " + MESSAGE_FILE_PATH + " (taille: " + fileContentBytes.length + " octets)");
        } catch (IOException e) {
            System.err.println("Erreur de lecture du fichier " + MESSAGE_FILE_PATH + ": " + e.getMessage());
            return;
        }

        // Le try-with-resources garantit que le socket sera fermé à la fin
        try (Socket socket = new Socket(HOST, PORT)) {
            System.out.println("Client bloquant connecté à " + HOST + ":" + PORT);

            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            while (!socket.isClosed()) {
                // 1. Envoyer les données
                System.out.println("Client envoie (" + fileContentBytes.length + " octets)...");
                outputStream.write(fileContentBytes);
                outputStream.flush(); // S'assurer que les données sont envoyées
                System.out.println("Client a envoyé (" + fileContentBytes.length + " octets): " + formatMessageForLog(new String(fileContentBytes, StandardCharsets.UTF_8)));


                // 2. Lire la réponse (l'écho)
                // Le serveur renvoie exactement ce qu'il a reçu.
                // On doit donc s'attendre à recevoir le même nombre d'octets.
                byte[] responseBuffer = new byte[fileContentBytes.length];
                int totalBytesRead = 0;
                // Boucle de lecture pour s'assurer de lire toute la réponse
                while (totalBytesRead < fileContentBytes.length) {
                    int bytesRead = inputStream.read(responseBuffer, totalBytesRead, responseBuffer.length - totalBytesRead);
                    if (bytesRead == -1) {
                        System.out.println("Le serveur a fermé la connexion prématurément.");
                        break;
                    }
                    totalBytesRead += bytesRead;
                }

                if (totalBytesRead > 0) {
                     String receivedMessage = new String(responseBuffer, 0, totalBytesRead, StandardCharsets.UTF_8);
                     System.out.println("Client a reçu du serveur (" + totalBytesRead + " octets): " + formatMessageForLog(receivedMessage));
                }

                // Attendre avant le prochain envoi
                Thread.sleep(SEND_INTERVAL_MS);
            }
        } catch (IOException e) {
            System.err.println("Erreur du client bloquant: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Le client a été interrompu.");
            Thread.currentThread().interrupt(); // Restaurer le statut d'interruption
        }
        System.out.println("Client bloquant arrêté.");
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
