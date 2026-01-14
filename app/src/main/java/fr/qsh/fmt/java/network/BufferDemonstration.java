package fr.qsh.fmt.java.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BufferDemonstration {

    public static void main(String[] args) {
        System.out.println("--- Démonstration de ByteBuffer ---");

        // 1. Allocation d'un ByteBuffer
        // allocate(capacity) crée un nouveau buffer.
        // position = 0, limit = capacity, capacity = capacity.
        ByteBuffer buffer = ByteBuffer.allocate(10); // Un buffer de 10 octets de capacité
        printBufferState("Après allocation (capacité 10)", buffer);

        // 2. Écriture (put) dans le buffer
        // La position avance à chaque écriture.
        System.out.println("\n--- Écriture dans le buffer ---");
        buffer.put((byte) 'H');
        buffer.put((byte) 'e');
        buffer.put((byte) 'l');
        printBufferState("Après put 'H', 'e', 'l'", buffer);

        byte[] world = "lo".getBytes(StandardCharsets.UTF_8);
        buffer.put(world); // Écriture d'un tableau d'octets
        printBufferState("Après put 'lo'", buffer);

        // Tentative d'écriture au-delà de la limite (qui est égale à la capacité ici)
        // Ceci lèverait une BufferOverflowException si on essayait d'écrire plus que la capacité restante.
        // buffer.put((byte) '!'); // Commenté pour éviter l'exception

        // 3. Flip (retournement) du buffer
        // Prépare le buffer pour la lecture :
        // limit = position actuelle, position = 0.
        System.out.println("\n--- Flip du buffer (prêt pour la lecture) ---");
        buffer.flip();
        printBufferState("Après flip", buffer);

        // 4. Lecture (get) depuis le buffer
        // La position avance à chaque lecture.
        System.out.println("\n--- Lecture depuis le buffer ---");
        System.out.println("Lecture 1er octet: " + (char) buffer.get());
        printBufferState("Après get 1 octet", buffer);

        byte[] readBytes = new byte[2];
        buffer.get(readBytes); // Lecture de 2 octets dans un tableau
        System.out.println("Lecture 2 octets: " + new String(readBytes, StandardCharsets.UTF_8));
        printBufferState("Après get 2 octets", buffer);

        // 5. Rewind (rembobinage) du buffer
        // Remet la position à 0, la limite reste inchangée. Permet de relire depuis le début.
        System.out.println("\n--- Rewind du buffer (relire depuis le début) ---");
        buffer.rewind();
        printBufferState("Après rewind", buffer);

        System.out.println("Re-lecture 1er octet: " + (char) buffer.get());
        printBufferState("Après re-get 1 octet", buffer);

        // 6. Clear (nettoyage) du buffer
        // Prépare le buffer pour une nouvelle écriture complète :
        // position = 0, limit = capacity. Le contenu n'est pas effacé mais "oublié".
        System.out.println("\n--- Clear du buffer (prêt pour nouvelle écriture) ---");
        buffer.clear();
        printBufferState("Après clear", buffer);

        // Écriture de nouvelles données après clear
        buffer.put("Java".getBytes(StandardCharsets.UTF_8));
        printBufferState("Après put 'Java'", buffer);

        // 7. Compact (compactage) du buffer
        // Déplace les données non lues (entre position et limit) au début du buffer.
        // position = nombre de bytes non lues, limit = capacity. Prépare pour écriture à la suite des données non lues.
        System.out.println("\n--- Compact du buffer ---");
        buffer.flip(); // On écrit "Java", donc on flip pour simuler une lecture partielle
        printBufferState("Après flip (avant compact)", buffer);
        buffer.get(); // Lecture partielle 'J'
        buffer.get(); // Lecture partielle 'a'
        printBufferState("Après lecture partielle 'Ja'", buffer);

        buffer.compact();
        printBufferState("Après compact", buffer); // 'va' devrait être au début, position = 2 (longueur de 'va')

        // Écriture de nouvelles données après compact
        buffer.put("NIO".getBytes(StandardCharsets.UTF_8));
        printBufferState("Après put 'NIO' (devrait être 'vaNIO')", buffer);

        System.out.println("\n--- Lecture finale après compact et nouvelle écriture ---");
        buffer.flip();
        byte[] finalBytes = new byte[buffer.remaining()];
        buffer.get(finalBytes);
        System.out.println("Contenu final lu: " + new String(finalBytes, StandardCharsets.UTF_8));
        printBufferState("Après lecture finale", buffer);
    }

    private static void printBufferState(String description, ByteBuffer buffer) {
        System.out.printf("%40s | Position: %d, Limit: %d, Capacity: %d, Contenu (UTF-8): '%s'%n",
                          description,
                          buffer.position(),
                          buffer.limit(),
                          buffer.capacity(),
                          new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8));
    }
}
