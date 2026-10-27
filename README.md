# triangle-app

TP d'intégration et de déploiement continus (CI/CD) avec **Maven**, **Jenkins** et **Docker**.

L'application est volontairement simple : elle indique si trois longueurs peuvent former un triangle
(chaque côté doit être strictement positif et plus petit que la somme des deux autres).
L'objectif du TP est la chaîne d'automatisation autour : compilation, tests, packaging,
construction d'une image Docker et publication sur Docker Hub.

## Structure

```
.
├── src/main/java/com/example/
│   ├── Triangle.java      # estValide(a, b, c) : règle de l'inégalité triangulaire
│   └── App.java           # point d'entrée, affiche le résultat pour (3, 4, 5)
├── src/test/java/com/example/
│   └── TriangleTest.java  # tests JUnit 5
├── pom.xml                # build Maven (Java 21, JUnit 5)
├── Dockerfile             # image d'exécution basée sur eclipse-temurin:21-jre
└── Jenkinsfile            # pipeline CI/CD
```

## Prérequis

- Java 21
- Maven 3
- Docker (pour l'image et pour faire tourner Jenkins)

## Démarrer en local

```bash
# Compiler et lancer les tests
mvn test

# Construire le jar
mvn package

# Lancer l'application
java -jar target/triangle-app-1.0.0.jar
# Triangle 3,4,5 valide : true
```

Avec Docker :

```bash
mvn package
docker build -t triangle-app .
docker run --rm triangle-app
```

## Pipeline Jenkins

Le `Jenkinsfile` enchaîne les étapes suivantes :

| Étape         | Action                                                        |
|---------------|---------------------------------------------------------------|
| Checkout      | Récupère la branche `main` de ce dépôt                        |
| Build         | `mvn clean compile`                                           |
| Unit Tests    | `mvn test`                                                    |
| Package       | `mvn package`                                                 |
| Docker Build  | Construit l'image `fitiafenohajaun/triangle-app:1.0`          |
| Docker Login  | Se connecte à Docker Hub                                      |
| Docker Push   | Publie l'image sur Docker Hub                                 |

### Lancer Jenkins avec Docker

```bash
docker run -d --name jenkins --restart unless-stopped \
  -p 8080:8080 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$(command -v docker)":/usr/bin/docker:ro \
  --group-add "$(stat -c %g /var/run/docker.sock)" \
  jenkins/jenkins:2.555.2-lts
```

Le socket et le binaire Docker sont montés dans le conteneur pour que le pipeline puisse
construire et pousser l'image. Jenkins est ensuite accessible sur http://localhost:8080.

### Configuration Jenkins requise

- **Outil Maven** nommé `Maven3` (*Manage Jenkins → Tools*).
- **Credential `github_credentials`** : identifiants GitHub (nécessaire seulement si le dépôt est privé).
- **Credential `dockerhubpass`** : de type *Secret text*, contenant un token d'accès Docker Hub.
- **Job** de type *Pipeline*, « Pipeline script from SCM », pointant sur
  `https://github.com/Fitiafenohaja/triangle-app.git`, branche `*/main`, script `Jenkinsfile`.

Il suffit ensuite de cliquer sur **Build Now** dans le job.
