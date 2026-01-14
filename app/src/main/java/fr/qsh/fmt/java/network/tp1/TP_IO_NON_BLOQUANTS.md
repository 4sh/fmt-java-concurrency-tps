# TP : I/O Non Bloquants - 4 Approches Comparées

## 🎯 Objectif Global

Ce TP démontre **4 approches différentes** pour implémenter un serveur TCP scalable capable de gérer des milliers de connexions simultanées. Chaque step illustre une technique et ses compromis.

## 📊 Vue d'ensemble des 4 Steps

| Step | Approche | Technologie | Scalabilité | Complexité | Port |
|------|----------|-------------|-------------|------------|------|
| **1** | Thread-per-connection | Platform Threads | ❌ ~1000 max | ⭐ Simple | 5042 |
| **2** | I/O non-bloquant | Java NIO (Selector) | ✅ 10k+ | ⭐⭐⭐ Complexe | 5043 |
| **3** | Event Loop | Netty | ✅ 10k+ | ⭐⭐ Modéré | 5044 |
| **4** | Virtual Threads | Java 21 Loom | ✅ 10k+ | ⭐ Simple | 5045 |

## 📂 Structure du TP

```
tp1/
├── TP_IO_NON_BLOQUANTS.md      ← Ce fichier (vue d'ensemble)
├── README.md                    ← Instructions détaillées Step 1
├── GUIDE_FORMATEUR.md           ← Guide formateur avec commandes
├── LoadClient.java              ← Client de charge (partagé par toutes les steps)
│
├── Step 1 : Thread-per-connection (Naïf)
│   ├── NaiveServer.java         ← Squelette à compléter
│   └── solution/
│       └── NaiveServerSolution.java
│
├── step2/ : Java NIO (Selector)
│   ├── NioServer.java           ← Squelette à compléter
│   └── solution/
│       └── NioServerSolution.java
│
├── step3/ : Netty (Event Loop)
│   ├── NettyServer.java         ← Squelette à compléter
│   └── solution/
│       └── NettyServerSolution.java
│
└── step4/ : Loom (Virtual Threads)
    ├── LoomServer.java          ← Squelette à compléter
    └── solution/
        └── LoomServerSolution.java
```

## 🎓 Démarche Pédagogique

### Ordre recommandé

1. **Step 1** : Comprendre les limites du modèle thread-per-connection
2. **Step 2** : Découvrir Java NIO et comprendre la complexité du bas niveau
3. **Step 3** : Apprécier l'abstraction élégante de Netty
4. **Step 4** : Découvrir Loom qui combine simplicité ET performance

### Phrase clé

> **"Pour tester la scalabilité d'un serveur, il faut des connexions nombreuses et durables, pas juste des connexions éphémères."**

## 🚀 Guide de Démarrage Rapide

### Prérequis

- Java 21+ (pour Step 4 Loom)
- Gradle

### Compiler le projet

```bash
./gradlew build
```

### Client de charge (partagé)

Le `LoadClient` est utilisé par toutes les steps. Il simule N connexions simultanées, maintient ces connexions ouvertes, et envoie périodiquement des messages.

```bash
# Format : LoadClient <host> <port> <nb_connexions> <intervalle_ms>

# Step 1 : Serveur naïf (port 5042)
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 100 1000"

# Step 2 : Serveur NIO (port 5043)
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5043 1000 1000"

# Step 3 : Serveur Netty (port 5044)
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5044 5000 1000"

# Step 4 : Serveur Loom (port 5045)
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5045 10000 1000"
```

---

## 📖 Step 1 : Thread-per-Connection (Naïf)

### Principe

Un thread plateforme par connexion. Les I/O sont **bloquants**.

```java
while (true) {
    Socket client = serverSocket.accept();
    new Thread(() -> handleClient(client)).start();  // ⚠️ Problème !
}
```

### Limites

- **Mémoire** : ~1 MB par thread
- **CPU** : Context switching coûteux au-delà de ~1000 threads
- **Scalabilité** : max ~1000-2000 connexions

### Commandes

```bash
# Lancer le serveur
./gradlew run --args="fr.qsh.fmt.java.network.step1.BlockingServerSolution"

# Tester avec 100 connexions : ✅ OK
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 100 1000"

# Tester avec 1000 connexions : ⚠️ Lent
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 1000 1000"

# Tester avec 5000 connexions : ❌ Le système souffre
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 5000 1000"
```

### À retenir

- Simple à implémenter
- Ne passe **pas** à l'échelle
- Démontre le besoin d'une autre approche

---

## 📖 Step 2 : Java NIO (Selector)

### Principe

Un seul thread multiplexe tous les canaux grâce à un **Selector**. Les I/O sont **non-bloquants**.

```java
Selector selector = Selector.open();
serverChannel.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();  // Attend des événements
    for (SelectionKey key : selector.selectedKeys()) {
        if (key.isAcceptable()) handleAccept(key);
        else if (key.isReadable()) handleRead(key);
        else if (key.isWritable()) handleWrite(key);
    }
}
```

### Avantages

- ✅ Scalabilité excellente (10k+ connexions)
- ✅ Peu de threads
- ✅ Performance élevée

### Inconvénients

- ❌ Code complexe et verbeux
- ❌ Gestion manuelle des buffers
- ❌ Facile de faire des erreurs

### Commandes

```bash
# Lancer le serveur
./gradlew run --args="fr.qsh.fmt.java.network.step2.NioServerSolution"

# Tester avec 1000 connexions : ✅ Excellent
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5043 1000 1000"

# Tester avec 5000 connexions : ✅ Toujours performant
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5043 5000 1000"
```

### À retenir

- Puissant mais difficile à utiliser correctement
- Base de Netty et autres frameworks asynchrones
- Rarement utilisé directement en production

---

## 📖 Step 3 : Netty (Event Loop)

### Principe

Netty abstrait Java NIO avec une architecture **Event Loop** + **Pipeline**. Quelques threads (typiquement 2 × nb_cores) suffisent.

```java
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
EventLoopGroup workerGroup = new NioEventLoopGroup();

ServerBootstrap bootstrap = new ServerBootstrap();
bootstrap.group(bossGroup, workerGroup)
    .channel(NioServerSocketChannel.class)
    .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) {
            ch.pipeline()
                .addLast(new LineBasedFrameDecoder(1024))
                .addLast(new StringDecoder())
                .addLast(new StringEncoder())
                .addLast(new EchoHandler());  // Handler applicatif
        }
    });
```

### Avantages

- ✅ Scalabilité excellente (10k+ connexions)
- ✅ Code élégant et maintenable
- ✅ Encoders/Decoders prêts à l'emploi
- ✅ Performance optimisée
- ✅ Utilisé en production (gRPC, Kafka, Elasticsearch, etc.)

### Inconvénients

- ⚠️ Courbe d'apprentissage
- ⚠️ Dépendance externe

### Commandes

```bash
# Lancer le serveur
./gradlew run --args="fr.qsh.fmt.java.network.step3.NettyServerSolution"

# Tester avec 5000 connexions : ✅ Excellent
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5044 5000 1000"

# Tester avec 10000 connexions : ✅ Toujours performant
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5044 10000 1000"
```

### À retenir

- **Standard de facto** pour les serveurs haute performance en Java
- Abstraction élégante au-dessus de Java NIO
- Utilisé massivement en production

---

## 📖 Step 4 : Loom (Virtual Threads)

### Principe

Les **Virtual Threads** (Java 21+) permettent de revenir à un style simple "thread-per-connection", mais sans les limitations des threads plateforme. Les virtual threads sont ultra-légers (~1 KB vs ~1 MB).

```java
while (true) {
    Socket client = serverSocket.accept();
    Thread.startVirtualThread(() -> handleClient(client));  // ✅ Scalable !
}
```

### Avantages

- ✅ Code simple et synchrone (comme Step 1)
- ✅ Scalabilité excellente (millions de virtual threads possibles)
- ✅ Les I/O bloquants sont automatiquement optimisés par la JVM
- ✅ Pas de callback hell, pas de CompletableFuture complexes
- ✅ Pas de dépendance externe

### Inconvénients

- ⚠️ Nécessite Java 21+
- ⚠️ Nouveau paradigme (moins mature que Netty)

### Commandes

```bash
# Lancer le serveur (nécessite Java 21+)
./gradlew run --args="fr.qsh.fmt.java.network.step4.LoomServerSolution"

# Tester avec 5000 connexions : ✅ Excellent
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5045 5000 1000"

# Tester avec 10000 connexions : ✅ Toujours performant
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5045 10000 1000"
```

### À retenir

- **Game changer** pour Java
- Combine simplicité de Step 1 et performance de Step 2/3
- L'avenir du développement concurrent en Java

---

## 📊 Tableau Comparatif Final

| Critère | Step 1 | Step 2 | Step 3 | Step 4 |
|---------|--------|--------|--------|--------|
| **Simplicité du code** | ⭐⭐⭐ | ⭐ | ⭐⭐ | ⭐⭐⭐ |
| **Scalabilité** | ❌ ~1k | ✅ 10k+ | ✅ 10k+ | ✅ 10k+ |
| **Consommation mémoire** | ❌ Élevée | ✅ Faible | ✅ Faible | ✅ Très faible |
| **Nb de threads** | N (1 par conn.) | 1 | ~2×cores | ~cores |
| **Complexité** | Faible | Élevée | Moyenne | Faible |
| **Maturité** | ✅ | ✅ | ✅ | ⚠️ Nouveau |
| **Production ready** | ❌ | ⚠️ | ✅ | ✅ (Java 21+) |

## ⚠️ Problème Fréquent : "Too many open files"

### Symptôme

Lors des tests avec un grand nombre de connexions (5000+), vous pourriez rencontrer :

```
java.io.FileNotFoundException: ... (Too many open files)
Caused by: java.lang.Error: java.io.FileNotFoundException:
    /path/to/jdk/lib/tzdb.dat (Too many open files)
```

### Explication

- **Chaque socket TCP = 1 descripteur de fichier**
- Le système d'exploitation limite le nombre de descripteurs qu'un processus peut ouvrir
- Par défaut sur macOS : ~256-1024
- Par défaut sur Linux : ~1024-4096

C'est une **limite système**, pas une limite du code !

### Solution Rapide

```bash
# 1. Vérifier la limite actuelle
ulimit -n

# 2. Augmenter temporairement (pour la session en cours)
ulimit -n 65536

# 3. Relancer votre serveur
```

### Valeurs Recommandées

Avec `ulimit -n 65536`, vous pouvez tester :

| Step | Test Recommandé | Maximum Safe |
|------|-----------------|--------------|
| Step 1 (Naïf) | 100-1000 | 1000 |
| Step 2 (NIO) | 1000-10000 | 50000 |
| Step 3 (Netty) | 5000-20000 | 50000 |
| Step 4 (Loom) | 5000-20000 | 50000 |

### Point Pédagogique Important

Cette limitation illustre un point crucial :

> **La scalabilité en production ne dépend pas uniquement du code, mais aussi de la configuration système.**

Même avec les meilleures approches (Netty, NIO, Loom), il faut dimensionner les ressources :
- Limites de descripteurs de fichiers
- Mémoire disponible
- Bande passante réseau
- Nombre de CPU cores

## 🤔 Questions de Réflexion

### Après Step 1
- Pourquoi crée-t-on un thread par connexion ?
- Quelle est la limite théorique de cette approche ?
- Que se passe-t-il si un client envoie des données lentement ?

### Après Step 2
- Comment un seul thread peut-il gérer des milliers de connexions ?
- Pourquoi Java NIO est-il rarement utilisé directement ?
- Qu'est-ce qu'un Selector ?

### Après Step 3
- En quoi Netty simplifie-t-il Java NIO ?
- Qu'est-ce qu'un Event Loop ?
- Pourquoi Netty est-il utilisé par gRPC, Kafka, etc. ?

### Après Step 4
- Comment les Virtual Threads peuvent-ils être si légers ?
- Pourquoi peut-on revenir à un style synchrone avec Loom ?
- Loom va-t-il remplacer Netty ?

### Question bonus (limites système)
- Pourquoi même un serveur Netty/Loom peut rencontrer "Too many open files" ?
- Quelles autres limites système faut-il configurer en production ?

## 🎯 Quel modèle choisir en production ?

| Situation | Recommandation |
|-----------|----------------|
| **Projet greenfield Java 21+** | **Step 4 (Loom)** - Simple et performant |
| **Besoin de performance extrême** | **Step 3 (Netty)** - Mature et optimisé |
| **Serveur HTTP/gRPC** | **Step 3 (Netty)** - Frameworks basés dessus |
| **Application legacy** | **Step 3 (Netty)** ou migration vers Loom |
| **Formation/apprentissage** | **Toutes les steps !** - Comprendre les compromis |

## 📚 Ressources Complémentaires

- [The C10K Problem](http://www.kegel.com/c10k.html)
- [Java NIO Tutorial](https://jenkov.com/tutorials/java-nio/index.html)
- [Netty User Guide](https://netty.io/wiki/user-guide.html)
- [Project Loom](https://openjdk.org/projects/loom/)
- [JEP 425: Virtual Threads](https://openjdk.org/jeps/425)

## ✅ Checklist Complète du TP

### Préparation
- [ ] **IMPORTANT** : J'ai augmenté la limite de fichiers ouverts : `ulimit -n 65536`
- [ ] J'ai lu la section "Too many open files" ci-dessus

### Step 1 - Thread-per-connection
- [ ] J'ai complété le serveur naïf
- [ ] J'ai observé les limites avec 1000+ connexions
- [ ] Je comprends pourquoi cette approche ne passe pas à l'échelle

### Step 2 - Java NIO
- [ ] J'ai complété le serveur NIO
- [ ] J'ai compris le rôle du Selector
- [ ] J'ai observé qu'un seul thread suffit

### Step 3 - Netty
- [ ] J'ai complété le serveur Netty
- [ ] J'ai compris l'architecture Event Loop + Pipeline
- [ ] J'ai apprécié l'abstraction élégante par rapport à NIO brut

### Step 4 - Loom (Virtual Threads)
- [ ] J'ai complété le serveur Loom (nécessite Java 21+)
- [ ] J'ai compris les Virtual Threads
- [ ] J'ai constaté qu'on peut revenir à un style simple ET scalable

### Synthèse
- [ ] J'ai comparé les 4 approches
- [ ] Je sais quel modèle choisir selon le contexte
- [ ] Je comprends que la scalabilité dépend du code ET de la configuration système

---

**Durée totale estimée :** 2h30 - 3h00
- Step 1 : 40 min
- Step 2 : 50 min
- Step 3 : 40 min
- Step 4 : 30 min
- Synthèse : 20 min
