# MC Skin Creator - engine

*MC Skin Creator — Copyright (C) 2026 clixmods — All rights reserved (see [`LICENSE`](LICENSE))*

The computation that turns a list of layers into a 64×64 Minecraft skin texture,
shared by [mcskincreator.app](https://mcskincreator.app/) and the Minecraft mod
so that it is not written twice.

![Java 21](https://img.shields.io/badge/Java-21-orange) ![Dependencies](https://img.shields.io/badge/dependencies-none-brightgreen)

## What it does

You hand it a project — a stack of layers, each one either a catalogue item with
the colours chosen for it, or a buffer drawn pixel by pixel — and it hands back
the texture. On the way it recolours each item through its zone map, applies the
hue/saturation/lightness adjustments, stacks the layers and, if you ask, renders
the front view a thumbnail is made of.

```java
TextureEngine engine = new TextureEngine(mySource);

Project project = new Project(false, List.of(
        new PresetLayer("skin", "skin-uni", Map.of("tone", "#c98a63"), true, 1, null),
        new PresetLayer("hair", "hair-bucheron", Map.of(), true, 1, null)));

int[] texture = engine.texture(project);          // RGBA, 64 * 64 * 4
```

## What it does not do

This is the half of the problem that has no I/O in it. The library **never**:

- encodes or decodes a PNG — `ImageIO` on the server, `NativeImage` in the mod;
- opens a file, or touches the network;
- parses JSON. It has no idea how a project was written down;
- renders anything in 3D.

You give it pixels, it gives you pixels. Everything it cannot compute itself
arrives through one interface:

```java
public interface PresetSource {
    Optional<Preset> preset(String cat, String id);
    Optional<int[]> pixels(Preset preset, boolean slim);
}
```

That seam is the reason the library exists in this shape: the site resolves a
PNG on disk, the mod reads an atlas it downloaded from `/api/atlas` and cached.
Same computation, different supply — and **caching belongs to your
implementation**, which alone knows what a fetch costs and when it goes stale.

The production dependency tree is empty, and `maven-enforcer-plugin` fails the
build if it ever stops being. That is not tidiness: the mod ships this jar inside
its own, and every kilobyte reaches every player.

## What else the two clients share

Composing pixels is the bulk of it, but it is not the only rule the site and the
mod have to agree on. Three more live here, for the same reason: each one fails
*silently* when the two drift apart, and none of them needs a dependency.

- **`Atlas`** — the slot order of a category sheet. The site writes the sheet
  (every item of the category, in catalogue order, each followed by its slim
  variant when it has one); every client cuts it back up by redoing that order.
  Get it wrong by one and each item wears its neighbour's pixels, with nothing
  raising an error. `Atlas.slots(cat, items)` is that order, and
  `Atlas.SLOT_BYTES` the size of one slot;
- **`SkinImport`** — the sizes a `.png` may be imported at, and what comes back.
  64×64 and any whole multiple of it, plus the pre-2013 64×32 layout, which has
  no left arm or leg: the game mirrored the right ones, so importing one unfolds
  it through `Model.mirrorTexel`. An "HD" skin is reduced by taking one texel in
  *f*, never by averaging — averaging invents colours the texel grid does not
  have;
- **the project bounds** — `Project.MAX_LAYERS`, `ProjectLayer.OPACITY_MIN` /
  `OPACITY_MAX`, and the three pairs on `Composition.Adjustments`. The server
  validates a stored project against them and the editors clamp their sliders to
  them; a bound that differs on one side is a project one client writes and the
  other refuses.

None of these decides a pixel, so none of them is covered by the parity rule
below. They are here because they are *shared*, which is the other reason a
line of code belongs in this library.

## Using it

The artifact is `fr.clixmods.mcsc:mcsc-engine`; the repository is called
`mcskincreator-engine`, and the registry URL follows the repository.

**Maven**

```xml
<dependency>
  <groupId>fr.clixmods.mcsc</groupId>
  <artifactId>mcsc-engine</artifactId>
  <version>0.2.0</version>
</dependency>
```

**Gradle, and the Jar-in-Jar trap**

```gradle
dependencies {
    implementation 'fr.clixmods.mcsc:mcsc-engine:0.2.0'
    include        'fr.clixmods.mcsc:mcsc-engine:0.2.0'
}
```

**`include` is not optional under Fabric.** With `implementation` alone the mod
compiles perfectly and then dies in game on a `NoClassDefFoundError`, because
nothing put the library inside the shipped jar. The build gives you no warning
whatsoever; the first sign is a crash report from a player.

**Pin the version, always, and never depend on a `SNAPSHOT`.** Versions are
semantic: a change in what the engine computes raises the minor, a break in the
API raises the major. The site can follow quickly; a mod is installed on
players' machines and has to be able to sit on one version for months.

While the library is in `0.x` the API is not settled: it is `MCSkinCreator#59`,
which makes the site consume it, that will prove it. `1.0.0` follows.

## The parity rule

The value of this code is one property: **it produces exactly the same bytes as
`js/core.js` and `js/assets.js`**, the reference implementation in the
MCSkinCreator repository. If that falls, the mod shows one skin and the site
shows another, and the player is right to complain.

There are three implementations of this computation, held together byte for
byte: the reference in JavaScript, the front-end TypeScript that recolours
sixty times a second while you drag a slider, and this one. A fourth was about
to be written for the mod. **Staying at three is what this repository is for.**

Two consequences that go against ordinary reflexes:

- **nothing gets tidied up on the way.** `Colors.byteOf` exists because writing
  into a `Uint8ClampedArray` rounds halves *to even*, where `Math.round` rounds
  towards +∞. A rewrite that looks equivalent is not, and nothing in this
  repository would notice;
- **a fix is carried to both sides.** Correcting the computation here without
  reporting it into `js/` makes the parity tests of MCSkinCreator fail — which is
  exactly what they are there for.

The tests here run on frozen vectors (`src/test/resources/golden-vectors.txt`),
produced by the reference engine on synthetic items. They catch a regression at
compile time; they are **not** the authority. That is `TextureEngineRepoTest`, in
the private repository, which compares against the real catalogue — 1 848
buffers, 366 fingerprints, every slot of every atlas.

## Where the catalogue lives, and why it is not here

The library is the *how*; the catalogue is the *what*. The PNG files, the zone
maps, the cut-ups and the drawing sources stay in the private MCSkinCreator
repository.

This repository is public because there is nothing to hide in a computation that
is already outside: the TypeScript engine is compiled into the bundle every
visitor downloads, and a mod jar decompiles on the first release. It matters
more that anyone can audit what the mod does with a player's session token.
Public is not licensed, though — see [`LICENSE`](LICENSE).

## Building

```bash
./mvnw -B verify              # compiles and runs the tests
./mvnw -B dependency:tree     # must show nothing outside test scope
```

## Mentions

Unofficial project. Not approved by or associated with Mojang or Microsoft.
"Minecraft" is a trademark of Mojang Synergies AB.
