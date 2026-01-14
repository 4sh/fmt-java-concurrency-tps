# Guide Formateur - TP1

## 📦 Contenu du TP1

```
tp1/
├── README.md                    ← Instructions complètes pour les stagiaires
├── GUIDE_FORMATEUR.md           ← Ce fichier
├── LoadClient.java              ← Client de charge Netty (complet)
├── NaiveServer.java             ← Serveur à compléter par les stagiaires
└── solution/
    └── NaiveServerSolution.java ← Corrigé complet
```

## 🎯 Objectifs pédagogiques

1. Comprendre l'approche thread-per-connection
2. Observer les limites de scalabilité
3. Mesurer le coût réel d'un thread
4. Préparer la transition vers Netty (TP2)

## 🚀 Démarrage rapide

### Option 1 : Les stagiaires complètent le serveur (recommandé)

Les stagiaires travaillent sur `NaiveServer.java` qui contient des TODO clairs.

**Actuellement, le serveur :**
- ✅ Accepte les connexions
- ✅ Crée un thread par connexion
- ❌ Ne lit PAS les messages (TODO 3)
- ❌ Ne renvoie PAS l'écho

**Ce que les stagiaires doivent faire :**
Décommenter et compléter le code dans TODO 3 :
```java
String message;
while ((message = in.readLine()) != null) {
    if (!message.isEmpty()) {
        out.println(message);
    }
}
```

### Option 2 : Démonstration directe avec le corrigé

Utilisez `NaiveServerSolution.java` pour une démonstration immédiate.

## 🧪 Commandes Gradle

### Compiler le projet
```bash
./gradlew build
```

### Lancer le serveur (squelette - pour que les stagiaires complètent)
```bash
./gradlew run --args="fr.qsh.fmt.java.network.step1.BlockingServer"
```

### Lancer le serveur corrigé (pour démo formateur)
```bash
./gradlew run --args="fr.qsh.fmt.java.network.step1.BlockingServerSolution"
```

### Lancer le client de charge
```bash
# Configuration par défaut : 100 connexions vers localhost:5042
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient"

# Charge moyenne : 500 connexions
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 500 1000"

# Charge élevée : 2000 connexions
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 2000 1000"

# Charge extrême : 5000 connexions (pour démontrer l'échec)
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 5000 1000"
```

**Format des arguments du client :**
```
LoadClient <host> <port> <nb_connexions> <intervalle_envoi_ms>
```

## 📊 Scénarios de test recommandés

### Scénario 1 : Tout fonctionne bien (100 connexions)
```bash
# Terminal 1
./gradlew run --args="fr.qsh.fmt.java.network.step1.BlockingServerSolution"

# Terminal 2
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 100 1000"
```

**Observation attendue :**
- ✅ ~100 threads créés
- ✅ Mémoire : ~100 MB pour les threads
- ✅ Messages envoyés = messages reçus
- ✅ Latence faible

### Scénario 2 : Ça commence à ralentir (1000 connexions)
```bash
# Client avec 1000 connexions
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 1000 1000"
```

**Observation attendue :**
- ⚠️ ~1000 threads créés
- ⚠️ Mémoire : ~1 GB
- ⚠️ CPU élevé (context switching)
- ⚠️ Latence qui augmente

**Questions à poser aux stagiaires :**
- Combien de threads sont créés ?
- Combien de mémoire est utilisée ?
- Que se passe-t-il si on continue d'augmenter le nombre de connexions ?

### Scénario 3 : Le système souffre (5000+ connexions)
```bash
# Client avec 5000 connexions
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5042 5000 1000"
```

**Observation attendue :**
- ❌ Le système devient très lent
- ❌ Possible : timeouts, connexions refusées
- ❌ Possible : `OutOfMemoryError: unable to create new native thread`
- ❌ CPU à 100% (context switching)

## 🎓 Points clés à faire passer en formation

### 1. Pourquoi un thread par connexion ?
**Réponse :** Parce que `readLine()` et `println()` sont **bloquants**. Sans threads séparés, le serveur ne pourrait gérer qu'un seul client à la fois.

### 2. Quel est le coût d'un thread ?
**Réponse :**
- **Mémoire** : ~1 MB de stack par thread
- **CPU** : Context switching entre threads
- **Limite OS** : ~32000 threads max sur Linux, ~2000 sur Windows

### 3. Pourquoi les connexions restent-elles ouvertes ?
**Réponse :** Pour simuler un scénario réaliste (WebSocket, chat, streaming). Le vrai coût vient du maintien de milliers de connexions, pas de l'acceptation/fermeture.

### 4. Quelle est la solution ?
**Réponse :** **I/O non bloquant + Event Loop** (c'est ce que fait Netty dans le TP2)

## 🔧 Dépannage

### ⚠️ "Too many open files" - PROBLÈME FRÉQUENT

**Symptômes :**
```
java.io.FileNotFoundException: ... (Too many open files)
```

**Cause :** Chaque socket TCP = 1 descripteur de fichier. Le système d'exploitation limite le nombre de descripteurs qu'un processus peut ouvrir (par défaut ~256-1024 sur macOS).

**Solution rapide (pour la formation) :**

```bash
# 1. Vérifier la limite actuelle
ulimit -n
# → Probablement 256, 1024 ou 4096

# 2. Augmenter TEMPORAIREMENT la limite pour la session en cours
ulimit -n 65536

# 3. Relancer le serveur
./gradlew run --args="fr.qsh.fmt.java.network.step3.NettyServerSolution"
```

**Solution permanente macOS :**

Créer le fichier `/Library/LaunchDaemons/limit.maxfiles.plist` :
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
         "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
  <dict>
    <key>Label</key>
    <string>limit.maxfiles</string>
    <key>ProgramArguments</key>
    <array>
      <string>launchctl</string>
      <string>limit</string>
      <string>maxfiles</string>
      <string>65536</string>
      <string>200000</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
  </dict>
</plist>
```

Puis charger :
```bash
sudo launchctl load -w /Library/LaunchDaemons/limit.maxfiles.plist
```

**Solution Linux :**
```bash
# Temporaire
ulimit -n 65536

# Permanent : éditer /etc/security/limits.conf
* soft nofile 65536
* hard nofile 65536
```

**💡 Point pédagogique IMPORTANT :**

Cette erreur est **excellente pour la formation** ! Elle démontre que :
1. Même avec du code scalable (Netty, NIO, Loom), on peut atteindre des limites système
2. Chaque socket = 1 descripteur de fichier (limite OS)
3. En production, la scalabilité dépend du code **ET** de la configuration système
4. Il faut dimensionner les ressources selon la charge attendue

**Valeurs recommandées avec `ulimit -n 65536` :**

| Step | Test Faible | Test Moyen | Test Élevé | Max Raisonnable |
|------|-------------|------------|------------|-----------------|
| Step 1 (Naïf) | 100 | 500 | 1000 | 1000 (limite thread) |
| Step 2 (NIO) | 1000 | 5000 | 10000 | 50000 |
| Step 3 (Netty) | 5000 | 10000 | 20000 | 50000 |
| Step 4 (Loom) | 5000 | 10000 | 20000 | 50000 |

### Le serveur n'accepte pas de connexions
Vérifiez que le port n'est pas déjà utilisé :
```bash
# macOS/Linux
lsof -i :5042

# Ou pour tous les ports du TP
lsof -i :5042 -i :5043 -i :5044 -i :5045
```

### Le client ne peut pas se connecter
Assurez-vous que le serveur est lancé AVANT le client.

### OutOfMemoryError sur le serveur (Step 1 uniquement)
C'est normal avec 5000+ connexions sur Step 1 ! C'est justement ce qu'on veut démontrer.
Pour augmenter la limite (si vraiment nécessaire), modifiez `app/build.gradle.kts` :
```kotlin
application {
    applicationDefaultJvmArgs = listOf("-Xmx2g", "-Xss512k")
}
```

### Le client ne reçoit rien
Vérifiez que les stagiaires ont bien **implémenté la boucle de lecture/écriture** dans le serveur (TODO 3).

## 📈 Métriques à surveiller pendant la démo

### Côté serveur
```
[Stats] Connexions actives: 1000 | Total acceptées: 1000 | Threads actifs: 1005
```
- **Connexions actives** : nombre de clients connectés
- **Threads actifs** : doit être ≈ connexions + quelques threads système

### Côté client
```
[Stats] Connexions: 1000 | Envoyés: 50000 | Reçus: 50000
```
- **Envoyés** : nombre total de messages envoyés
- **Reçus** : nombre total de messages reçus (doit être égal si tout fonctionne)

### Sur la machine (optionnel)
```bash
# CPU usage
top -pid $(pgrep -f NaiveServer)

# Mémoire
jcmd <PID> VM.native_memory summary

# Nombre de threads
jstack <PID> | grep "java.lang.Thread" | wc -l
```

## 🎤 Exemple de narration pour la formation

1. **Introduction (5 min)**
   > "Aujourd'hui, nous allons voir pourquoi l'approche thread-per-connection ne passe pas à l'échelle. On va créer un serveur naïf qui crée un thread par connexion, et on va le mettre sous charge."

2. **Implémentation (15 min)**
   > "Regardez le serveur. Pour chaque connexion, on crée un thread. C'est simple, mais est-ce scalable ? À vous de compléter la logique de lecture/écriture."

3. **Test avec 100 connexions (5 min)**
   > "Avec 100 connexions, tout va bien. 100 threads, c'est gérable."

4. **Test avec 1000 connexions (5 min)**
   > "Avec 1000 connexions, on commence à voir les limites. 1 GB de mémoire rien que pour les threads. Le CPU travaille beaucoup pour gérer le context switching."

5. **Test avec 5000 connexions (5 min)**
   > "Avec 5000 connexions, le système souffre. On atteint les limites du modèle thread-per-connection. C'est pour ça que Netty existe."

6. **Conclusion (5 min)**
   > "Le problème : les I/O bloquants + un thread par connexion. La solution : I/O non bloquants + event loop. C'est ce qu'on va voir dans le TP2."

## 📚 Ressources complémentaires

- [The C10K Problem](http://www.kegel.com/c10k.html)
- [Java NIO Tutorial](https://jenkov.com/tutorials/java-nio/index.html)
- [Netty User Guide](https://netty.io/wiki/user-guide.html)

## ✅ Checklist avant la session

- [ ] **Configuration système** : J'ai augmenté la limite de fichiers ouverts : `ulimit -n 65536`
- [ ] Le projet compile : `./gradlew build`
- [ ] Le serveur démarre : `./gradlew run --args="fr.qsh.fmt.java.network.step1.BlockingServerSolution"`
- [ ] Le client se connecte : `./gradlew run --args="fr.qsh.fmt.java.network.LoadClient"`
- [ ] J'ai testé avec 100, 1000 et 5000 connexions
- [ ] Je sais expliquer l'erreur "Too many open files" si elle survient
- [ ] J'ai préparé les questions de réflexion
- [ ] J'ai lu le fichier `CONFIGURATION_SYSTEME.md` (optionnel mais recommandé)

---

**Durée estimée du TP complet (4 steps) :** 2h30 - 3h00
- Step 1 : 40 min
- Step 2 : 50 min
- Step 3 : 40 min
- Step 4 : 30 min
- Synthèse : 20 min
