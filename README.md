# Courbe de poids — Android

Application Android native de suivi du poids.

## Versionnement

Les builds GitHub Actions sont déclenchés uniquement pour un commit dont le message contient un numéro de version selon la nomenclature :

- 0.3
- 0.3-1
- 0.3-2

Format général : milestonecount.featurecount-patchcount.

La version actuelle est 0.3.

Exemple de commit déclencheur :

    feat: finaliser la validation - version 0.3

## GitHub Actions

L'ordre de la CI est strict :

1. extraction de la version ;
2. construction de l'APK ;
3. publication de l'APK comme artefact ;
4. téléchargement de cet artefact ;
5. installation de cet APK dans l'émulateur ;
6. exécution des tests instrumentés contre ce même APK.

Sans numéro de version dans le commit, les jobs de build et de test sont ignorés.

L'artefact est nommé courbe-de-poids-apk-<version>.

## Générer l'APK localement

Avec un JDK 17 et le SDK Android installés :

    gradle assembleDebug -PVERSION_NAME=0.3

L'APK est généré ici :

    app/build/outputs/apk/debug/app-debug.apk
