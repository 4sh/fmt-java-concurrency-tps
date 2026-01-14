# TP1 - Serveur TCP Naïf : Thread-per-Connection

## 🎯 Objectifs pédagogiques

Ce TP permet de comprendre :
- L'approche classique **thread-per-connection** pour un serveur TCP
- Les **limites de scalabilité** de cette approche
- Le coût réel d'un thread (mémoire, context switching)
- Pourquoi des frameworks comme **Netty** sont nécessaires pour gérer des milliers de connexions

## 📋 Contexte

Dans l'approche "naïve" traditionnelle, chaque connexion cliente est gérée par un thread dédié :
```
Client 1 → Thread 1
Client 2 → Thread 2
...
Client N → Thread N
```

Cette approche simple fonctionne bien pour quelques dizaines de clients, mais **ne passe pas à l'échelle** :
- Chaque thread consomme ~1 MB de mémoire (stack)
- Le scheduler CPU doit gérer des milliers de threads
- Le context switching devient coûteux
- Les I/O bloquants gaspillent des ressources

## 📂 Structure du TP

```
tp1/
├── README.md                    ← Vous êtes ici
├── LoadClient.java              ← Client de charge (déjà complet)
├── NaiveServer.java             ← Serveur à compléter (⚠️ À FAIRE)
└── solution/
    └── NaiveServerSolution.java ← Corrigé (ne pas regarder avant d'avoir essayé !)
```

## 🚀 Étape 1 : Découverte du client de charge

Le client `LoadClient.java` est un outil Netty qui permet de simuler une charge réaliste :
- Ouvre **N connexions TCP en parallèle**
- Maintient les connexions **ouvertes** (pas de connect/disconnect répété)
- Envoie des messages **périodiquement** sur chaque connexion
- Affiche des **statistiques** en temps réel

### Lancer le client de charge

```bash
# Compiler le projet
./gradlew build

# Lancer le client avec la configuration par défaut (100 connexions vers localhost:5042)
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient"

# Lancer avec des paramètres personnalisés
# Format : <host> <port> <nb_connexions> <intervalle_ms>
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 500 1000"
```

**⚠️ Le client échouera tant que le serveur n'est pas lancé !**

## 🛠️ Étape 2 : Compléter le serveur naïf

Ouvrez le fichier `NaiveServer.java` et complétez les sections marquées par `TODO` :

### TODO 1 : Créer un thread par connexion
```java
// Accepter une nouvelle connexion
Socket clientSocket = serverSocket.accept();

// TODO : Créer et démarrer un nouveau thread pour gérer cette connexion
// Indice : new Thread(() -> handleClient(clientSocket, connId)).start();
```

### TODO 2 : Créer les flux d'entrée/sortie
```java
// TODO : Créer un BufferedReader pour lire les données du client
// BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

// TODO : Créer un PrintWriter pour écrire au client
// PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
```

### TODO 3 : Boucle de lecture/écriture
```java
String message;
while ((message = in.readLine()) != null) {
    // Renvoyer le message en écho
    out.println(message);
}
```

### TODO 4 : Fermer proprement les ressources
```java
finally {
    activeConnections.decrementAndGet();
    try {
        if (!clientSocket.isClosed()) {
            clientSocket.close();
        }
    } catch (IOException e) {
        // ...
    }
}
```

## 🧪 Étape 3 : Tester la scalabilité

Une fois le serveur complété :

### Test 1 : Charge faible (100 connexions)
```bash
# Terminal 1 : Lancer le serveur
./gradlew run --args="fr.qsh.fmt.java.network.step1.BlockingServer"

# Terminal 2 : Lancer le client avec 100 connexions
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 100 1000"
```

**Observation attendue** :
- ✓ Tout fonctionne bien
- ~100 threads créés
- Consommation mémoire raisonnable

### Test 2 : Charge moyenne (1000 connexions)
```bash
# Client avec 1000 connexions
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 1000 1000"
```

**Observation attendue** :
- ⚠️ ~1000 threads créés
- Consommation mémoire : ~1 GB (1000 threads × 1 MB)
- Le serveur répond mais plus lentement

### Test 3 : Charge élevée (5000+ connexions)
```bash
# Client avec 5000 connexions
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 5000 1000"
```

**Observation attendue** :
- ❌ Le système souffre (CPU élevé, latence importante)
- Possible : timeouts, connexions refusées, OutOfMemoryError
- Le thread-per-connection ne passe **pas à l'échelle**

## 📊 Comprendre les statistiques

Le serveur affiche toutes les 5 secondes :
```
[Stats] Connexions actives: 1000 | Total acceptées: 1000 | Threads actifs: 1005
```

Le client affiche :
```
[Stats] Connexions: 1000 | Envoyés: 50000 | Reçus: 50000
```

### Que surveiller ?
- **Nombre de threads** : Doit être proche du nombre de connexions (+ quelques threads systèmes)
- **Mémoire utilisée** : Augmente linéairement avec le nombre de threads
- **CPU** : Le context switching devient coûteux au-delà de quelques milliers de threads
- **Latence** : Temps entre envoi et réception d'un message

## 🤔 Questions de réflexion

1. **Pourquoi crée-t-on un thread par connexion dans ce modèle ?**
   <details>
   <summary>Réponse</summary>
   Parce que les opérations `readLine()` et `println()` sont **bloquantes**. Si on utilisait un seul thread, il serait bloqué sur la première connexion et ne pourrait pas gérer les autres.
   </details>

2. **Quelle est la limite théorique de ce serveur ?**
   <details>
   <summary>Réponse</summary>
   - Limite OS du nombre de threads (souvent ~32000 sur Linux, ~2000 sur Windows)
   - Limite mémoire : avec 8 GB RAM et 1 MB par thread → ~8000 threads max
   - Limite pratique : au-delà de 1000-2000 threads, le context switching dégrade les performances
   </details>

3. **Que se passe-t-il si un client envoie des données très lentement ?**
   <details>
   <summary>Réponse</summary>
   Le thread reste bloqué en lecture (`readLine()`), consommant des ressources sans rien faire d'utile. C'est un **gaspillage** de thread.
   </details>

4. **Pourquoi les connexions doivent-elles rester ouvertes pour un test réaliste ?**
   <details>
   <summary>Réponse</summary>
   Dans la vraie vie (WebSocket, chat, streaming), les connexions restent ouvertes longtemps. Si on testait seulement avec des connexions éphémères, on testerait uniquement la capacité à accepter/fermer rapidement, pas la scalabilité à maintenir des milliers de connexions simultanées.
   </details>

## 🎓 Phrase clé de la formation

> **"Pour tester la scalabilité d'un serveur, il faut des connexions nombreuses et durables, pas juste des connexions éphémères."**

Le vrai coût d'un serveur vient de sa capacité à **maintenir** des milliers de sockets ouverts, pas seulement à les accepter.

## 🔑 Points clés à retenir

1. **Thread-per-connection est simple mais non scalable**
   - Fonctionne jusqu'à ~1000 connexions
   - Au-delà, le coût mémoire et CPU devient prohibitif

2. **Un thread bloqué = ressources gaspillées**
   - Un thread en attente de I/O consomme de la mémoire sans rien faire

3. **La solution : I/O non bloquant + Event Loop**
   - C'est ce que fait Netty (TP2)
   - Au lieu de N threads pour N connexions, on utilise quelques threads (typiquement nb_cores × 2)

## 📚 Pour aller plus loin

- **C10K problem** : https://en.wikipedia.org/wiki/C10k_problem
- **Java NIO (New I/O)** : API Java pour les I/O non bloquants
- **Netty** : Framework asynchrone basé sur NIO

## ✅ Checklist avant de passer au TP2

- [ ] J'ai complété les 4 TODO du serveur
- [ ] J'ai testé avec 100, 1000 et 5000 connexions
- [ ] J'ai observé la dégradation des performances
- [ ] Je comprends pourquoi thread-per-connection ne passe pas à l'échelle
- [ ] Je suis curieux de voir comment Netty résout ce problème (TP2 !)

---

**Prochaine étape** : TP2 - Serveur Netty basique (à venir)
