#!/bin/bash

# Script pour lancer le client de charge avec la limite de fichiers augmentée
# Usage: ./run-client.sh [host] [port] [connexions] [intervalle_ms]
# Exemples:
#   ./run-client.sh localhost 5042 1000       # 1000 connexions vers step1
#   ./run-client.sh localhost 5044 10000      # 10000 connexions vers step3
#   ./run-client.sh localhost 5045 50000 2000 # 50000 connexions, envoi toutes les 2s

set -e

# Couleurs pour l'affichage
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Valeurs par défaut
DEFAULT_HOST="localhost"
DEFAULT_PORT="5042"
DEFAULT_CONNECTIONS="1000"
DEFAULT_INTERVAL="1000"

# Arguments
HOST=${1:-$DEFAULT_HOST}
PORT=${2:-$DEFAULT_PORT}
CONNECTIONS=${3:-$DEFAULT_CONNECTIONS}
INTERVAL=${4:-$DEFAULT_INTERVAL}

# Vérifier la limite actuelle
CURRENT_LIMIT=$(ulimit -n)
HARD_LIMIT=$(ulimit -Hn)
EXPECTED_LIMIT=${CONNECTIONS}

echo "=========================================="
echo "  Client de Charge - TP I/O Non Bloquants"
echo "=========================================="
echo ""

# Afficher la configuration
echo "Configuration du client:"
echo "  Serveur cible: $HOST:$PORT"
echo "  Connexions   : $CONNECTIONS"
echo "  Intervalle   : ${INTERVAL}ms"
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
    echo -e "${RED}   Le client risque d'échouer. Réduisez le nombre de connexions ou augmentez la hard limit.${NC}"
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

# Sur macOS, exec ne propage pas correctement les limites ulimit à Java.
# Solution: utiliser un login shell bash qui hérite les limites système correctement.
echo "Lancement de Java via login shell pour hériter les limites système..."
/bin/bash -l -c "
    echo \"Java va s'exécuter avec ulimit -n = \$(ulimit -n)\"
    java -Xms512m -Xmx4g -XX:-MaxFDLimit -cp \"$CLASSPATH\" fr.qsh.fmt.java.network.LoadClient \"$HOST\" \"$PORT\" \"$CONNECTIONS\" \"$INTERVAL\"
"
