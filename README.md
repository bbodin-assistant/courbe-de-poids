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

## Configuration locale

Le fichier versionné `.env.default` définit les valeurs par défaut. Les variables définies dans `.env` les remplacent pour une configuration propre à votre machine :

```bash
cp .env.default .env
```

Le fichier `.env` est ignoré par Git et n'est pas nécessaire pour utiliser les valeurs par défaut. Adaptez-y les chemins et paramètres spécifiques à votre machine. Ne commitez jamais de secrets ni le keystore. Les mots de passe et alias de signature de `.env.default` sont uniquement des valeurs indicatives et doivent être remplacés avant une release signée.

Les valeurs contenant des caractères spéciaux doivent être écrites en respectant la syntaxe Make utilisée par le fichier : dans une valeur Make, doublez les signes dollar (`$$`) et échappez les dièses qui doivent être littéraux.

## Construire l'APK debug

Avec un JDK 17, le SDK Android et Gradle disponibles :

```bash
make build
```

L'APK généré se trouve dans :

```
app/build/outputs/apk/debug/app-debug.apk
```

## Construire un APK release signé

Renseignez dans `.env` les quatre paramètres de signature ainsi que `RELEASE_KEYSTORE_PATH`. Le chemin du keystore est relatif à la racine du dépôt, sauf s'il est absolu.

```bash
make release
```

L'APK généré se trouve dans :

```
app/build/outputs/apk/release/app-release.apk
```

## Tests

Tests unitaires JVM :

```bash
make test-unit
```

Tests instrumentés (un appareil ou émulateur doit être connecté) :

```bash
make devices
make test
```

Les scénarios d'acceptation sont regroupés dans `app/src/androidTest/java/fr/bbodin/courbedepoids/AcceptanceCriteriaInstrumentedTest.java`. La description détaillée de la validation se trouve dans [`docs/TESTS_VALIDATION.md`](docs/TESTS_VALIDATION.md).

## CI GitHub Actions

Le workflow [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml) s'exécute sur les pushes vers `main`. Il valide le format de version et l'incrément du `versionCode`, construit l'APK debug, puis publie un artefact conservé 30 jours et met à jour la pré-release GitHub `latest-debug` avec `app-debug.apk`.

La CI lit la version par défaut depuis `.env.default` et ne nécessite aucun fichier `.env` privé. Cette pré-release contient uniquement l'APK debug ; la génération de l'APK/AAB release signé et les tests instrumentés restent désactivés.

## Versionnement

La valeur de version par défaut est définie une seule fois dans `.env.default` (`VERSION_NAME`). La CI valide le format et vérifie l'incrément du `versionCode` lorsqu'une nouvelle version est déclarée.

## Licence

Aucune licence n'est actuellement déclarée dans le dépôt.
