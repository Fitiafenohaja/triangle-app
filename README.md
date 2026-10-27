# triangle-app

TP d'intégration et de déploiement continus (CI/CD) avec **Maven**, **Jenkins** et **Docker**.

L'application est volontairement simple : elle indique si trois longueurs peuvent former un triangle
(chaque côté doit être strictement positif et plus petit que la somme des deux autres).
L'objectif du TP est la chaîne d'automatisation autour : compilation, tests, packaging,
construction d'une image Docker et publication sur Docker Hub.

## Le projet en bref

**Le principe :** à chaque build, Jenkins récupère le code sur GitHub, le compile, lance les tests,
puis, si tout passe, empaquette l'application dans une image Docker publiée sur Docker Hub.
Aucune étape manuelle : un code qui ne compile pas ou un test qui échoue arrête le pipeline,
et rien n'est publié.

```
GitHub ──► Jenkins ──► compile ──► tests ──► jar ──► image Docker ──► Docker Hub
```

**L'application :** la méthode `Triangle.estValide(a, b, c)` applique l'inégalité triangulaire.

| Côtés     | Résultat | Raison                              |
|-----------|----------|-------------------------------------|
| 3, 4, 5   | `true`   | chaque côté < somme des deux autres |
| 1, 2, 5   | `false`  | 1 + 2 n'est pas > 5                 |
| -1, 5, 5  | `false`  | un côté négatif                     |

Ces trois cas sont couverts par les tests JUnit de `TriangleTest`.

**Ce que le TP permet de pratiquer :**

- structurer un projet Java avec Maven et écrire des tests unitaires ;
- décrire un pipeline CI/CD sous forme de code (`Jenkinsfile`) ;
- conteneuriser une application avec un `Dockerfile` ;
- gérer des secrets (tokens GitHub et Docker Hub) avec les credentials Jenkins.

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

## Démarrer Jenkins

### 1. Lancer le conteneur

```bash
docker run -d --name jenkins --restart unless-stopped \
  -p 8080:8080 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$(command -v docker)":/usr/bin/docker:ro \
  --group-add "$(stat -c %g /var/run/docker.sock)" \
  jenkins/jenkins:2.555.2-lts
```

| Option                                   | Rôle                                                              |
|------------------------------------------|-------------------------------------------------------------------|
| `-p 8080:8080`                           | Interface web sur http://localhost:8080                           |
| `-v jenkins_home:/var/jenkins_home`      | Volume persistant : jobs, utilisateurs, plugins, historique       |
| `-v /var/run/docker.sock:...`            | Permet au pipeline d'utiliser le Docker de la machine hôte        |
| `-v "$(command -v docker)":/usr/bin/docker:ro` | Fournit la commande `docker` à l'intérieur du conteneur     |
| `--group-add ...`                        | Donne à Jenkins le droit d'accéder au socket Docker               |
| `--restart unless-stopped`               | Redémarre Jenkins automatiquement avec Docker                     |

Attendre que Jenkins soit prêt :

```bash
docker logs -f jenkins   # attendre "Jenkins is fully up and running", puis Ctrl+C
```

Vérifier que Docker est utilisable depuis Jenkins :

```bash
docker exec jenkins docker version
```

### 2. Première connexion (installation neuve uniquement)

Si le volume `jenkins_home` est neuf, Jenkins demande un mot de passe initial :

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Ouvrir http://localhost:8080, coller ce mot de passe, choisir
**Install suggested plugins**, puis créer le compte administrateur.

Si le volume existe déjà, se connecter directement avec un compte existant.

### 3. Installer les plugins nécessaires

*Manage Jenkins → Plugins → Available plugins* :

- **Pipeline** (inclus dans les plugins suggérés)
- **Git** (inclus dans les plugins suggérés)
- **Credentials Binding** (inclus dans les plugins suggérés)

### 4. Configurer Maven

*Manage Jenkins → Tools → Maven installations → Add Maven* :

- **Name** : `Maven3` (doit correspondre exactement au nom utilisé dans le `Jenkinsfile`)
- Cocher **Install automatically**

### 5. Ajouter les credentials

*Manage Jenkins → Credentials → System → Global credentials → Add Credentials* :

| ID                   | Type                          | Contenu                                                  |
|----------------------|-------------------------------|----------------------------------------------------------|
| `dockerhubpass`      | Secret text                   | Token d'accès Docker Hub (*Account settings → Personal access tokens*) |
| `github_credentials` | Username with password        | Utilisateur GitHub + token personnel (seulement si le dépôt est privé) |

### 6. Créer le job

*New Item* → nom `triangle-pipeline` → type **Pipeline** → OK, puis dans la section *Pipeline* :

- **Definition** : Pipeline script from SCM
- **SCM** : Git
- **Repository URL** : `https://github.com/Fitiafenohaja/triangle-app.git`
- **Credentials** : `github_credentials` (ou aucun si le dépôt est public)
- **Branch Specifier** : `*/main`
- **Script Path** : `Jenkinsfile`

Enregistrer.

### 7. Lancer un build

Dans le job, cliquer sur **Build Now**, puis ouvrir le build → **Console Output** pour suivre
l'exécution. Un build réussi se termine par :

```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
...
Build SUCCESS
Finished: SUCCESS
```

L'image est alors disponible sur Docker Hub :

```bash
docker run --rm fitiafenohajaun/triangle-app:1.0
```

## Gérer Jenkins au quotidien

```bash
docker stop jenkins      # arrêter
docker start jenkins     # redémarrer
docker restart jenkins   # relancer (après une modification de configuration)
docker logs -f jenkins   # voir les logs
```

Les données sont conservées dans le volume `jenkins_home` : supprimer le conteneur
(`docker rm -f jenkins`) ne perd ni les jobs ni les utilisateurs.

## Dépannage

| Problème                                         | Solution                                                                 |
|--------------------------------------------------|--------------------------------------------------------------------------|
| La page ne s'ouvre pas                           | Vérifier l'adresse : port **8080** (pas 8000), et `docker ps` pour voir si le conteneur tourne |
| `address already in use` au lancement            | Un autre service utilise le port : changer le côté hôte, ex. `-p 8081:8080` |
| `docker: not found` dans le build                | Le binaire Docker n'est pas monté : relancer le conteneur avec l'option `-v "$(command -v docker)":/usr/bin/docker:ro` |
| `permission denied ... docker.sock`              | Ajouter `--group-add "$(stat -c %g /var/run/docker.sock)"`               |
| `Tool type "maven" does not have an install of "Maven3"` | Configurer l'outil Maven nommé `Maven3` (étape 4)                |
| Échec du `docker push` (`denied`)                | Vérifier le credential `dockerhubpass` et les droits du compte Docker Hub |
| Mot de passe Jenkins oublié                      | Voir ci-dessous                                                          |

### Réinitialiser un mot de passe Jenkins oublié

Jenkins ne stocke que des empreintes bcrypt : un mot de passe ne peut pas être retrouvé,
seulement remplacé. Exemple pour l'utilisateur `tino` avec le nouveau mot de passe `NOUVEAU` :

```bash
H=$(python3 -c 'import bcrypt;print(bcrypt.hashpw(b"NOUVEAU",bcrypt.gensalt(10,b"2a")).decode())')
F=$(docker exec jenkins sh -c 'ls -d /var/jenkins_home/users/tino_*')/config.xml
docker exec -e H="$H" jenkins sh -c "cp $F $F.bak && sed -i \"s|<passwordHash>[^<]*</passwordHash>|<passwordHash>#jbcrypt:\$H</passwordHash>|\" $F"
docker restart jenkins
```

Changer ensuite le mot de passe depuis l'interface (nom d'utilisateur en haut à droite → *Security*).
