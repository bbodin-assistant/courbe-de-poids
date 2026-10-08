# Validation instrumentée des critères d'acceptation

## Principe

Les critères fonctionnels sont couverts par des scénarios Android instrumentés exécutés sur un émulateur API 35.

Fichier de référence : `app/src/androidTest/java/fr/bbodin/courbedepoids/AcceptanceCriteriaInstrumentedTest.java`

## Matrice de validation

| Critère | Scénario automatisé | Validation |
|---|---|---|
| 1 | criterion01_enregistrerLePoidsDuJour | Saisie UI, enregistrement, statut UI et lecture SQLite |
| 2 | criterion02_enregistrerUneMesurePassee | Mesure passée puis contrôle dans l'historique UI |
| 3 | criterion03_uneDateNePossedeQuUneMesure | Deux écritures sur une date, une seule mesure conservée |
| 4 | criterion04_modifierUneMesureExistante | Modification depuis l'UI puis contrôle de l'historique |
| 5 | criterion05_supprimerUneMesure | Suppression depuis l'UI puis contrôle SQLite |
| 6 | criterion06_historiqueCorrectementAffiche | Trois mesures et vérification de l'ordre décroissant |
| 7 | criterion07_courbeRepresenteLesMesures | Courbe visible et deux mesures présentes dans son modèle |
| 8 | criterion08_activerDesactiverRappel | Activation puis désactivation et contrôle des préférences |
| 9 | criterion09_choisirHeureDuRappel | TimePicker, choix 07:35 et contrôle de configuration |
| 10 | criterion10_rappelDeclencheUneNotification | Programmation du rappel et contrôle de la notification publiée |
| 11 | criterion11_notificationOuvreApplication | PendingIntent puis contrôle de MainActivity |
| 12 | criterion12_rappelFonctionneApplicationFermee | Activité fermée puis déclenchement du receiver hors UI |
| 13 | criterion13_donneesConserveesApresFermeture | Fermeture puis nouvelle activité et lecture SQLite |
| 14 | criterion14_donneesConserveesApresRedemarrageSimule | Broadcast BOOT_COMPLETED puis contrôle des données et préférences |
| 15 | criterion15_lapplicationEstInstallableEtVersionnee | Application installée, package attendu et version non vide |

## Ordre CI

Le workflow `.github/workflows/build-apk.yml` s'exécute sur les pushes vers `main`.

1. `version-gate` lit la version déclarée dans `app/build.gradle`.
2. Cette version est comparée à celle du commit parent.
3. Si la version n'a pas changé, les jobs de build et de test sont ignorés.
4. Si la version a changé, `build-apk` construit l'APK debug avec cette version.
5. L'APK est vérifié puis publié comme artefact.
6. `instrumented-tests` télécharge exactement cet artefact.
7. L'émulateur API 35 installe l'APK publié.
8. L'APK des tests instrumentés est construit séparément.
9. Le runner Android instrumenté exécute les 15 scénarios contre l'APK applicatif déjà installé.

Le job de test ne lance donc pas `connectedDebugAndroidTest`, qui pourrait reconstruire l'APK applicatif. Il installe d'abord l'APK publié, puis construit uniquement l'APK de test.

## Convention de version

La version est déclarée dans `app/build.gradle` et suit la convention :

```
milestonecount.featurecount-patchcount
```

Exemples :

- `0.3`
- `0.3-1`
- `0.3-4`

La valeur par défaut actuellement présente dans le dépôt est `0.3-4`. La CI ne dépend pas du message du commit : elle déclenche le build et les tests lorsque la version de `app/build.gradle` diffère de celle du commit parent.

## Identité de l'APK testé

L'APK applicatif construit par `build-apk` est publié avec l'artefact :

```
courbe-de-poids-apk-<version>
```

Le job `instrumented-tests` télécharge cet artefact, vérifie sa version via `app-version.txt`, puis installe :

```
published-apk/app/build/outputs/apk/debug/app-debug.apk
```

Ainsi, l'APK applicatif publié comme artefact est le même binaire que celui sur lequel les tests instrumentés sont exécutés.
