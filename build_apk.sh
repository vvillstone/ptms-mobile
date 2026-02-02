#!/bin/bash
# ====================================================
# Script de Build Android APK - PTMS Mobile v2.0
# ====================================================

echo ""
echo "===================================================="
echo "  BUILD ANDROID APK - PTMS Mobile v2.0"
echo "===================================================="
echo ""

# Couleurs pour le terminal
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Vérifier que nous sommes dans le bon répertoire
if [ ! -f "gradlew" ]; then
    echo -e "${RED}[ERREUR]${NC} gradlew non trouvé!"
    echo "Assurez-vous d'exécuter ce script depuis le répertoire appAndroid"
    exit 1
fi

# Rendre gradlew exécutable
chmod +x gradlew

echo -e "${YELLOW}[1/4]${NC} Nettoyage des builds précédents..."
./gradlew clean
if [ $? -ne 0 ]; then
    echo -e "${RED}[ERREUR]${NC} Le nettoyage a échoué!"
    exit 1
fi

echo ""
echo -e "${YELLOW}[2/4]${NC} Build du projet Android..."
./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo -e "${RED}[ERREUR]${NC} Le build a échoué!"
    exit 1
fi

echo ""
echo -e "${YELLOW}[3/4]${NC} Vérification du fichier APK..."
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo -e "${GREEN}[OK]${NC} APK généré avec succès!"
    echo ""
    echo "Emplacement: $APK_PATH"

    # Obtenir la taille du fichier
    SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
    echo "Taille: $SIZE"

    # Obtenir la date de création
    DATE=$(ls -l "$APK_PATH" | awk '{print $6, $7, $8}')
    echo "Date: $DATE"
else
    echo -e "${RED}[ERREUR]${NC} APK non trouvé!"
    exit 1
fi

echo ""
echo -e "${YELLOW}[4/4]${NC} Copie de l'APK vers le répertoire principal..."
mkdir -p ../apk_output
cp -f "$APK_PATH" "../apk_output/PTMS-Mobile-v2.0-debug.apk"
if [ $? -eq 0 ]; then
    echo -e "${GREEN}[OK]${NC} APK copié vers: ../apk_output/PTMS-Mobile-v2.0-debug.apk"
fi

echo ""
echo "===================================================="
echo -e "  ${GREEN}BUILD TERMINÉ AVEC SUCCÈS!${NC}"
echo "===================================================="
echo ""
echo "Fichiers générés:"
echo "  - app/build/outputs/apk/debug/app-debug.apk"
echo "  - ../apk_output/PTMS-Mobile-v2.0-debug.apk"
echo ""
echo "Vous pouvez maintenant installer l'APK sur votre appareil Android."
echo ""
