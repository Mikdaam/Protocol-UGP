# UGP - Protocole de Distribution de Calcul

## Contexte

Ce dépôt GitHub contient le code source et la documentation du protocole UGEGreed, développé dans le cadre du cours de "Programmation Réseaux". L'objectif principal de ce projet est de créer un protocole de distribution de calcul, baptisé "UGP", permettant aux chercheurs de tester des conjectures de manière efficace.

## Introduction

Le protocole UGEGreed est un système de calcul distribué conçu pour faciliter la répartition de tâches entre différentes applications. Les applications utilisent le protocole TCP pour communiquer et échangent des fichiers JAR contenant des classes implémentant une interface de vérification de conjectures. L'objectif ultime est d'aider les chercheurs à tester des conjectures sur un très grand nombre de cas en distribuant leurs calculs sur plusieurs applications.

## Fonctionnalités clés

- **Calcul Distribué**: UGEGreed permet la distribution de calculs sur plusieurs applications, offrant ainsi une puissance de traitement accrue.
- **Communication via TCP**: Les applications communiquent entre elles en utilisant le protocole TCP, assurant une communication fiable et sécurisée.
- **Échange de Classes JAR**: Les fichiers JAR contiennent des classes implémentant une interface de vérification de conjectures, permettant une flexibilité dans les tests.

## Utilisation

Pour utiliser l'application UGEGreed, veuillez suivre les instructions ci-dessous :

```bash
java -jar UGEGreed.jar [port] [results_directory] [parent_host] [parent_port]
```

- **port**: Le numéro de port pour l'écoute.
- **results_directory**: Le chemin du répertoire pour stocker les fichiers de résultats.
- **parent_host** (optionnel): L'adresse de l'application parent.
- **parent_port** (optionnel): Le numéro de port de l'application parent.

## Prérequis

Assurez-vous d'avoir les éléments suivants installés sur votre système avant de commencer le déploiement du protocole UGEGreed :

- Java 19
- Maven

## Guide d'Installation

1. Clonez le dépôt sur votre machine locale.
    ```bash
    git clone https://github.com/votre-utilisateur/UGEGreed.git
    ```

2. Accédez au répertoire du projet.
    ```bash
    cd UGEGreed
    ```

3. Compilez le projet avec Maven.
    ```bash
    mvn compile
    ```

4. Exécutez le protocole UGEGreed en spécifiant les paramètres requis.
    ```bash
    java -jar UGEGreed.jar [port] [results_directory] [parent_host] [parent_port]
    ```

## Licence

Ce projet est sous licence MIT - voir le fichier [LICENSE](LICENSE) pour plus de détails.
