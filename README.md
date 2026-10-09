# Courbe de poids

Application Android native permettant d'enregistrer et de consulter des mesures de poids.

**[Télécharger directement le dernier APK debug](https://github.com/bbodin-assistant/courbe-de-poids/releases/download/latest-debug/app-debug.apk)** — le lien pointe vers le fichier `.apk`, pas vers une archive ZIP. Le fichier est mis à jour automatiquement après chaque build réussi sur `main`.

[Voir les exécutions du workflow](https://github.com/bbodin-assistant/courbe-de-poids/actions/workflows/build-apk.yml?query=branch%3Amain).

## Fonctionnalités

- Enregistrer le poids du jour.
- Ajouter une mesure pour une date passée.
- Modifier une mesure existante.
- Supprimer une mesure.
- Conserver au plus une mesure par date.
- Consulter l'historique des mesures, du plus récent au plus ancien.
- Afficher les mesures sous forme de courbe.
- Activer ou désactiver un rappel quotidien.
- Choisir l'heure du rappel.
- Afficher une notification de rappel ouvrant l'application.
- Restaurer la programmation du rappel après le redémarrage de l'appareil.

Les mesures sont stockées localement dans une base SQLite (`weight.db`). Les préférences du rappel sont stockées dans les préférences Android de l'application.

## Structure principale

```
app/
├── src/main/
│   ├── java/fr/bbodin/courbedepoids/
│   │   ├── MainActivity.java
│   │   ├── AddMeasurementActivity.java
│   │   ├── HistoryActivity.java
│   │   ├── SettingsActivity.java
│   │   ├── WeightDatabase.java
│   │   ├── WeightChartView.java
│   │   ├── ReminderScheduler.java
│   │   ├── ReminderReceiver.java
│   │   └── BootReceiver.java
│   └── res/
└── src/androidTest/
    └── java/fr/bbodin/courbedepoids/
        └── AcceptanceCriteriaInstrumentedTest.java
docs/
└── TESTS_VALIDATION.md
.github/
└── workflows/
    └── build-apk.yml
```

## Environnement de développement

- Android SDK / compileSdk 35
- minSdk 26
- targetSdk 35
- Java 17
- Gradle 8.9
- AndroidX
- Tests instrumentés avec AndroidX Test et Espresso

Le projet utilise une application Android Java classique, sans framework multiplateforme.

## Construire l'APK

Avec un JDK 17, le SDK Android et Gradle disponibles :

```bash
gradle assembleDebug -PVERSION_NAME=0.3-4
```

L'APK généré se trouve dans :

```
app/build/outputs/apk/debug/app-debug.apk
```

Le numéro de version peut également être défini par défaut dans `app/build.gradle`. La version actuellement définie dans le dépôt est **0.3-4**.

## Tests instrumentés

Les scénarios d'acceptation sont regroupés dans :

```
app/src/androidTest/java/fr/bbodin/courbedepoids/AcceptanceCriteriaInstrumentedTest.java
```

Ils couvrent notamment :

1. enregistrement du poids du jour ;
2. ajout d'une mesure passée ;
3. unicité d'une mesure par date ;
4. modification ;
5. suppression ;
6. affichage et ordre de l'historique ;
7. représentation dans la courbe ;
8. activation/désactivation du rappel ;
9. choix de l'heure ;
10. déclenchement d'une notification ;
11. ouverture de l'application depuis la notification ;
12. fonctionnement lorsque l'activité est fermée ;
13. conservation des données après fermeture ;
14. conservation après redémarrage simulé ;
15. présence d'une version dans l'application.

La description détaillée de la validation se trouve dans [`docs/TESTS_VALIDATION.md`](docs/TESTS_VALIDATION.md).

## CI GitHub Actions

Le workflow [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml) s'exécute sur les pushes vers `main`. Il valide la version et construit l'APK debug, puis :

- publie un artefact temporaire conservé 30 jours ;
- met à jour une pré-release GitHub `latest-debug` avec le fichier `app-debug.apk`, accessible par le lien direct en haut de cette page.

Cette pré-release contient uniquement l'APK debug. La génération de l'APK/AAB release signé et les tests instrumentés restent désactivés.

## Versionnement

La version suit la convention :

```
milestonecount.featurecount-patchcount
```

Exemples :

- `0.3`
- `0.3-1`
- `0.3-4`

La CI valide le format de version et vérifie l'incrément du `versionCode` lorsqu'une nouvelle version est déclarée.

## Licence

Aucune licence n'est actuellement déclarée dans le dépôt.
