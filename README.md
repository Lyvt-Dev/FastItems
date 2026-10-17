# Fast Items 1.21.11 Port

Clientseitiger Fabric-Port des Rendering-Prinzips von [Noryea/fast-items](https://github.com/Noryea/fast-items), CC0-1.0. Keine offizielle Veröffentlichung von Noryea. Mod-ID `fastitems`: Original und Port dürfen nicht gleichzeitig installiert werden.

## Bauen und installieren

- Vollständiges **JDK 21**, nicht nur eine JRE.
- **Gradle 9.2.1** wird über den enthaltenen Wrapper automatisch heruntergeladen und mit SHA-256 geprüft.
- Fabric Loom **1.14.10**, Fabric Loader **0.20.0**, Fabric API **0.141.6+1.21.11**.
- Loom 1.14 ist die hier verwendete Java-21-kompatible Linie; neuere Loom-Linien verlangen ein neueres Build-JDK.

```sh
./gradlew build
```

Windows: `gradlew.bat build`. Die installierbare Datei ist
`build/libs/fastitems-port-1.0.0+1.21.11.jar`. Die `-sources.jar` enthält den Quellcode und gehört nicht in den mods-Ordner.

Minecraft **1.21.11** mit Fabric Loader installieren, Fabric API für **1.21.11** und die Port-JAR in den `mods`-Ordner des Clients legen. Keine Installation auf dem Server notwendig. Entwicklung: `./gradlew runClient`.

## Konfiguration

Beim ersten Clientstart entsteht `config/fastitems.json`. Minecraft schließen, Datei bearbeiten und neu starten. Keine zusätzliche Konfigurationsbibliothek oder Menü-Mod erforderlich.

```json
{
  "enabled": true,
  "keepItemShadows": true,
  "renderSidesOf2DItems": false,
  "affect3DModels": true,
  "filterMode": "ALL",
  "itemIds": []
}
```

| Feld | Bedeutung |
| --- | --- |
| `enabled` | Gesamte Optimierung aktivieren |
| `keepItemShadows` | Vanilla-Schatten betroffener Item-Entities behalten |
| `renderSidesOf2DItems` | Seiten und Rückseite flacher Sprites behalten |
| `affect3DModels` | Auch normale 3D-Modelle zur Kamera drehen |
| `filterMode` | `ALL`, `WHITELIST` oder `BLACKLIST` |
| `itemIds` | Beispielsweise `["minecraft:potion", "minecraft:book"]` |

Leere Whitelist beeinflusst keine Items; leere Blacklist erlaubt alle. Sonderrenderer bleiben unabhängig von der 3D-Option unverändert. Ungültige Konfigurationen werden protokolliert und nicht überschrieben; der Client verwendet dann Standardwerte.

## Änderungen gegenüber 1.21/1.21.1

Analysierter Originalbranch: `1.21(.1)-multi-loader`, Commit `3d754b756e2bf48463e00120b9b4ad4a365af690`. Der Defaultbranch des Originals enthält dagegen eine ältere 1.20.1-Version.

Das Original ersetzt `ItemEntityRenderer.render`, löst ein `BakedModel` auf und filtert dessen Quads über einen Wrapper. Diese Architektur wird nicht nachgebildet: Minecraft 1.21.11 trennt `extractRenderState` und `submit`. Modelle werden durch `ItemModelResolver` in die Layer eines `ItemStackRenderState` aufgelöst und über `SubmitNodeCollector` eingereicht. Die Namen und Deskriptoren wurden anhand der offiziellen Mappings und der tatsächlichen 1.21.11-Klassen geprüft.

Der Port verändert nur die fertig aufgelösten Layer einzelner Item-Entities. Die Vanilla-Auflösung von Komponenten, CustomModelData, Modellselektoren, Tint und Glint bleibt erhalten. Die einzelne Spin-Rotation wird durch die Kameraorientierung ersetzt (die Kamera-Quaternion enthält bereits die erforderliche 180°-Basisdrehung). Vanilla behält Bobbing, Stack-Anzahl, Beleuchtung, Umrisse und Submission. Anders als der alte globale GROUND-Kontext betrifft der Port keine Hände, Inventare, Item-Frames oder Item-Displays.

Flache Modelle werden defensiv erkannt: keine Spezialrenderer, Frontbeleuchtung, positive Skalierung, ungedrehter Ground-Transform, Vorder- und Rückseitenquads und höchstens 1/16 Modelleinheit Tiefe. Nur dann werden Seiten und Rückseite entfernt. Dickere oder ungewöhnlich transformierte Modelle behalten ihre Geometrie. Die Optimierung reduziert die eingereichten Quads; sie reduziert weder Entity-Ticks noch die Anzahl gedroppter Items oder grundsätzlich die Draw Calls. Es gibt keine pauschale FPS-Garantie.

## Tests

```sh
./gradlew build compileGametestJava
./gradlew runClientGameTest
```

Der Client-Test benötigt eine grafische Sitzung/OpenGL. Er erstellt eine Testwelt, prüft die aufgelösten Modelle von Trank, Enderperle und Buch auf tatsächlich reduzierte Quad-Zahlen, die Wiederherstellung der Seiten, 3D-Opt-in/Opt-out, Sonderrenderer, leere Stacks, Schattenabschaltung und beide Item-ID-Filter. Anschließend werden echte Item-Entities gerendert und ein Screenshot gespeichert. Testcode liegt ausschließlich in `src/gametest` und wird nicht in die Release-JAR gepackt.

Manuell zusätzlich prüfen:

1. Tränke mit verschiedenen Farben, verzauberte Bücher, Enderperlen und volle Stacks fallen lassen.
2. Kamera von vorne, hinten, oben und unten bewegen; alle vier Optionen einzeln testen.
3. Stein, Werkzeuge und Spezialmodelle wie Truhen, Schilde und Köpfe vergleichen.
4. Ressourcenpakete mit CustomModelData sowie mehrere Modell-Layer prüfen.
5. In einer festen Testszene identische Item-Mengen und Grafikeinstellungen mit aktivierter/deaktivierter Mod vergleichen. Nicht nur das FPS-Maximum, sondern Framezeiten beobachten.

## Kompatibilität und Grenzen

- Sodium/Iris und Shader wurden nicht allein durch einen erfolgreichen Vanilla-Client-Test als kompatibel nachgewiesen. Separat mit den gewünschten Versionen testen.
- Embeddium gehört typischerweise zum NeoForge-Umfeld; dieser Release ist ausschließlich für Fabric. Keine NeoForge-JAR enthalten.
- Andere Mods, die denselben Item-Entity-Spin mit einem Redirect ersetzen, können einen Mixin-Konflikt verursachen.
- Ressourcenpakete mit ungewöhnlichen dünnen Custom-Modellen können anders klassifiziert werden. Für betroffene IDs die Blacklist nutzen oder Seiten aktivieren.
- Spezielle Renderer und nicht sicher erkennbare Modelle werden nicht abgeflacht. Dadurch werden Sonderfälle nicht zwanghaft in ein Sprite umgewandelt.
- Kameraorientierte 3D-Modelle sehen absichtlich anders aus als rotierende Vanilla-Drops.
- Die Mod beschleunigt keine Serverlogik, Hopper, Sortierung, Physik oder Netzwerkübertragung.

Siehe [Validierung](docs/VALIDATION.de.md) für den konkreten Teststand dieses Releases.
