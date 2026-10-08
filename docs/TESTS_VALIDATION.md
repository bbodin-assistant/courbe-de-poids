# Validation instrumentée des critères d'acceptation

## Principe

Les critères fonctionnels sont couverts par des scénarios Android instrumentés exécutés sur un émulateur API 35.

Fichier de référence : app/src/androidTest/java/fr/bbodin/courbedepoids/AcceptanceCriteriaInstrumentedTest.java

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
| 10 | criterion10_rappelDeclencheUneNotification | Receiver et contrôle de notification publiée |
| 11 | criterion11_notificationOuvreApplication | PendingIntent puis contrôle de MainActivity |
| 12 | criterion12_rappelFonctionneApplicationFermee | Activité fermée puis receiver hors UI |
| 13 | criterion13_donneesConserveesApresFermeture | Fermeture puis nouvelle activité et lecture SQLite |
| 14 | criterion14_donneesConserveesApresRedemarrageSimule | BOOT_COMPLETED instrumenté et contrôle données/configuration |
| 15 | Workflow CI | Construction, publication, puis test de l'APK publié |

## Ordre obligatoire CI

1. version-gate extrait une version du message du commit.
2. Sans version, les jobs build et tests sont ignorés.
3. build-apk construit l'APK avec cette version.
4. L'APK est immédiatement publié comme artefact.
5. instrumented-tests télécharge cet artefact.
6. L'émulateur installe exactement cet APK.
7. L'APK de test d'instrumentation est construit puis les 15 scénarios sont exécutés contre l'application installée.

GitHub Actions permet de transmettre un artefact entre jobs avec upload-artifact/download-artifact et de séquencer les jobs avec needs. Les artefacts v4 fournissent également une empreinte SHA-256 lors du transfert.

## Convention de version

Le numéro doit apparaître dans le message du commit.

Formats acceptés :

- 0.3
- 0.3-1
- 0.3-2

Format général : milestonecount.featurecount-patchcount.

Pour l'état actuel, la version de référence est 0.3.

Exemple : feat: finaliser la validation - version 0.3

Un commit sans numéro de version ne déclenche ni construction ni test de l'APK. Le workflow peut être créé par le push, mais aucun runner de build ou de test n'est lancé.

## Identité de l'APK testé

Le job de test ne lance pas connectedDebugAndroidTest, qui pourrait reconstruire l'APK applicatif. Il installe d'abord l'APK déjà publié, puis construit uniquement l'APK de test et lance le runner d'instrumentation.

Ainsi, l'artefact livré et l'APK effectivement testé sont le même binaire.