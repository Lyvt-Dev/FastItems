# Originalanalyse und Portierungsentscheidungen

Quelle: https://github.com/Noryea/fast-items

Analysierter Branch: `1.21(.1)-multi-loader`, Commit `3d754b756e2bf48463e00120b9b4ad4a365af690`, Lizenz CC0-1.0. Der Standardbranch enthält dagegen eine 1.20.1-Version.

Das Original ersetzt das Rendering einer Item-Entity, übernimmt Bobbing und Stack-Darstellung, richtet das Modell zur Kamera aus und steuert den Schattenradius. Sein `SimpleItemModel` filtert die Quads auf `Direction.SOUTH`; die vier booleschen Optionen existieren bereits im Original. Der alte ItemRenderer-Hook kann außerdem andere GROUND-Kontexte beeinflussen.

Für 1.21.11 wurden die offiziellen Mojang-Mappings, die tatsächlichen Klassendeskriptoren und die mit Loom erzeugten Quellen geprüft. Der Port verwendet `ItemEntityRenderer.extractRenderState` und `submit`, die aktiven Layer von `ItemStackRenderState` und die Kameraorientierung aus `CameraRenderState`. Der frühere `BakedModel`-Wrapper wird nicht übernommen.

Der Vanilla-Spin wird durch die Kamera-Quaternion ersetzt. `Camera.setRotation` setzt in dieser Version bereits `PI - yaw`; die zusätzliche 180°-Drehung des Originals darf bei den erhaltenen SOUTH-Quads nicht nochmals angewendet werden. Die Sichtbarkeit wurde deshalb auch mit Screenshots aus zwei entgegengesetzten Kamerapositionen geprüft, statt nur die Quad-Anzahl zu zählen.

Die Filterung erfolgt ausschließlich in der pro Item-Entity aufgelösten Quad-Liste. Globale Ressourcenmodelle und andere Item-Darstellungen werden nicht verändert. Composite-Modelle mit Sonderrenderer und leere Modelle bleiben unverändert. Komponenten, Tint und Glint löst weiterhin Minecraft auf.

Die zu Beginn blockierten Downloads und GitHub-Zugriffe wurden nach der Freigabe erneut erfolgreich geprüft. Für den Build wurde ein checksum-geprüftes vollständiges JDK 21 ergänzt; TLS-Prüfungen bleiben aktiv. Der genaue abschließende Teststand steht in [VALIDATION.de.md](VALIDATION.de.md).
