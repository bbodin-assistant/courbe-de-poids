# Courbe de poids — Android

Application Android minimale contenant un bouton. Un clic sur le bouton génère une notification système.

## Générer l'APK localement

Avec un JDK 17 et le SDK Android installés :

    gradle assembleDebug

L'APK sera généré ici :

    app/build/outputs/apk/debug/app-debug.apk

## GitHub Actions

Le workflow `.github/workflows/build-apk.yml` se lance à chaque push sur `main`, et peut aussi être lancé manuellement depuis l'onglet Actions.

L'APK `app-debug.apk` est publié comme artifact de l'exécution GitHub Actions et peut être téléchargé depuis cette exécution.

Sur Android 13 et versions ultérieures, l'application demande l'autorisation d'envoyer des notifications au premier lancement.
