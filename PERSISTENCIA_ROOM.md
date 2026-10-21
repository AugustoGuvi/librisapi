# LIBRIS — Persistencia con Room (adaptación del manual UPB)

Esta es una **modificación del proyecto LIBRIS original**, no una app nueva. Conserva **Google Books, Open Library y Gutendex**, las pantallas originales y la arquitectura MVVM. Google Books y Open Library (respaldo) ahora se conectan a la base de datos local SQLite mediante Room.
La respuesta inicial se intenta con Google Books; si falla por 403, 429, conexión, etc., se intenta Open Library sin borrar la caché.
La consulta de autor de Open Library y la búsqueda de edición gratuita de Gutendex siguen siendo independientes.

## ¿Qué cumple del manual?

| Manual | En Libris | Ubicación |
|---|---|---|
| Dependencias Room + procesador de anotaciones (paso 1) | Room con kapt | `build.gradle.kts`, `app/build.gradle.kts` |
| Entity de favoritos (paso 2) | Tabla `favorites` | `data/local/FavoriteEntity.kt` |
| DAO de favoritos con Flow, insertar y borrar (paso 3) | `FavoriteDao` | `data/local/FavoriteDao.kt` |
| Database singleton (paso 4) | `LibraryDatabase` | `data/local/LibraryDatabase.kt` |
| Mapeadores (paso 5) | `Book` ↔ entidades; el libro completo se guarda en JSON | `data/mapper/RoomBookMapper.kt` |
| Repositorio (paso 6) | `FavoritesRepository` | `data/repository/FavoritesRepository.kt` |
| ViewModels y estados combinados (paso 7) | `BooksViewModel`, `FavoritesViewModel` | `ui/viewmodel/` |
| Corazones, lista de favoritos y navegación (paso 8) | Búsqueda, detalle, favoritos | `ui/components/BookComponents.kt`, `ui/view/` |
| Probar favoritos (paso 9) | Guardar, borrar, cerrar/abrir, modo avión | lista de pruebas más abajo |
| Entity y DAO para caché (paso 10) | `book_cache` y `search_cache` | `data/local/` |
| Versión y migración (paso 11) | `1 → 2` añade caché | `LibraryDatabase.kt` |
| Mapeadores para caché (paso 12) | `Book.toCacheEntity` y `BookCacheEntity.toBook` | `RoomBookMapper.kt` |
| Repositorio API → Room (paso 13) | `BookRepository.refresh()` | `data/repository/BookRepository.kt` |
| ViewModel observa Room (paso 14) | `observeSaved()` + `flatMapLatest` + `combine` | `BooksViewModel.kt` |
| Portadas ya descargadas sin red (paso 15) | `ImageLoaderFactory` y `respectCacheHeaders(false)` | `MainActivity.kt` (`LibraryApplication`) |
| Probar modo avión (paso 16) | lista persistente, mensaje de error, reintento | pruebas manuales |

### Ejercicios de la última página

1. **Contador** de favoritos en la cabecera de la pantalla principal.
2. **Ordenar** favoritos por fecha o título con dos consultas DAO.
3. **Actualizar caché** con icono de actualizar y **Cargar más** con paginación Google Books (`startIndex`) u Open Library (`offset`) guardada en Room. Primero se obtiene la respuesta de la API; solo entonces se reemplazan los registros dentro de una transacción. Si la red falla, la lista anterior queda intacta.
4. **Notas** personales de favoritos: editor en pantalla Favoritos; `Migration(2,3)` añade columna `note TEXT` sin borrar favoritos.
5. **Detalle sin internet**, adaptado a la temática Libris: la **ficha completa del catálogo** (descripción, editorial, fecha, páginas, ISBN, idioma, portada, etc.) se almacena como `Book` en `book_cache` y también en `favorites`. Se puede abrir sin red. Las consultas *adicionales* a Open Library y Gutendex siguen necesitando internet, igual que los enlaces externos de lectura.

## Arquitectura real

```text
Google Books/Open Library ──> BookRepository.refresh()
                                   |
                                   v
                       Room / libris.db
                 ├── search_cache (consultas)
                 ├── book_cache   (resultados y ficha)
                 └── favorites    (libros favoritos, notas)
                                   |
                             DAO + Flow
                                   |
                  Repositorios (mapeo al modelo Book)
                                   |
                          ViewModels (combine)
                                   |
                 SearchScreen / DetailScreen / FavoritesScreen

Open Library (autores) ──> AuthorRepository ──> AuthorViewModel ──> DetailScreen
Gutendex     (Retrofit) ──> GutenbergRepository -> ReadingViewModel -> DetailScreen
```

La base de datos es **local en Android** (SQLite, gestionada por Room), no MySQL/XAMPP ni un servidor remoto. Para satisfacer el manual es lo adecuado: el PDF pide explícitamente Room. Cada resultado que llega de **Google Books u Open Library** se convierte a `Book`, luego a `BookCacheEntity` y se guarda en Room. **La lista visible nunca se alimenta directamente de la API**, sino del `Flow` de Room.

Se agregó `search_cache` para distinguir entre "todavía no se ha buscado" y "se buscó y hubo cero resultados". La clave primaria compuesta de `book_cache` (`queryKey`, `bookId`) evita borrar resultados de otra consulta al repetir un mismo título. El botón **Cargar más libros** solicita la página siguiente y la **anexa** a la caché en una transacción sin borrar páginas anteriores.

## Versiones y por qué son distintas del ejemplo del manual

El PDF parte de un proyecto antiguo con Kotlin 1.9.24, AGP 8.7.0 y Room 2.6.1. El proyecto LIBRIS adjunto partía de **Kotlin 2.0.21**, **AGP 8.7.3**, **Gradle 8.9** y **compileSdk 35**. Para usar Room compatible con Kotlin más reciente, el proyecto entregado usa **Kotlin/Compose plugin 2.1.21**, **Room 2.7.2**, **AGP 8.7.3**, **Gradle 8.9**, **compileSdk 35** y **JDK 17 o superior**. No se ha reducido el SDK 35 original.

La base se crea directamente en **versión 4** en instalaciones nuevas. Se registran las migraciones **1→2, 2→3 y 3→4** para demostrar cómo evolucionaría si la app ya contara con versiones previas de Room. El ZIP original no tenía Room, por lo que ninguna base de datos previa de Libris necesita migración. No se usa `fallbackToDestructiveMigration()`.

## Cómo ejecutar

1. Descomprime el ZIP actualizado; abre Android Studio en **`Libris_3_APIs`**, la carpeta que contiene `settings.gradle.kts`.
2. Selecciona **Gradle JDK = JDK 17 o 21**; asegúrate de instalar Android SDK Platform **35**.
3. Con conexión a Internet, ejecuta **Sync Project with Gradle Files** y luego **Run ▶**.
4. Desde PowerShell dentro del directorio raíz:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

Nota: el lanzador de Gradle incluido en el ZIP de origen es educativo (no es el wrapper oficial). En el primer arranque necesita conexión a `services.gradle.org` para obtener Gradle 8.9. El proyecto también descarga sus dependencias la primera vez.

**APK si la compilación finaliza correctamente:** `app/build/outputs/apk/debug/app-debug.apk`.

## Pruebas que debes pasar en Android Studio

- [ ] Abre la app con Internet. Busca `Don Quijote`. Aparecen resultados reales y se guardan en `book_cache`.
- [ ] En `View > Tool Windows > App Inspection > Database Inspector`, abre **`libris.db`**, consulta `book_cache` y `search_cache`.
- [ ] Marca tres corazones. Se insertan tres filas en `favorites`.
- [ ] Abre la ficha de un favorito y desmárcalo. Cambia en la pantalla principal y en la de Favoritos.
- [ ] Abre Favoritos y añade una nota. Verifica que `note` cambia en Database Inspector.
- [ ] Alterna la ordenación entre **Más recientes** y **Por nombre**.
- [ ] Cierra completamente la app y vuelve a abrirla. Los favoritos y las notas permanecen.
- [ ] Con los resultados ya guardados, activa modo avión y vuelve a abrir: debe verse la lista que había en Room **sin llamar a Google Books al inicio**.
- [ ] Abre la ficha de un resultado guardado sin conexión. Los datos de Google Books siguen visibles; Open Library y Gutendex indican que no pudieron consultar la red.
- [ ] Con Internet, pulsa **Cargar más libros** y verifica que llegan más libros a la tabla `book_cache` (página siguiente, hasta completar los resultados).
- [ ] En modo avión pulsa **Cargar más libros**: se muestra un error y **los libros previos no desaparecen**.
- [ ] En modo avión pulsa Actualizar: se muestra un error y **los libros previos no desaparecen**.
- [ ] Quita modo avión, pulsa Reintentar/Actualizar y comprueba que se reemplaza la copia local.
- [ ] Primera instalación / datos borrados, con modo avión: no hay datos y se ofrece Reintentar.
- [ ] Ejecuta `RoomPersistenceTest` desde `app/src/androidTest` para probar reinicio de DB, notas, favoritos y reemplazo de cache.

## Limitaciones de comprobación del archivo entregado

Se ejecutó con éxito una **prueba de las sentencias SQL de migración contra SQLite** en el entorno de elaboración. No se pudo ejecutar `assembleDebug` ni las pruebas instrumentadas aquí: no hay SDK de Android, Gradle no está instalado y el entorno no puede resolver `services.gradle.org` para descargar Gradle 8.9. La **compilación en Android Studio está pendiente de verificación**. El ZIP se entrega como proyecto fuente editable, no como APK precompilado.

## Corrección de APIs (comparada con GitHub: AugustoGuvi/3Apis, rama main)

Al introducir Room en un ZIP antiguo se perdieron mejoras que sí tenía la versión funcional de GitHub:

- **Google Books**: llave API opcional en `local.properties` + `BuildConfig.GOOGLE_BOOKS_API_KEY`; Retrofit admite `key` y `startIndex`.
- **Open Library**: vuelve a existir `/search.json` con mapeador de libros y búsqueda de respaldo cuando falla Google Books; también existe `/search/authors.json` para los autores.
- **Gutendex**: cliente HTTP con `Accept: application/json` y `User-Agent`, reintentos por títulos cortos y manejo de 401/403/429.
- **Room**: `search_cache` registra el `source` y `nextOffset` real, de modo que la siguiente página se solicita al servicio que entregó los datos originales. Migración `3 → 4` conserva los datos previos sin borrarlos.
- **UI**: muestra el nombre del catálogo, aviso de respaldo o errores y permite actualizar, reintentar o consultar los libros ya guardados.

La app **no garantiza** datos si ambas fuentes externas están caídas/restringidas y aún no existe caché. Gutendex puede rechazar conexiones en determinadas redes: se mantiene el enlace alternativo a Project Gutenberg para buscar mediante navegador. Las portadas offline solo aparecen si ya se cargaron anteriormente.

### Clave de Google Books (opcional)

Crea/edita `local.properties` dentro del proyecto (no lo subas a GitHub) y añade:

```properties
GOOGLE_BOOKS_API_KEY=TU_CLAVE_DE_GOOGLE_CLOUD
```

Si no defines la clave, se intenta la API sin ella y Open Library queda como respaldo para respuestas fallidas. Asegura una conexión a Internet real desde tu emulador.

### Validación realizada en este entorno

- El `BookRepository.kt` y `BooksViewModel.kt` se compilaron con Kotlin JVM 1.9.0, dependencias simuladas y coroutines; 13 aserciones de Google Books, Open Library, caché, paginación y fallo conjunto superadas.
- `GutenbergRepository.kt` se compiló de forma aislada y superó pruebas simuladas de coincidencia de libro y errores HTTP.
- Se ejecutó una migración equivalente en SQLite `1 → 2 → 3 → 4`; favoritos y búsquedas existentes permanecieron.
- **NO** se compiló la APK Android. Este entorno no tiene Android SDK ni puede descargar la distribución Gradle desde `services.gradle.org` por DNS. Las pruebas anteriores **no sustituyen** a un `assembleDebug` y pruebas en emulador o dispositivo.
