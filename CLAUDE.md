# CLAUDE.md

Ce fichier guide Claude Code (claude.ai/code) dans ce dépôt.

*(**Tout s'écrit en anglais ici** — le code, les commentaires, la Javadoc, le
README, les messages de commit. Ce fichier est la seule exception : c'est un
document de travail interne, et il suit la prose française du dépôt jumeau
`MCSkinCreator`. Le dépôt est **public**, contrairement à son jumeau.)*

## Ce que cette bibliothèque est

`mcsc-engine` **n'est pas un produit**. C'est le calcul qui transforme une liste
de calques en une texture 64×64, sorti de `site/` pour que le mod Minecraft
puisse s'en servir sans qu'on le réécrive une quatrième fois.

Il existe **trois** implémentations de ce calcul, tenues d'accord octet par
octet :

| Implémentation | Où | Rôle |
|---|---|---|
| `js/core.js` + `js/assets.js` | `MCSkinCreator` (privé) | **la référence** — l'atelier, les générateurs, les vérificateurs |
| `site/web/src/app/engine/*.ts` | `MCSkinCreator` (privé) | le navigateur : recolore soixante fois par seconde pendant qu'on fait glisser un curseur |
| **ce dépôt** | public | le serveur, et le mod |

Le mod en aurait demandé une quatrième. **C'est tout l'objet de ce dépôt :
rester à trois.**

Deux noms, et ce n'est pas une étourderie : le **dépôt** s'appelle
`mcskincreator-engine`, l'**artefact Maven** `fr.clixmods.mcsc:mcsc-engine`.
C'est la convention déjà en place — le dépôt `MCSkinCreator` publie
`fr.clixmods.mcsc:mcsc-site`. L'URL du registre suit le **dépôt**.

## La règle de parité, et pourquoi elle prime sur tout le reste

**Ce code rend exactement les mêmes octets que `js/core.js` et `js/assets.js`.**
Si cette propriété tombe, le mod affiche un skin et le site en affiche un autre,
et le joueur a raison de se plaindre.

Elle a des conséquences qui vont contre les réflexes ordinaires :

- **on n'« améliore » rien au passage.** `Colors.byteOf` existe parce qu'une
  écriture dans un `Uint8ClampedArray` arrondit la demie **au pair** là où
  `Math.round` arrondit vers +∞. Un `+ 0.5` qui paraît équivalent ne l'est pas.
  Une simplification cosmétique casse la parité **sans que rien ne le dise ici** ;
- **une correction se reporte des deux côtés.** Corriger le calcul ici sans le
  reporter dans `js/` fait échouer les tests de parité de `MCSkinCreator` — et
  c'est exactement ce qu'on leur demande ;
- **les tests de ce dépôt ne font pas autorité.** Ils tournent sur des cas figés,
  sans catalogue, et attrapent une régression à la compilation. L'autorité est
  `TextureEngineRepoTest`, dans le dépôt privé, qui compare sur le **vrai**
  catalogue — 1 848 tampons, 366 empreintes, chaque emplacement de chaque
  planche. Voir « Les vecteurs figés » plus bas.

## Ce que la bibliothèque n'a pas le droit de contenir

L'arbre des dépendances de production est **vide**, et ce n'est pas une
coquetterie : le mod embarque ce jar dans le sien (Jar-in-Jar), et chaque
kilo-octet part chez chaque joueur.

Sont donc bannis du code de production :

| Interdit | Pourquoi |
|---|---|
| Jackson, Gson, tout parseur | Minecraft embarque Gson, le site Jackson 3. Une signature qui nomme l'un oblige l'autre à l'embarquer pour rien |
| Spring, toute injection | c'est une bibliothèque, pas une application |
| `java.nio.file`, `java.io.File` | le site lit des PNG sur disque, le mod lit des planches en cache. L'approvisionnement est une **interface** |
| `javax.imageio`, toute image | l'encodage PNG est une affaire d'appelant : `ImageIO` côté site, `NativeImage` côté mod |
| le base64 d'un calque de dessin | c'est le format de sérialisation, pas le calcul. L'adaptateur décode |

`maven-enforcer-plugin` le vérifie à chaque build, et **c'est
`bannedDependencies` qui le tient** : il bannit les scopes `compile`, `runtime`
et `provided` en bloc. `banTransitiveDependencies`, qui l'accompagne, ne suffit
pas — il bannit ce qu'une dépendance traîne derrière elle, pas la dépendance
elle-même, si bien qu'un `commons-lang3` ajouté en direct passait au vert. Le
workflow de vérification le redit autrement (`dependency:list
-DincludeScope=runtime`, qui doit rendre « none »), pour que retirer la règle du
pom ne retire pas la garantie en silence.

Attention au piège d'à côté : `dependency:list -DexcludeScope=test` **échoue**
(« excluding everything »), et une étape de CI qui échoue dans un `|| true` se
lit comme un succès.

## Commandes

```bash
./mvnw -B verify              # compile et joue les tests
```

```bash
./mvnw -B dependency:tree     # doit ne rien montrer hors du scope test
```

```bash
# rien d'interdit dans le code de production — doit ne rien rendre
grep -rE "jackson|gson|springframework|java\.nio\.file|javax\.imageio" src/main/java/
```

## Les vecteurs figés

`src/test/resources/golden-vectors.json` porte des empreintes que **le moteur de
référence a réellement produites** — pas ce que le Java calcule de lui-même, ce
qui ne prouverait rien.

Deux points qui ne se devinent pas :

- **ils portent sur des presets synthétiques**, écrits à la main, jamais sur des
  éléments du catalogue : celui-ci ne quitte pas le dépôt privé. Rien du
  catalogue ne doit entrer ici, ni en donnée de test, ni en commentaire ;
- **ils ne se régénèrent pas dans la CI de ce dépôt** : `tools/cutlib.js` est
  privé. Ce sont des données gelées, refaites à la main depuis `MCSkinCreator`
  quand le calcul change — et si le calcul change, c'est qu'on est en train de
  toucher à la parité, donc de faire quelque chose qui se décide.

## Flux de travail Git

Deux branches longues, comme le dépôt jumeau :

- **`main`** — les versions publiées. Un tag `v*` y déclenche la publication de
  l'artefact. On n'y travaille jamais directement ;
- **`develop`** — la branche d'intégration, base de tout nouveau travail.

**Ne jamais committer directement sur `main` ni sur `develop`.** Chaque tâche
part de `develop` sur une branche `feature/` (ou `fix/`) :

```bash
git checkout develop && git checkout -b feature/short-name
```

Une fois le travail **vérifié**, fusion en conservant la trace de la branche,
puis suppression des deux côtés, sans demander :

```bash
git checkout develop && git merge --no-ff feature/short-name && git branch -d feature/short-name
git push origin --delete feature/short-name
```

`-d` et jamais `-D` : il refuse ce qui n'est pas fusionné, et c'est le garde-fou
voulu. `develop` n'est fusionnée dans `main` que sur demande explicite — c'est
une publication. Et **ne jamais pousser sans que ça ait été demandé**, la
suppression d'une branche fusionnée étant la seule exception.

**En session cloud** : la branche automatique (`claude/…`) se renomme en
`feature/nom-court` **avant le premier commit**. Ce renommage ne se demande pas.

## Versionnage

Sémantique, et **jamais de `SNAPSHOT`** : le site peut suivre vite, le mod est
installé chez des joueurs et doit pouvoir rester des mois sur une version. Les
consommateurs pincent la version, toujours.

- un changement de comportement du moteur monte la **mineure** ;
- une rupture d'API monte la **majeure**.

On est en `0.x` tant que `MCSkinCreator#59` n'a pas consommé cette API pour de
vrai : c'est lui qui l'éprouve. **1.0.0 quand il est vert.**

## Où vit le catalogue, et pourquoi pas ici

La bibliothèque est le **comment**, le catalogue est le **quoi**. Les PNG, les
cartes de zones, les découpages, les sources de dessin restent dans
`MCSkinCreator`, qui est privé — c'est là qu'est la valeur. Ici il n'y a que du
calcul, et le calcul est de toute façon dehors : le moteur TypeScript est déjà
compilé dans le bundle que chaque visiteur télécharge, et un jar de mod se
décompile dès la première release.
