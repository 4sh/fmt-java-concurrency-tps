# Configuration Système pour Haute Scalabilité

## 🎯 Objectif

Ce document explique comment configurer votre système pour tester des serveurs avec un grand nombre de connexions simultanées (10k+).

## 📊 Le Problème : "Too many open files"

### Symptôme

```
java.io.FileNotFoundException: ... (Too many open files)
Caused by: java.lang.Error: java.io.FileNotFoundException:
    /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/lib/tzdb.dat
    (Too many open files)
```

### Cause

- **1 socket TCP = 1 descripteur de fichier**
- Le système d'exploitation limite le nombre de descripteurs de fichiers ouverts par processus
- Quand vous testez avec 10000 connexions → besoin de ~10000 descripteurs de fichiers

### Limites par Défaut

| OS | Limite Soft (défaut) | Limite Hard (max) |
|----|---------------------|-------------------|
| **macOS** | 256-1024 | 10240-unlimited |
| **Linux** | 1024 | 4096-unlimited |
| **Windows** | Pas de limite ulimit | - |

## 🔧 Configuration macOS

### Solution Temporaire (Session Actuelle)

```bash
# Vérifier la limite actuelle
ulimit -n
# → Affiche probablement 256, 1024 ou 4096

# Vérifier la limite maximale autorisée
ulimit -Hn
# → Affiche la hard limit

# Augmenter la limite pour la session en cours
ulimit -n 65536

# Vérifier que c'est bien appliqué
ulimit -n
# → Devrait afficher 65536
```

**Important :** Cette modification n'est valable que pour le terminal actuel. Fermez le terminal = limite perdue.

### Solution Permanente (Système)

#### Méthode 1 : LaunchDaemon (Recommandé)

Créer le fichier `/Library/LaunchDaemons/limit.maxfiles.plist` avec les droits administrateur :

```bash
sudo nano /Library/LaunchDaemons/limit.maxfiles.plist
```

Contenu du fichier :

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
    <key>ServiceIPC</key>
    <false/>
  </dict>
</plist>
```

Activer :

```bash
# Définir les permissions correctes
sudo chown root:wheel /Library/LaunchDaemons/limit.maxfiles.plist
sudo chmod 644 /Library/LaunchDaemons/limit.maxfiles.plist

# Charger le daemon
sudo launchctl load -w /Library/LaunchDaemons/limit.maxfiles.plist

# Vérifier
launchctl limit maxfiles
# → Devrait afficher : maxfiles 65536 200000
```

**Redémarrer votre session** ou redémarrer le Mac pour que ce soit effectif.

#### Méthode 2 : Configuration du shell (Utilisateur)

Ajouter dans `~/.zshrc` (ou `~/.bash_profile` si vous utilisez bash) :

```bash
# Augmenter la limite de fichiers ouverts
ulimit -n 65536
```

Puis recharger :

```bash
source ~/.zshrc
```

## 🐧 Configuration Linux

### Solution Temporaire

```bash
# Vérifier la limite actuelle
ulimit -n

# Augmenter pour la session en cours
ulimit -n 65536
```

### Solution Permanente

#### Méthode 1 : /etc/security/limits.conf (Recommandé)

Éditer le fichier `/etc/security/limits.conf` :

```bash
sudo nano /etc/security/limits.conf
```

Ajouter ces lignes à la fin :

```
# Augmenter les limites de descripteurs de fichiers
* soft nofile 65536
* hard nofile 200000
root soft nofile 65536
root hard nofile 200000
```

#### Méthode 2 : systemd (pour services)

Si vous déployez votre serveur en tant que service systemd :

```ini
[Service]
LimitNOFILE=65536
```

#### Méthode 3 : sysctl (Kernel)

Pour une limite globale système :

```bash
# Temporaire
sudo sysctl -w fs.file-max=200000

# Permanent : éditer /etc/sysctl.conf
echo "fs.file-max = 200000" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p
```

**Redémarrer votre session** pour que ce soit effectif.

## 🪟 Configuration Windows

Windows ne limite pas les descripteurs de fichiers de la même manière. Cependant, vous pourriez rencontrer d'autres limites.

### Limites Réseau Windows

```powershell
# Augmenter le nombre de connexions TCP simultanées
netsh int ipv4 set dynamicport tcp start=1025 num=64511
netsh int ipv6 set dynamicport tcp start=1025 num=64511

# Réduire le temps de TIME_WAIT
reg add "HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters" /v TcpTimedWaitDelay /t REG_DWORD /d 30 /f
```

## ✅ Vérification de la Configuration

### macOS / Linux

```bash
# Vérifier la limite actuelle
ulimit -n

# Vérifier la limite système globale (Linux uniquement)
cat /proc/sys/fs/file-max

# Tester avec un serveur
./gradlew run --args="fr.qsh.fmt.java.network.step3.NettyServerSolution"

# Dans un autre terminal : client avec 20000 connexions
./gradlew run --args="fr.qsh.fmt.java.network.LoadClient localhost 5044 20000 1000"
```

### Surveiller l'utilisation

```bash
# macOS : nombre de fichiers ouverts par processus
lsof -p <PID> | wc -l

# Linux : même chose
lsof -p <PID> | wc -l

# Linux : limite actuelle du processus
cat /proc/<PID>/limits | grep "open files"
```

## 📈 Valeurs Recommandées par Contexte

### Pour la Formation

```bash
ulimit -n 65536
```

Permet de tester jusqu'à ~50000-60000 connexions.

### Pour le Développement

```bash
ulimit -n 10240
```

Suffisant pour la plupart des tests (jusqu'à ~8000-9000 connexions).

### Pour la Production

```bash
# Linux
* soft nofile 200000
* hard nofile 1000000
fs.file-max = 2000000

# macOS (serveurs de production rares)
ulimit -n 200000
```

Ajuster selon la charge attendue :
- Serveur web classique : 10k-50k
- Serveur de chat/WebSocket : 50k-500k
- Serveur de signalisation : 100k-1M+

## 🎓 Comprendre les Limites

### Soft Limit vs Hard Limit

- **Soft limit** : Limite actuelle, peut être augmentée jusqu'à la hard limit (sans droits root)
- **Hard limit** : Limite maximale, nécessite les droits root pour être modifiée

```bash
# Voir les deux limites
ulimit -Sn  # Soft
ulimit -Hn  # Hard
```

### Coût par Connexion

Chaque connexion TCP consomme :
- **1 descripteur de fichier** (socket)
- **~16 KB de buffers TCP** (réglable via SO_RCVBUF / SO_SNDBUF)
- **Mémoire applicative** (buffers, objets)

Pour 50000 connexions :
- 50000 descripteurs de fichiers
- ~800 MB de buffers TCP (16 KB × 50000)
- Variable selon l'application (buffers Netty, Virtual Threads, etc.)

## 🚨 Troubleshooting

### "Cannot increase limit: ulimit: value exceeds hard limit"

Votre hard limit est trop basse. Augmentez-la avec les droits root :

```bash
# macOS : modifier le LaunchDaemon
sudo launchctl limit maxfiles 65536 200000

# Linux : modifier /etc/security/limits.conf
```

### "Operation not permitted"

Vous essayez d'augmenter au-delà de la hard limit sans être root.

```bash
# Solution : augmenter en tant que root
sudo bash -c 'ulimit -n 200000 && su - votre_utilisateur'
```

### Les modifications ne persistent pas après redémarrage

Assurez-vous que les modifications sont bien dans les fichiers de configuration système (pas juste dans le shell).

## 📚 Ressources

- [The C10k Problem](http://www.kegel.com/c10k.html)
- [Linux man page: setrlimit](https://man7.org/linux/man-pages/man2/setrlimit.2.html)
- [macOS sysctl](https://support.apple.com/guide/mac-help/welcome/mac)
- [TCP Tuning](https://fasterdata.es.net/network-tuning/tcp-tuning/)

## ✅ Checklist Formation

Avant de commencer le TP avec un grand nombre de connexions :

- [ ] J'ai vérifié ma limite actuelle : `ulimit -n`
- [ ] J'ai augmenté la limite à 65536 : `ulimit -n 65536`
- [ ] J'ai relancé mon terminal si nécessaire
- [ ] Je peux maintenant tester avec 10k, 20k, voire 50k connexions !

---

**Note :** Si vous rencontrez toujours des problèmes après avoir configuré correctement, vérifiez aussi :
- La mémoire disponible (chaque connexion consomme de la RAM)
- Les limites réseau (bande passante, latence)
- Les paramètres TCP du kernel (`net.ipv4.tcp_max_syn_backlog`, etc.)
