# TP I/O Non Bloquants - Guide de Démarrage Rapide

## 🚀 Démarrage Ultra-Rapide (Scripts Automatisés)

Les scripts `run-server.sh` et `run-client.sh` configurent **automatiquement** la limite de fichiers ouverts.

### Lancer un Serveur

```bash
# Step 1 - Serveur Naïf (port 5042)
./run-server.sh step1

# Step 2 - Serveur Java NIO (port 5043)
./run-server.sh step2

# Step 3 - Serveur Netty (port 5044)
./run-server.sh step3

# Step 4 - Serveur Loom (port 5045)
./run-server.sh step4
```

### Lancer le Client de Charge

```bash
# Format: ./run-client.sh [host] [port] [connexions] [intervalle_ms]

# Exemples simples
./run-client.sh localhost 5042 1000      # 1000 connexions vers Step 1
./run-client.sh localhost 5044 10000     # 10000 connexions vers Step 3

# Exemple avancé
./run-client.sh localhost 5045 50000 2000  # 50000 connexions, ping toutes les 2s
```

### Exemple de Session Complète

```bash
# Terminal 1 : Lancer le serveur Netty
./run-server.sh step3

# Terminal 2 : Tester avec 10000 connexions
./run-client.sh localhost 5044 10000
```

**Les scripts gèrent automatiquement `ulimit -n 65536` pour vous !**

---

## 📖 Démarrage Manuel (sans scripts)

Si vous préférez lancer manuellement avec Gradle :

### 1. Configurer la limite (OBLIGATOIRE)

```bash
ulimit -n 65536
```

### 2. Lancer un serveur

```bash
# Step 1 - Serveur Naïf
./gradlew run --args="fr.qsh.fmt.java.network.step1.BlockingServerSolution"

# Step 2 - Serveur Java NIO
./gradlew run --args="fr.qsh.fmt.java.network.step2.NioServerSolution"

# Step 3 - Serveur Netty
./gradlew run --args="fr.qsh.fmt.java.network.step3.NettyServerSolution"

# Step 4 - Serveur Loom
./gradlew run --args="fr.qsh.fmt.java.network.step4.LoomServerSolution"
```

### 3. Lancer le client (autre terminal)

```bash
# Format: LoadClient <host> <port> <connexions> <intervalle_ms>

./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5044 10000 1000"
```

---

## 📂 Structure du Projet

```
fmt-java-concurrency-tps/
├── run-server.sh                    ← Script pour lancer les serveurs (avec ulimit auto)
├── run-client.sh                    ← Script pour lancer le client (avec ulimit auto)
├── README_TP.md                     ← Ce fichier
│
└── app/src/main/java/fr/qsh/fmt/java/network/tp1/
    │
    ├── 📄 Documentation
    │   ├── TP_IO_NON_BLOQUANTS.md   ← Vue d'ensemble (LIRE EN PREMIER)
    │   ├── README.md                ← Instructions Step 1
    │   ├── GUIDE_FORMATEUR.md       ← Guide formateur
    │   └── CONFIGURATION_SYSTEME.md ← Configuration ulimit détaillée
    │
    ├── LoadClient.java              ← Client de charge (partagé)
    │
    ├── Step 1 : Thread-per-connection (Port 5042)
    │   ├── NaiveServer.java
    │   └── solution/NaiveServerSolution.java
    │
    ├── Step 2 : Java NIO (Port 5043)
    │   ├── step2/NioServer.java
    │   └── step2/solution/NioServerSolution.java
    │
    ├── Step 3 : Netty (Port 5044)
    │   ├── step3/NettyServer.java
    │   └── step3/solution/NettyServerSolution.java
    │
    └── Step 4 : Loom (Port 5045)
        ├── step4/LoomServer.java
        └── step4/solution/LoomServerSolution.java
```

---

## 🎯 Ports et Steps

| Step | Technologie | Port | Scalabilité | Lancer avec |
|------|-------------|------|-------------|-------------|
| **1** | Platform Threads | 5042 | ~1000 | `./run-server.sh step1` |
| **2** | Java NIO | 5043 | 10k+ | `./run-server.sh step2` |
| **3** | Netty | 5044 | 10k+ | `./run-server.sh step3` |
| **4** | Loom | 5045 | 10k+ | `./run-server.sh step4` |

---

## ⚠️ Problème "Too many open files"

Si vous voyez cette erreur :

```
java.io.FileNotFoundException: ... (Too many open files)
```

**Solution :**

```bash
# Vérifier la limite actuelle
ulimit -n

# Augmenter la limite
ulimit -n 65536

# Ou utiliser les scripts (ils le font automatiquement)
./run-server.sh step3
```

**Les scripts `run-server.sh` et `run-client.sh` font cela automatiquement !**

Pour plus de détails, consultez : `app/src/main/java/fr/qsh/fmt/java/network/tp1/CONFIGURATION_SYSTEME.md`

---

## 📊 Valeurs de Test Recommandées

### Avec `ulimit -n 65536`

| Step | Test Léger | Test Moyen | Test Élevé |
|------|------------|------------|------------|
| Step 1 | 100 | 500 | 1000 |
| Step 2 | 1000 | 5000 | 10000 |
| Step 3 | 5000 | 10000 | 20000 |
| Step 4 | 5000 | 10000 | 20000 |

### Exemples de commandes

```bash
# Test léger Step 1
./run-client.sh localhost 5042 100

# Test moyen Step 2
./run-client.sh localhost 5043 5000

# Test élevé Step 3
./run-client.sh localhost 5044 20000

# Test extrême Step 4 (nécessite Java 21+)
./run-client.sh localhost 5045 50000
```

---

## 🎓 Pour les Stagiaires

1. **Lire d'abord** : `app/src/main/java/fr/qsh/fmt/java/network/tp1/TP_IO_NON_BLOQUANTS.md`
2. **Utiliser les scripts** : `./run-server.sh` et `./run-client.sh`
3. **Compléter les TODO** dans chaque step
4. **Comparer les performances** entre les 4 approches

---

## 🧑‍🏫 Pour les Formateurs

1. **Lire d'abord** : `app/src/main/java/fr/qsh/fmt/java/network/tp1/GUIDE_FORMATEUR.md`
2. **Avant la session** : Tester que les scripts fonctionnent
3. **Pendant la session** : Utiliser les scripts pour les démos
4. **Point pédagogique** : Expliquer pourquoi `ulimit` est nécessaire

---

## 🔧 Compilation

```bash
# Compiler le projet
./gradlew build

# Nettoyer et recompiler
./gradlew clean build
```

---

## ✅ Checklist Rapide

- [ ] J'ai cloné le projet
- [ ] J'ai compilé : `./gradlew build`
- [ ] J'ai rendu les scripts exécutables : `chmod +x run-*.sh` (normalement déjà fait)
- [ ] J'ai testé un serveur : `./run-server.sh step3`
- [ ] J'ai testé le client : `./run-client.sh localhost 5044 1000`
- [ ] Ça fonctionne ! 🎉

---

## 📚 Documentation Complète

- **Vue d'ensemble** : `app/src/main/java/fr/qsh/fmt/java/network/tp1/TP_IO_NON_BLOQUANTS.md`
- **Step 1 détaillée** : `app/src/main/java/fr/qsh/fmt/java/network/tp1/README.md`
- **Guide formateur** : `app/src/main/java/fr/qsh/fmt/java/network/tp1/GUIDE_FORMATEUR.md`
- **Configuration système** : `app/src/main/java/fr/qsh/fmt/java/network/tp1/CONFIGURATION_SYSTEME.md`

---

**Bon TP !** 🚀
