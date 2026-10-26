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
Entrée invalide : Direction invalide : X
```

## Prérequis

- Java 21
- Maven

## Utilisation

```bash
mvn package
java -cp target/Rover-1.0-SNAPSHOT.jar fr.paulm.projects.Main src/main/resources/input.txt
```

## Tests

```bash
mvn test
```

Les tests de `Mission` lisent des fichiers d'entrée dans `src/test/resources/missions/` :

- `valid/<nom>.txt` : entrée d'un cas nominal, comparée au résultat attendu dans `<nom>.expected.txt` ;
- `invalid/<nom>.txt` : entrée d'un cas d'erreur, dont le message attendu est indiqué dans `MissionTest`.

Pour ajouter un cas nominal, il suffit de créer les deux fichiers et d'ajouter le nom dans `MissionTest.validMission`.

## Structure

| Classe | Rôle |
|---|---|
| `Direction` | les quatre orientations, les rotations et le vecteur de déplacement |
| `Plateau` | les limites de la grille |
| `Rover` | position, orientation et exécution des instructions |
| `Mission` | lit le fichier, fait avancer chaque rover et affiche les positions finales |
| `Main` | point d'entrée : lit le fichier passé en argument et gère les erreurs |
