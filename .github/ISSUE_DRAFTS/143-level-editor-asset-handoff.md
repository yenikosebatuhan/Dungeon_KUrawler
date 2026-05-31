# Issue 143 Level Editor Asset Handoff

Bu not, level editor asset entegrasyonunda Codex tarafında yapılan işleri ve Claude'un devam ederken bilmesi gereken teknik borçları özetler.

## Kapsam

Amaç, `src/main/resources/assets/` altına eklenen pre-sliced asset klasörlerini level editor palette'ine almak, map üzerinde doğru PNG render etmek, editor ile oyun runtime'ının aynı model/sprite kararlarını kullanmasını sağlamak ve static dekorlar dışındaki assetlere gerçek oyun içi özellik bağlamaktı.

Bu süreçte dokunulan ana alanlar:

- `src/main/java/com/kurawler/ui/TileRenderer.java`
- `src/main/java/com/kurawler/ui/EditorView.java`
- `src/main/java/com/kurawler/ui/GameView.java`
- `src/main/java/com/kurawler/model/ResourceAssetCatalog.java`
- `src/main/java/com/kurawler/model/object/StaticContainer.java`
- `src/main/java/com/kurawler/editor/LevelEditor.java`
- `src/main/java/com/kurawler/editor/MapEditor.java`
- `src/main/java/com/kurawler/persistence/MapSerializer.java`
- `src/main/java/com/kurawler/persistence/MapDeserializer.java`
- `src/main/java/com/kurawler/model/character/Assassin.java`
- `src/main/java/com/kurawler/model/character/Guard.java`
- `src/main/java/com/kurawler/model/character/Character.java`

## Yeni Resource Asset Kataloğu

`ResourceAssetCatalog` eklendi. Bu sınıf, pre-sliced resource folder assetlerinin model karşılığını merkezi olarak çözüyor.

Klasörler:

- `assets/items/tile000.png` ... `tile023.png`
- `assets/containers/tile000.png` ... `tile023.png`
- `assets/wall and statics/tile000.png` ... `tile239.png`
- `assets/characters/...`

`ResourceAssetCatalog.ITEM_TILES` item tile idlerini anlamlı isimlere bağlıyor:

- Key çeşitleri: `Bone Key`, `Silver Key`, `Gold Key`, `Copper Key`, `Key Ring`, `Brass Key`
- Potion çeşitleri: `Health Potion`, `Mana Potion`, `Energy Potion`
- Ring çeşitleri: `Ruby Ring`, `Emerald Ring`, `Sapphire Ring`
- Treasure/relic: skull, gem, coin, orb
- Scroll/book: `Spell Book`, `Glyph Scroll`, `Rune Scroll`, `Sigil Scroll`, `Curse Scroll`

`ResourceAssetCatalog.CONTAINER_TILES` container tile idlerini isimlere bağlıyor. Örneğin:

- `Blue Banded Chest`, `Gold Banded Chest`
- `Open Blue Chest`, `Open Gold Chest`
- `Full Blue Chest`, `Full Gold Chest`
- `Bronze Treasure Chest`, `Copper Chest`, `Orange Chest`
- `Open Bronze Chest`, `Open Copper Chest`, `Open Orange Chest`

`ResourceAssetCatalog.wallStaticName(tileId)` 30x8 grid mantığıyla wall/static isimleri üretiyor. `isTerrainWall` ve `isTerrainFloor` bu isimlerden editor asset kind seçiyor.

## Editor Palette Entegrasyonu

`TileRenderer.buildEditorCatalog()` artık item/container/wall-static assetlerini eski atlas slicing yerine resource klasörlerinden de kaydediyor:

- `registerItemResourceTiles()`
- `registerContainerResourceTiles()`
- `registerWallAndStaticResourceTiles()`
- `addCharacterAsset(...)`

`atlasRegistry` tek kaynak gibi çalışıyor: palette preview, editor map render ve runtime map render aynı isimden aynı PNG'yi bulabiliyor.

Önemli detay: `addResourceSprite` boş/neredeyse boş tile'ları `hasOpaquePixels(img, 8)` ile filtreliyor.

## Wall/Floor/Static Render Düzeltmeleri

Duvar/statics tarafında ilk sorun, assetlerin grid hücresine oturmamasıydı. Şu an `isResourceCellTile(name)` ile `01-22`, `04-08` gibi suffix taşıyan resource cell tile'ları tespit ediliyor.

Bu tile'lar için davranış:

- Palette preview'de hücreye tam çiziliyor.
- Editor/runtime map'te hücreye tam çiziliyor.
- Transparan alanlarda mavi/dark filler görünmesin diye önce dungeon floor çiziliyor, sonra PNG overlay basılıyor.
- Container parçaları için de aynı mantığa benzer biçimde `drawContainerByName` resource assetleri artık padding ile küçültmüyor, doğrudan `tileSize x tileSize` çiziyor.

Bu sayede bölünmüş duvar, arch, floor ve container parçaları gridde yan yana/alt alta gelince tek parça gibi birleşebiliyor.

## Character Entegrasyonu

Yeni enemy sınıfları:

- `Assassin extends Enemy`
- `Guard extends Enemy`

`Character` sınıfına `setName(String name)` eklendi. Bunun sebebi editor palette varyant adının runtime render ve save/load tarafında korunması.

Editor karakterleri şu an `TileRenderer.buildEditorCatalog()` içinde elle kaydediliyor:

- `Hero`
- `Hero Blonde`
- `Hero Yellow`
- `Knight`
- `Horned Guard Brown`
- `Horned Guard Blue`
- `Sorcerer Purple`
- `Sorcerer Red`
- `Assassin Blue Hood`
- `Assassin Red Hood`

Kullanıcı düzeltmeleri:

- Sarı saçlı çocuk hero.
- Mavi zırhlı olanlar knight.
- Yüzü görünmeyen şapkalı/hooded karakterler assassin.
- `MainChar005.png` ve `MainChar006.png` knight değil; bunlar `Horned Guard Brown/Blue` olarak ayrıldı.
- Stand/portrait olmayan yürüme frame'leri editor'e stand olarak eklenmemeli.

`EditorView.createCharacterFor(...)` asset kind'e göre model oluşturuyor. `applyVariantStats(...)` varyant adına göre attack power set ediyor:

- `Horned Guard Blue`: ATK 6
- `Horned Guard Brown`: ATK 4
- `Hero Yellow`: ATK 8
- `Hero Blonde`: ATK 6
- `Sorcerer Red`: ATK 7
- `Sorcerer Purple`: ATK 5
- `Assassin Red`: ATK 7
- `Assassin Blue`: ATK 5

`GameView` ve `EditorView` render tarafı artık `occupant.getName()` görsel ismini `TileRenderer.drawEnemy(..., visualName)` / `drawHeroByName(...)` ile geçiriyor.

## Character Size Normalization

Karakter assetlerinde farklı canvas/opaque bounds yüzünden mapte boyutlar tutmuyordu. `TileRenderer` içine normalize çizim helperları eklendi:

- `drawCharacterSprite`
- `drawCharacterSpriteMirrored`
- `drawCharacterPreview`
- `drawBottomAlignedFit`

Bu helperlar `SpriteAtlas.opaqueBounds(img)` ile görünür alanı bulup karakteri tile içinde bottom-aligned fit ediyor. Amaç tüm karakterlerin aynı hücre yüksekliği içinde benzer görsel ölçekte durması.

## Container İçine Item Ekleme

Level editor'e container loot editing eklendi.

Akış:

1. Haritaya container koy.
2. Palette'ten item veya weapon seç.
3. Container hücresine tıkla veya sürükle-bırak.
4. Item yere konmak yerine `StaticContainer.contents` içine eklenir.

Teknik değişiklikler:

- `LevelEditor.addItemToContainer(int row, int col, Item item)`
- `MapEditor.addItemToContainer(Item item, int x, int y)`
- `EditorView.applyPayloadAt(...)` ve `applyAtCell(...)` item ise önce container'a eklemeyi dener.
- Container doluysa editor map üzerinde küçük sayı rozeti çiziliyor: `drawContainerLootBadge(...)`

Persistence zaten container `contents` yazıyordu; bu akışla eklenen itemler save/load içinde korunuyor.

## Container Open/Closed Sprite Eşleşmesi

Önce `StaticContainer.getSpriteName()` boş container için sadece `getName() + " open"` yapıyordu. Resource isimleri buna uymadığı için yanlış/fallback sprite çıkıyordu.

Şimdi `StaticContainer.getSpriteName()` şunu çağırıyor:

```java
ResourceAssetCatalog.containerSpriteName(getName(), isEmpty())
```

Örnek eşleşmeler:

- `Blue Banded Chest` empty -> `Open Blue Chest`
- `Gold Banded Chest` empty -> `Open Gold Chest`
- `Bronze Treasure Chest` empty -> `Open Bronze Chest`
- `Copper Chest` empty -> `Open Copper Chest`
- `Orange Chest` empty -> `Open Orange Chest`

Editor ve runtime aynı karar mekanizmasını kullanıyor.

## Item / Wearable / Weapon Model Özellikleri

`ResourceAssetCatalog.createItem(name)` genişletildi. Static objeler hariç assetlerin mümkün olduğunca gerçek model etkisi olsun diye item tarafı ayrıştırıldı.

Şu an:

- `Health Potion`: `Consumable`, HP +25
- `Mana Potion`: `Consumable`, mana +30
- `Energy Potion`: `Consumable`, energy +35
- `Ruby Ring`: `Wearable`, DEF +2
- `Emerald Ring`: `Wearable`, DEF +3
- `Sapphire Ring`: `Wearable`, DEF +4
- `Armor`: `Wearable`, armor slot, DEF +5
- `Shield`: `Wearable`, shield slot, DEF +3
- `Ring`: `Wearable`, ring slot, DEF +1
- Scroll/book/orb: `Scroll`, farklı heal değerleri/text
- Key/gem/skull/coin: şimdilik `Key` üzerinden farklı `keyId`/treasure identity ile taşınıyor

Editor'de weapon üretimi de düzeltildi:

```java
case WEAPON:
    obj = WeaponCatalog.create(name);
```

Böylece tüm weapon assetleri sabit `damage=5` yerine `WeaponCatalog.damageFor(name)` değerini alıyor.

Persistence düzeltmesi:

- `MapSerializer` artık `Consumable` için `healAmount`, `manaAmount`, `energyAmount` yazıyor.
- `MapDeserializer` artık üç değeri de okuyup `new Consumable(name, description, heal, mana, energy)` kullanıyor.

## Save/Load Enemy Varyantları

Enemy save/load genişletildi:

- `MapSerializer.buildEnemyJson(...)` artık `type`, `name`, `atk` yazıyor.
- `MapDeserializer.restoreEnemies(...)` artık `name` ve `atk` restore ediyor.
- `Guard` ve `Assassin` tipleri create/load tarafına eklendi.

Bu sayede `Horned Guard Blue`, `Assassin Red Hood` gibi görsel varyantlar kayıt sonrası kaybolmuyor.

## Bilinen Açık İşler

Kullanıcının son notu: PNG asset sistemi genel olarak başarılı olduğunda kodla çizim fallbackleri azaltılmalı. Özellikle:

1. Potion render tam PNG-first değil.
   - `GameView` halen consumable branch'inde `renderer.drawConsumable(gc, sx, sy, pt)` çağırıyor.
   - `drawConsumable` DC potion PNG'lerini kullanıyor olabilir ama resource folder'daki `assets/items/tile006/007/008` direkt kullanılmıyor.
   - Hedef: yerdeki, inventory'deki ve editor'deki potionlar aynı resource PNG path/name üzerinden çizilmeli.

2. Scroll etkileri sınırlı.
   - Scroll/book/orb şu an `Scroll` modelinin var olan `healAmount` alanına oturtuldu.
   - Farklı scroll tipleri için gerçek farklı gameplay effect isteniyorsa `Scroll` modelinde effect type/enum veya ayrı scroll subclasses gerekebilir.
   - `GameController.readScroll(...)` ve inventory action tarafı incelenmeli.

3. Treasure itemlar hâlâ `Key` ile temsil ediliyor.
   - Gem/skull/coin için dedicated `Treasure` veya `ValuableItem` class daha temiz olur.
   - Şimdilik farklı `keyId` ile ayrışıyorlar, bu teknik olarak çalışır ama semantik olarak ideal değil.

4. Wearable eski kayıt sorunu.
   - Daha önce `Shield`/`Armor` factory fallback nedeniyle `Key` olarak oluşmuştu.
   - Kod düzeldi ama eski kayıttaki yanlış instance'lar yeniden yerleştirilmedikçe key olarak kalabilir.

5. Render fallbackleri hâlâ var.
   - `TileRenderer.drawItem`, `drawConsumable`, bazı static fallbackleri ve weapon fallbackleri kodla şekil/renk çiziyor.
   - Geçiş hedefi: asset varsa her zaman PNG; fallback sadece asset yoksa debug amacıyla kalsın.

6. Static objeler için özel gameplay gerekmedi.
   - Kullanıcı özellikle static objeler hariç dedi.
   - Wall/floor/static render tarafı esasen görsel doğruluk için ele alındı.

## Önemli Dosya Noktaları

- `ResourceAssetCatalog.createItem(...)`: item özelliklerinin ana kaynağı.
- `ResourceAssetCatalog.createContainer(...)`: container lock ve açıklama kaynağı.
- `ResourceAssetCatalog.containerSpriteName(...)`: açık/kapalı sandık sprite eşleşmesi.
- `TileRenderer.buildEditorCatalog(...)`: palette'e hangi assetlerin eklendiği.
- `TileRenderer.spriteForAsset(...)`: isimden PNG lookup fallback sırası.
- `TileRenderer.drawItemObject(...)`: item runtime/editor çizimi için kritik nokta.
- `TileRenderer.drawConsumable(...)`: PNG-first refactor için bakılacak ilk sorunlu yer.
- `EditorView.createObjectFor(...)`: palette asset -> model object dönüşümü.
- `EditorView.applyPayloadAt(...)` ve `applyAtCell(...)`: container içine item ekleme akışı.
- `EditorView.createCharacterFor(...)`: karakter model/varyant üretimi.
- `MapSerializer.buildObjectJson(...)` / `MapDeserializer.deserializeObject(...)`: model özelliklerinin kayıt/geri yükleme tarafı.

## Derleme / Çalıştırma

Testler kullanıcı tarafından güvenilir bulunmadığı için bu turda esas doğrulama compile + JavaFX run ile yapıldı.

Compile:

```bash
mkdir -p /tmp/kurawler-classes && javac --module-path lib/javafx-sdk-21.0.2/lib --add-modules javafx.controls -d /tmp/kurawler-classes -sourcepath src/main/java $(find src/main/java -name "*.java")
```

Run:

```bash
./run.sh
```

Derleme genel olarak geçiyor. Bilinen uyarılar:

- `Hero.java` deprecated annotation warning
- `GameView.java` deprecated API note
- JavaFX native access / Unsafe runtime warningleri

Bu warningler bu asset işiyle ilişkili değil.

