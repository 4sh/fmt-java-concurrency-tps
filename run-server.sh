#!/bin/bash

# Script pour lancer un serveur avec la limite de fichiers augmentée
# Usage: ./run-server.sh <nom_classe_serveur> [connexions_attendues]
# Exemples:
#   ./run-server.sh step1         # Lance le serveur Step 1 (limite par défaut: 10000)
#   ./run-server.sh step2 20000   # Lance le serveur Step 2 avec limite 20000
#   ./run-server.sh step3 50000   # Lance le serveur Step 3 avec limite 50000
#   ./run-server.sh step4         # Lance le serveur Step 4

set -e

# Couleurs pour l'affichage
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Valeurs par défaut
DEFAULT_CONNECTIONS="10000"

# Arguments
STEP=${1:-""}
CONNECTIONS=${2:-$DEFAULT_CONNECTIONS}

# Vérifier la limite actuelle
CURRENT_LIMIT=$(ulimit -n)
HARD_LIMIT=$(ulimit -Hn)
EXPECTED_LIMIT=${CONNECTIONS}

echo "=========================================="
echo "  Lanceur de Serveur - TP I/O Non Bloquants"
echo "=========================================="
echo ""

# Déterminer quelle classe lancer selon l'argument
case "$STEP" in
    "step1"|"1"|"naive")
        CLASS="fr.qsh.fmt.java.network.tp1.solution.NaiveServerSolution"
        DESCRIPTION="Step 1 - Serveur Naïf (Thread-per-connection)"
        PORT=5042
        ;;
    "step2"|"2"|"nio")
        CLASS="fr.qsh.fmt.java.network.tp1.step2.solution.NioServerSolution"
        DESCRIPTION="Step 2 - Serveur Java NIO (Selector)"
        PORT=5043
        ;;
    "step3"|"3"|"netty")
        CLASS="fr.qsh.fmt.java.network.tp1.step3.solution.NettyServerSolution"
        DESCRIPTION="Step 3 - Serveur Netty (Event Loop)"
        PORT=5044
        ;;
    "step4"|"4"|"loom")
        CLASS="fr.qsh.fmt.java.network.tp1.step4.solution.LoomServerSolution"
        DESCRIPTION="Step 4 - Serveur Loom (Virtual Threads)"
        PORT=5045
        ;;
    "")
        echo "Usage: $0 <step> [connexions_attendues]"
        echo ""
        echo "Steps disponibles:"
        echo "  step1 (ou 1, naive)  - Serveur Naïf (port 5042)"
        echo "  step2 (ou 2, nio)    - Serveur Java NIO (port 5043)"
        echo "  step3 (ou 3, netty)  - Serveur Netty (port 5044)"
        echo "  step4 (ou 4, loom)   - Serveur Loom (port 5045)"
        echo ""
        echo "Exemples:"
        echo "  $0 step1         # Limite par défaut: $DEFAULT_CONNECTIONS"
        echo "  $0 step3 50000   # Avec limite 50000"
        exit 1
        ;;
    *)
        echo -e "${RED}❌ Step inconnue : $STEP${NC}"
        echo "Utilisez: step1, step2, step3, ou step4"
        exit 1
        ;;
esac

# Afficher la configuration
echo "Configuration du serveur:"
echo "  Step          : $DESCRIPTION"
echo "  Port          : $PORT"
echo "  Classe        : $CLASS"
echo "  Connexions max: $CONNECTIONS"
echo ""
echo "=========================================="
echo ""

# Afficher les limites actuelles
echo "Limites de fichiers ouverts:"
echo "  Soft limit (actuelle) : $CURRENT_LIMIT"
echo "  Hard limit (maximum)  : $HARD_LIMIT"
echo ""

# Vérifier et augmenter la limite si nécessaire
if [ "$CURRENT_LIMIT" = "unlimited" ]; then
    echo -e "${GREEN}✓ Limite actuelle : unlimited (parfait !)${NC}"
elif [ "$CURRENT_LIMIT" -lt "$EXPECTED_LIMIT" ] 2>/dev/null; then
    # Déterminer la limite cible (le minimum entre attendu et hard limit)
    if [ "$HARD_LIMIT" != "unlimited" ] && [ "$HARD_LIMIT" -lt "$EXPECTED_LIMIT" ] 2>/dev/null; then
        TARGET_LIMIT=$HARD_LIMIT
        echo -e "${YELLOW}⚠️  Hard limit ($HARD_LIMIT) < attendu ($EXPECTED_LIMIT)${NC}"
        echo -e "${YELLOW}⚠️  Augmentation à $TARGET_LIMIT (maximum possible)...${NC}"
    else
        TARGET_LIMIT=$EXPECTED_LIMIT
        echo -e "${YELLOW}⚠️  Augmentation à $TARGET_LIMIT...${NC}"
    fi

    ulimit -n $TARGET_LIMIT 2>/dev/null

    if [ $? -eq 0 ]; then
        # Relire la limite après modification
        CURRENT_LIMIT=$(ulimit -n)
        echo -e "${GREEN}✓ Limite augmentée à $CURRENT_LIMIT${NC}"
    else
        echo -e "${RED}❌ Impossible d'augmenter la limite automatiquement${NC}"
        echo -e "${RED}   Hard limit: $HARD_LIMIT${NC}"
        echo -e "${RED}   Pour augmenter la hard limit, consultez CONFIGURATION_SYSTEME.md${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Limite actuelle : $CURRENT_LIMIT (suffisant)${NC}"
fi

echo ""

# Vérifier si le nombre de connexions est raisonnable (avec la limite MISE À JOUR)
if [ "$CURRENT_LIMIT" != "unlimited" ] && [ "$CONNECTIONS" -gt "$CURRENT_LIMIT" ] 2>/dev/null; then
    echo -e "${RED}⚠️  ATTENTION : Vous demandez $CONNECTIONS connexions mais la limite est $CURRENT_LIMIT${NC}"
    echo -e "${RED}   Le serveur risque d'échouer. Réduisez le nombre de connexions ou augmentez la hard limit.${NC}"
    echo -e "${RED}   Consultez CONFIGURATION_SYSTEME.md pour augmenter la hard limit.${NC}"
    echo ""
fi

# Compiler si nécessaire
echo "Compilation..."
./gradlew compileJava --console=plain -q

# Construire le classpath
CLASSPATH="app/build/classes/java/main"

# Si pas de lib, utiliser les dépendances Gradle
if [ ! -d "app/build/install/app/lib" ]; then
    echo "Préparation des dépendances..."
    ./gradlew installDist --console=plain -q
    echo ""
fi

# Ajouter tous les JARs au classpath
shopt -s nullglob  # Évite les erreurs si aucun fichier ne correspond
for jar in app/build/install/app/lib/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done
shopt -u nullglob

echo ""
echo "Pour tester avec le client, exécutez dans un autre terminal:"
echo "  ./run-client.sh localhost $PORT $CONNECTIONS"
echo ""
echo "=========================================="
echo ""

# Sur macOS, exec ne propage pas correctement les limites ulimit à Java.
# Solution: utiliser un login shell bash qui hérite les limites système correctement.
echo "Lancement de Java via login shell pour hériter les limites système..."
/bin/bash -l -c "
    echo \"Java va s'exécuter avec ulimit -n = \$(ulimit -n)\"
    java -Xms512m -Xmx4g -XX:-MaxFDLimit -cp \"$CLASSPATH\" \"$CLASS\"
"
