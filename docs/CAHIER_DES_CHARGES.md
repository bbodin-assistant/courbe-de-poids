# Cahier des charges — Application Android de suivi du poids

## 1. Objectif

Transformer l'application Android actuelle en une application permettant à l'utilisateur d'enregistrer et de suivre son poids dans le temps.

L'application doit permettre :
- d'enregistrer son poids du jour ;
- d'enregistrer un poids correspondant à une date passée ;
- de consulter l'évolution de son poids sous forme de courbe ;
- de recevoir chaque matin une notification lui rappelant de renseigner son poids ;
- de choisir l'heure à laquelle cette notification doit être envoyée.

L'application fonctionne entièrement en local pour cette première version, sans compte utilisateur ni serveur.

## 2. Fonctionnalités principales

### 2.1 Enregistrer le poids du jour

L'écran principal permet de saisir simplement le poids du jour.

- La date du jour est automatiquement associée à la mesure.
- Le poids est enregistré localement.
- Si un poids existe déjà pour cette date, l'utilisateur peut le remplacer.
- Une confirmation visuelle est affichée après l'enregistrement.
- Unité : kilogramme (kg).

### 2.2 Enregistrer un poids passé

L'utilisateur peut ajouter une mesure correspondant à une date antérieure.

- Sélection de la date avec un sélecteur de date Android.
- Saisie numérique du poids.
- Une mesure peut être ajoutée pour toute date passée.
- Si une mesure existe déjà pour cette date, elle peut être modifiée/remplacée.

## 3. Historique

L'application propose un écran listant les mesures enregistrées.

L'utilisateur peut :
- consulter ses anciennes mesures ;
- modifier une mesure ;
- supprimer une mesure.

Les mesures sont triées de la plus récente à la plus ancienne.

## 4. Courbe de poids

L'application affiche une courbe représentant l'évolution du poids.

- Axe horizontal : dates des mesures.
- Axe vertical : poids en kilogrammes.
- Seules les mesures réellement enregistrées sont représentées.
- Les jours sans mesure ne sont pas artificiellement remplis.

## 5. Notification quotidienne

L'application peut rappeler quotidiennement à l'utilisateur d'enregistrer son poids.

Exemple :

> **Courbe de poids**  
> Il est temps de noter votre poids du jour.

Dans les paramètres, l'utilisateur peut :
- activer/désactiver le rappel ;
- choisir l'heure du rappel.

## 6. Comportement de la notification

Lorsque le rappel est activé :
- une notification est générée chaque jour à l'heure choisie ;
- elle fonctionne même si l'application n'est pas ouverte ;
- toucher la notification ouvre l'application sur l'écran permettant d'enregistrer le poids du jour.

Si l'heure est modifiée, la programmation précédente est remplacée. Si le rappel est désactivé, aucune nouvelle notification quotidienne n'est programmée.

L'application restaure la programmation du rappel après un redémarrage du téléphone.

## 7. Stockage des données

Toutes les données sont stockées localement sur le téléphone.

Aucune connexion Internet n'est nécessaire pour enregistrer un poids, consulter l'historique, afficher la courbe ou recevoir le rappel.

Chaque mesure contient au minimum :

```
date
poids
```

Une seule mesure est autorisée par date.

## 8. Écrans

### Accueil

- Saisie du poids du jour.
- Enregistrement.
- Aperçu de l'évolution récente.

### Historique

- Liste de toutes les mesures.
- Ajout d'une mesure passée.
- Modification.
- Suppression.

### Paramètres

- Activation/désactivation du rappel.
- Choix de l'heure du rappel.

## 9. Ajout et modification d'une mesure

Depuis l'historique, un bouton **+ Ajouter une mesure** ouvre un formulaire avec :
1. la date ;
2. le poids.

Une mesure existante peut être modifiée. Une confirmation est demandée avant une suppression définitive.

## 10. Notifications Android

L'application utilise le système de notifications Android et demande l'autorisation correspondante sur les versions Android qui l'exigent.

Un canal dédié au rappel du poids est créé :

> Rappel du poids

## 11. Programmation du rappel

Le rappel quotidien est programmé avec les mécanismes Android adaptés aux alarmes/rappels.

Flux attendu :

```
Rappel activé
    ↓
Heure configurée
    ↓
Notification Android
    ↓
Ouverture de l'application
    ↓
Saisie du poids
```

## 12. Validation des données

Le poids doit :
- être numérique ;
- être supérieur à zéro ;
- être enregistré avec une précision raisonnable, par exemple au dixième de kilogramme.

Les valeurs invalides sont refusées avant l'enregistrement.

## 13. Interface

L'interface doit être :
- simple ;
- lisible ;
- adaptée à un usage quotidien ;
- compatible avec différentes tailles d'écran Android ;
- en français.

La saisie du poids utilise un clavier numérique lorsque pertinent.

## 14. Architecture technique

Le projet reste un projet Android natif et simple.

Technologies prévues :
- Android ;
- Java ou Kotlin ;
- Gradle ;
- stockage local ;
- notifications Android ;
- système de programmation d'alarmes Android ;
- composant de graphique pour la courbe de poids.

Le projet doit rester compilable automatiquement avec GitHub Actions.

## 15. APK

Le workflow GitHub Actions existant doit être conservé et adapté si nécessaire.

Chaque modification poussée sur `main` doit pouvoir :
1. compiler l'application ;
2. générer l'APK ;
3. publier l'APK comme artifact GitHub Actions.

Fichier attendu :

```
app-debug.apk
```

## 16. Évolutions hors périmètre initial

Ces fonctionnalités ne sont pas nécessaires pour la première version :
- poids cible ;
- IMC ;
- taille ;
- statistiques avancées ;
- moyennes sur 7 ou 30 jours ;
- export CSV ;
- export PDF ;
- sauvegarde/restauration ;
- synchronisation cloud ;
- unités kg/lb ;
- plusieurs profils.

L'architecture doit toutefois permettre de les ajouter ultérieurement.

## 17. Critères d'acceptation

1. Enregistrer le poids du jour.
2. Enregistrer un poids à une date passée.
3. Une date ne possède qu'une mesure.
4. Modifier une mesure existante.
5. Supprimer une mesure.
6. Afficher correctement l'historique.
7. Représenter correctement les mesures sur une courbe.
8. Activer/désactiver le rappel quotidien.
9. Choisir l'heure du rappel.
10. Générer une notification quotidiennement à l'heure configurée.
11. Ouvrir l'application depuis la notification.
12. Faire fonctionner le rappel lorsque l'application est fermée.
13. Conserver les données après fermeture de l'application.
14. Conserver les données après redémarrage du téléphone.
15. Générer automatiquement un APK avec GitHub Actions.

## 18. Résultat attendu

L'application doit devenir un outil de suivi du poids local, simple et rapide :

**recevoir le rappel → ouvrir l'application → saisir le poids → enregistrer → suivre l'évolution sur la courbe.**
