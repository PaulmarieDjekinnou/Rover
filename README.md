# Rover

Simulation de rovers martiens qui explorent un plateau rectangulaire.

Chaque rover a une position `(x, y)` et une orientation (`N`, `E`, `S`, `W`). Il reçoit une suite d'instructions :

| Instruction | Effet |
|---|---|
| `L` | tourne de 90° à gauche, sans bouger |
| `R` | tourne de 90° à droite, sans bouger |
| `M` | avance d'une case dans la direction actuelle |

Le coin inférieur gauche du plateau est `(0, 0)`. Le nord correspond à `y + 1`.

Un déplacement qui ferait sortir le rover du plateau est ignoré : le rover reste sur place et passe à l'instruction suivante. Les rovers sont traités l'un après l'autre et indépendamment, sans détection de collision.

## Format du fichier d'entrée

```
5 5
1 2 N
LMLMLMLMM
3 3 E
MMRMMRMRRM
```

- **Ligne 1** : coordonnées du coin supérieur droit du plateau.
- **Puis, pour chaque rover, deux lignes** : sa position de départ et son orientation, puis ses instructions.

Les lignes vides et les espaces en trop sont ignorés.

## Sortie

La position finale et l'orientation de chaque rover, une par ligne, dans l'ordre du fichier :

```
1 3 N
5 1 E
```

Si le fichier est invalide, le programme affiche un message d'erreur en français et s'arrête avec le code 1. Par exemple :

```
Entrée invalide : Direction invalide : « X ». Attendu : N, E, S ou W
```

## Prérequis

- Java 21
- Maven

## Utilisation

```bash
mvn package
java -cp target/Rover-1.0-SNAPSHOT.jar fr.paulm.projects.rover.Main src/main/resources/input.txt
```

## Tests

```bash
mvn test
```

Les tests lisent des fichiers d'entrée dans `src/test/resources/simulations/` :

- `valid/<nom>.txt` : entrée d'un cas nominal, lue puis simulée, et comparée au résultat attendu dans `<nom>.expected.txt` (`SimulationTest`) ;
- `invalid/<nom>.txt` : entrée d'un cas d'erreur, dont le message attendu est indiqué dans `TextFileReaderTest`.

Pour ajouter un cas nominal, il suffit de créer les deux fichiers et d'ajouter le nom dans `SimulationTest.validFile`.

## Structure

Toutes les classes sont dans le package `fr.paulm.projects.rover`.

| Package | Classe | Rôle |
|---|---|---|
| *(racine)* | `Main` | point d'entrée : lit le fichier passé en argument et gère les erreurs |
| `domain.enums` | `Direction` | les quatre orientations, les rotations et le vecteur de déplacement |
| | `Instruction` | les ordres `L`, `R` et `M` |
| `domain` | `Plateau` | les limites de la grille |
| | `Rover` | position, orientation et exécution des instructions |
| | `SimulationInput`, `RoverInstructions` | le plateau, et chaque rover avec ses instructions ; vérifie que chaque rover démarre sur le plateau |
| | `Simulation` | fait avancer chaque rover et affiche sa position finale |
| `domain.exceptions` | `InvalidInputException` | le contenu de l'entrée est invalide (format ou règle) ; le message explique l'erreur |
| `input` | `SimulationReader` | interface : une entrée possible de la simulation (fichier texte, et plus tard JSON, base de données…) |
| | `TextFileReader` | lit et valide un fichier au format texte |
