# 🎾 Tennisplatz für Minecraft

Fabric-Mod für **Minecraft Java 26.2**: Echtes Tennis im Multiplayer, mit Plätzen in Originalgröße,
realistischer Ballphysik, Aufschlag mit Ballwurf, Schiedsrichter und Tennis-Outfits.

## Installation

1. [Fabric Loader](https://fabricmc.net/use/) (0.19.5 oder neuer) für Minecraft 26.2 installieren.
2. [Fabric API](https://modrinth.com/mod/fabric-api) (0.161.0+26.2) in den `mods`-Ordner legen.
3. `tennisplatz-1.0.0.jar` in den `mods`-Ordner legen.

Die Mod muss **auf dem Server und bei jedem Spieler** installiert sein (Java 25 wird benötigt, das bringt der Minecraft-Launcher mit).

## Schnellstart

```
/tennis platz bauen rasen       (Server-OP; Belag: rasen, sand oder hartplatz)
/tennis match <Kumpel>          (startet ein Einzel über einen Gewinnsatz)
/tennis match <Kumpel> 2        (zwei Gewinnsätze, "best of three")
```

Der Platz wird dort gebaut, wo du stehst; deine Blickrichtung wird zur Längsachse.
Er braucht rund 39 × 23 Blöcke freie Fläche.

## Steuerung

| Aktion | Taste |
|---|---|
| Ausholen | **Rechtsklick halten**: je länger, desto härter (volle Kraft nach knapp 1 Sekunde) |
| Schlagen | **Rechtsklick loslassen** |
| Aufschlag | Ohne Ball in der Nähe wirft Rechtsklick den Ball hoch. Halten und im höchsten Punkt loslassen: das ist auch der härteste Aufschlag. |
| Zielen | Mit dem **Fadenkreuz** auf den Punkt, an dem der Ball aufkommen soll |
| Slice / Unterschnitt | **Schleichen** beim Schlag (beim Aufschlag: Slice-/Kick-Aufschlag) |
| Lob | Beim Schlag **nach oben schauen** |
| Schneller Block-Schlag | **Linksklick** auf den Ball |

Tempo kostet Genauigkeit: Harte Schläge und schlecht getroffene Bälle streuen mehr.
Eine **Zielhilfe** zieht dein Ziel ins gegnerische Feld, beim Aufschlag ins richtige Aufschlagfeld.
Grob daneben zielen wird also verziehen, ins Aus oder Netz geht ein Ball nur durch Streuung.
Die Anzeige über der Hotbar zeigt nach jedem Schlag Schlagart und Tempo in km/h.

## Was realistisch ist

- **Platz in Originalproportionen**: 25 × 13 Blöcke Spielfeld (echt 23,77 × 10,97 m), Einzel- und Doppellinien,
  Aufschlagfelder, Netz, Auslauf, Zaun und Flutlicht. Linien zählen als „im Feld".
- **Ballphysik**: Schwerkraft, Luftwiderstand und Magnus-Effekt. Topspin taucht ab und springt hoch weg,
  Slice segelt und bleibt flach. Damit man in Minecraft mithalten kann, sind die Bälle etwas langsamer als im Profitennis (Aufschläge bis ca. 125 km/h).
- **Beläge**: Rasen ist schnell und flach, Sand langsam mit hohem Absprung, Hartplatz dazwischen.
- **Aufschlag**: Ballwurf, Treffpunkt entscheidet (im höchsten Punkt = sauber), erster und zweiter Aufschlag,
  Netzaufschlag (Let) wird wiederholt, Doppelfehler, Ass. Aufgeschlagen wird hinter der Grundlinie,
  abwechselnd von rechts und links, diagonal ins Aufschlagfeld.
- **Schiedsrichter**: Aus, Netz, Doppelfehler, Volley-Return beim Aufschlag, zweiter Aufsprung.
  Grüne/rote Markierung zeigt den Aufsprung (wie Hawk-Eye).
- **Zählweise**: 15/30/40, Einstand, Vorteil, Spiele, Sätze, Tiebreak bei 6:6, Seitenwechsel nach ungeraden Spielen,
  Breakball/Satzball/Matchball-Anzeige, Statistik am Ende (Asse, Doppelfehler, schnellster Aufschlag, längster Ballwechsel).

## Tennis-Outfits

Sobald die Mod läuft, bekommt jeder Spieler automatisch ein Tennis-Outfit. Die Figuren sind
**eigene Pixel-Charaktere im Stil berühmter Tennis-Legenden**, keine Abbilder echter Personen:

| Name | Look |
|---|---|
| `rotfuchs` | Der Rotfuchs: rotblonder Wuschelkopf, klassisches Weiß |
| `eismann` | Der Eismann: lange blonde Haare, Stirnband, Nadelstreifen |
| `hitzkopf` | Der Hitzkopf: dunkle Locken, rotes Stirnband |
| `graefin` | Die Gräfin: blonder Pferdeschwanz, weißes Tenniskleid |
| `maestro` | Der Maestro: weißes Bandana, Creme und Gold |
| `matador` | Der Matador: Bandana, ärmelloses Shirt, Dreiviertelhose |
| `koenigin` | Die Königin: lange Zöpfe, kräftiges Lila |

Wechseln mit `/tennis outfit <Name>`, zurück zum eigenen Skin mit `/tennis outfit aus`.

## Alle Befehle

| Befehl | Wirkung |
|---|---|
| `/tennis hilfe` | Kurzanleitung im Chat |
| `/tennis ausruestung` | Schläger und drei Bälle |
| `/tennis platz bauen [rasen\|sand\|hartplatz]` | Platz bauen (OP) |
| `/tennis platz entfernen` | Nächsten Platz abmelden, Blöcke bleiben (OP) |
| `/tennis platz liste` | Alle Plätze |
| `/tennis match <Spieler> [1-3]` | Einzel starten (Zahl = Gewinnsätze) |
| `/tennis stand` | Aktueller Spielstand |
| `/tennis aufgeben` | Match aufgeben |
| `/tennis outfit [liste\|<Name>\|aus]` | Outfit wählen |

Ohne Match kann man frei trainieren: Aufschläge üben (die Mod zeigt „im Feld"/„im Aus" an)
oder sich mit dem Tennisball (Rechtsklick) Bälle zuspielen.

**Rezepte**: Schläger = 4 Faden + 1 Stock, Tennisbälle (4) = Schleimball + gelber Farbstoff + weiße Wolle.

## Entwickeln

```
./gradlew build                 # Mod bauen und Unit-Tests ausführen, Ergebnis in build/libs/
./gradlew runClient             # Testclient starten
./gradlew runClientGameTest     # Spieltest: Dedicated Server + Client, macht Screenshots
python3 tools/generate_textures.py   # Skins, Item-Texturen und Icon neu erzeugen
python3 tools/generate_sounds.py     # Geräusche neu synthetisieren (braucht numpy und ffmpeg)
```
