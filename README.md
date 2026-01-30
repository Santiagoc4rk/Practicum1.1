# Proyecto PracticumMovies1.1

## TABLA DE CONTENIDOS

### Ítems Desarrollados
1. [Estructura del Proyecto](#1-estructura-del-proyecto)
2. [Modelos de Datos](#21-modelos-de-datos)
3. [Configuración de Base de Datos](#22-configuración-de-base-de-datos)
4. [Capa de Acceso a Datos (DAO)](#23-capa-de-acceso-a-datos-dao)
5. [Utilidades de Procesamiento](#24-utilidades-de-procesamiento)
6. [Proceso ETL Implementado](#3-proceso-etl-implementado)
7. [Tecnologías y Librerías Utilizadas](#4-tecnologías-y-librerías-utilizadas)
8. [Esquema de Base de Datos Relacional](#5-esquema-de-base-de-datos-relacional)
9. [Flujo de Ejecución](#6-flujo-de-ejecución)
10. [Comandos de Ejecución](#7-comandos-de-ejecución)
11. [Salida Esperada del Programa](#8-salida-esperada-del-programa)
12. [Mejoras Futuras](#9-mejoras-futuras)
13. [Conclusiones](#10-conclusiones)

---

## 1. Estructura del Proyecto
```
PracticumMovies1.1/
├── build.sbt
├── README.md
├── avances/
│   ├── avance_1.md
│   ├── avance_2.md
│   ├── avance_3.md
│   └── criterios_calificacion.md
├── src/
│   └── main/
│       ├── resources/
│       │   ├── data/
│       │   │   └── pi-movies-complete-2026-01-14.csv
│       │   └── application.conf
│       └── scala/
│           ├── config/
│           │   └── Database
│           ├── dao/
│           │   ├── SentenciasSQL
│           │   ├── moviesDAO
│           │   ├── castDAO
│           │   ├── crewDAO
│           │   ├── collectionDAO
│           │   ├── companiesDAO
│           │   ├── countriesDAO
│           │   ├── genresDAO
│           │   ├── keywordsDAO
│           │   ├── languagesDAO
│           │   └── rateDAO
│           ├── models/
│           │   ├── Movie
│           │   ├── Movie_Raw
│           │   └── MovieJson
│           ├── utilities/
│           │   ├── LimpiarJSON.scala
│           │   ├── NormalizarJSON.scala
│           │   ├── ParsearJSON.scala
│           │   ├── MovieConverter.scala
│           │   ├── ProcesadorPeliculas.scala
│           │   └── ValidacionMovies.scala
│           └── Main.scala
└── target/
```

## 2. Componentes Desarrollados

### 2.1 Modelos de Datos

#### Movie.scala
Entidad normalizada que representa una película en la base de datos. Contiene 19 campos esenciales:
- `id_movie` (Int) - Identificador único de la película
- `imdb` (String) - Código IMDB
- `status` (String) - Estado de la película (Released, Post Production, etc.)
- `vote_count` (Int) - Número total de votos
- `revenue` (Double) - Ingresos generados
- `vote_average` (Double) - Puntuación promedio (0-10)
- `title` (String) - Título de la película
- `tagline` (String) - Eslogan promocional
- `video` (Boolean) - Indica si es un video
- `popularity` (Double) - Índice de popularidad
- `runtime` (Int) - Duración en minutos
- `adult` (Boolean) - Contenido para adultos
- `oficial_language` (String) - Idioma original
- `poster_path` (String) - Ruta del póster
- `backdrop_path` (String) - Ruta de imagen de fondo
- `overview` (Text) - Sinopsis de la película
- `original_title` (String) - Título original
- `budget` (Int) - Presupuesto de producción
- `homepage` (String) - Sitio web oficial

#### Movie_Raw.scala
Mapeo directo del archivo CSV con 28 columnas originales. Incluye columnas JSON anidadas:
- Campos simples: adult, budget, homepage, id, imdb_id, original_language, etc.
- Campos JSON: belongs_to_collection, genres, production_companies, production_countries, spoken_languages, keywords, cast, crew, ratings

#### MovieJson.scala
Define 9 case classes para representar estructuras JSON anidadas:
- `collection` - Información de colecciones (id, name, backdrop_path, poster_path)
- `genres` - Géneros cinematográficos (id, name)
- `countries` - Países de producción (iso_3166_1, name)
- `companies` - Compañías productoras (id, name)
- `languages` - Idiomas (iso_639_1, name)
- `keywords` - Palabras clave (id, name)
- `rate` - Valoraciones de usuarios (userId, rating, timestamp)
- `Crew` - Equipo técnico (credit_id, department, gender, id, job, name, profile_path)
- `Cast` - Reparto (cast_id, character, id, credit_id, gender, order, name, profile_path)

### 2.2 Configuración de Base de Datos

#### Database.scala
Configuración del transactor de Doobie usando HikariCP para gestión eficiente de conexiones:
- Pool de conexiones configurado para alto rendimiento
- ExecutionContext global para operaciones asíncronas
- Carga de configuración desde application.conf
- Resource-safe para manejo automático de cierre de conexiones

#### application.conf
Archivo de configuración con parámetros de conexión:
```
db {
  driver = "com.mysql.cj.jdbc.Driver"
  url = "jdbc:mysql://localhost:3306/movies_db"
  user = "usuario"
  password = "contraseña"
}
```

### 2.3 Capa de Acceso a Datos (DAO)

#### SentenciasSQL.scala
Gestiona las operaciones DDL de la base de datos:
- `dropTablas()` - Elimina todas las tablas en orden correcto (CASCADE)
- `crearTablas()` - Crea 18 tablas con claves primarias y foráneas
- `inicializarBaseDatos()` - Ejecuta drop y create en secuencia
- Define esquema completo de base de datos relacional normalizada

#### moviesDAO.scala
Operaciones CRUD para la tabla principal de películas:
- `insert(m: Movie)` - Inserta película con INSERT IGNORE
- `consultarID(id: Int)` - Verifica existencia de película por ID

#### castDAO.scala
Gestión de actores y reparto:
- `insertP(c: Cast)` - Inserta persona en tabla people
- `insertCT(c: Cast, id: Int)` - Inserta relación en tabla casting con personaje y orden
- `consultarPeople(id: Option[Int])` - Verifica existencia de persona

#### crewDAO.scala
Gestión de equipo técnico:
- `insertP(c: Crew)` - Inserta persona en tabla people
- `insertCW(c: Crew, id: Int)` - Inserta relación en tabla crewing con trabajo y departamento
- `consultarPeople(id: Option[Int])` - Verifica existencia de persona

#### genresDAO.scala
Gestión de géneros cinematográficos:
- `insert(g: genres)` - Inserta género en catálogo
- `insertCG(g: genres, id: Int)` - Relaciona género con película
- `consultarGenre(iso: Option[String])` - Verifica existencia de género

#### keywordsDAO.scala
Gestión de palabras clave:
- `insert(k: keywords)` - Inserta palabra clave
- `insertDBib(k: keywords, id: Int)` - Relaciona keyword con película
- `consultarKeyWord(id_keyword: Option[Int])` - Verifica existencia

#### companiesDAO.scala
Gestión de compañías productoras:
- `insert(c: companies)` - Inserta compañía
- `insertPCY(c: companies, id: Int)` - Relaciona compañía con película
- `consultarCompany(id_company: Option[Int])` - Verifica existencia

#### countriesDAO.scala
Gestión de países de producción:
- `insert(c: countries)` - Inserta país
- `insertPCT(c: countries, id: Int)` - Relaciona país con película
- `consultarCountry(iso: Option[String])` - Verifica existencia

#### languagesDAO.scala
Gestión de idiomas:
- `insert(l: languages)` - Inserta idioma
- `insertSP(l: languages, id: Int)` - Relaciona idioma con película
- `consultarLanguage(iso: Option[String])` - Verifica existencia

#### collectionDAO.scala
Gestión de colecciones de películas:
- `insert(c: collection)` - Inserta colección
- `insertBC(c: collection, id: Int)` - Relaciona película con colección
- `consultarCollection(id_collection: Option[Int])` - Verifica existencia

#### rateDAO.scala
Gestión de valoraciones:
- `insert(u: rate)` - Inserta usuario
- `insertRT(r: rate, id: Int)` - Inserta valoración con rating y timestamp

### 2.4 Utilidades de Procesamiento

#### LimpiarJSON.scala
Limpieza y normalización de strings JSON malformados provenientes del CSV:
- `prepararJSONParaParseo(jsonRaw: String, tipo: String)` - Limpieza completa en dos fases
- `limpiezaGeneralSQL(rawJson: String)` - Reemplaza None→null, True→true, False→false, '→"
- `aplicarParchesEspecificos(json: String, tipoDato: String)` - Parches por tipo de columna
- `normalizarTexto(txt: String)` - Limpia espacios y normaliza strings
- `parseCSVLine(line: String)` - Parsea líneas CSV respetando comillas

Parches específicos por columna:
- Cast/Crew: Arregla comillas dentro de nombres
- Companies: Corrige falta de comas entre objetos
- Countries: Corrige objetos mal cerrados

#### ParsearJSON.scala
Parseo de 9 columnas JSON a case classes usando Circe:
- `parsearCrewDeCelda(crewStr: String)` - Parsea equipo técnico
- `parsearCastDeCelda(castStr: String)` - Parsea reparto
- `parsearGenresDeCelda(genresStr: String)` - Parsea géneros
- `parsearKeywordsDeCelda(keywordsStr: String)` - Parsea palabras clave
- `parsearCompaniesDeCelda(companiesStr: String)` - Parsea compañías
- `parsearCountriesDeCelda(countriesStr: String)` - Parsea países
- `parsearLanguagesDeCelda(languagesStr: String)` - Parsea idiomas
- `parsearCollectionDeCelda(collectionStr: String)` - Parsea colecciones
- `parsearRateDeCelda(celda: String)` - Parsea valoraciones (array u objeto único)

Todos los métodos:
- Validan strings vacíos y "[]"
- Aplican limpieza antes de parsear
- Retornan List.empty en caso de error
- Normalizan entidades después de parsear

#### NormalizarJSON.scala
Normalización de texto dentro de estructuras JSON:
- `normalizarCollection(c: collection)` - Normaliza campos de colección
- `normalizarGenres(g: genres)` - Normaliza nombres de géneros
- `normalizarCountries(c: countries)` - Normaliza nombres de países
- `normalizarCompanies(c: companies)` - Normaliza nombres de compañías
- `normalizarLanguages(l: languages)` - Normaliza nombres de idiomas
- `normalizarRate(r: rate)` - Mantiene valores numéricos
- `normalizarKeywords(k: keywords)` - Normaliza palabras clave
- `normalizarCrewMember(c: Crew)` - Normaliza información de crew
- `normalizarCast(c: Cast)` - Normaliza información de cast

#### ValidacionMovies.scala
Validación de integridad y limpieza de datos:

**Validación:**
- `esPeliculaValida(m: Movie_Raw)` - Valida campos críticos:
    - ID mayor a 0
    - Título válido y no nulo
    - Fecha en formato YYYY-MM-DD
    - Idioma válido

**Limpieza:**
- `limpiarUnaPelicula(m: Movie_Raw)` - Normaliza registro completo:
    - Convierte negativos a positivos
    - Limita vote_average a máximo 10.0
    - Normaliza strings vacíos y nulos
    - Corrige fechas inválidas a 1900-01-01
    - Normaliza booleanos a "true"/"false"
    - Valida códigos de idioma (2-3 caracteres)

**Helpers:**
- `isValidString(s: String)` - Valida strings no nulos y no vacíos
- `isValidOptionalUrl(s: String)` - Valida URLs opcionales
- `isValidOptionalImdbId(s: String)` - Valida IDs de IMDB (formato tt)
- `isValidBooleanString(s: String)` - Valida representaciones booleanas
- `limpiarNumerico()` - Convierte negativos a positivos
- `limpiarString()` - Limpia con valor por defecto
- `limpiarBooleano()` - Normaliza a "true"/"false"
- `limpiarFecha()` - Valida formato YYYY-MM-DD
- `limpiarIdioma()` - Valida código ISO 639

#### MovieConverter.scala
Conversión de Movie_Raw a Movie normalizado:
- `rawToMovie(raw: Movie_Raw)` - Transforma 28 campos crudos a 19 campos normalizados
- `parseBoolean(s: String)` - Convierte strings a booleanos (true/1 → true)
- Maneja conversiones numéricas con toInt y toDouble
- Retorna Option[Movie] para manejo seguro de errores

#### ProcesadorPeliculas.scala
Orquestación de inserción transaccional completa:
- `procesarPelicula(m: Movie, raw: Movie_Raw)` - Programa ConnectionIO que:
    1. Parsea 9 columnas JSON en memoria
    2. Inserta película principal
    3. Inserta catálogos (collection, genres, companies, countries, languages, keywords)
    4. Inserta personas (cast y crew en tabla people)
    5. Inserta relaciones en tablas intermedias
    6. Inserta valoraciones de usuarios

Usa `.traverse` para procesamiento funcional y `.void` para ignorar resultados individuales.

### 2.5 Punto de Entrada

#### Main.scala
Programa principal que ejecuta el proceso ETL completo:
1. Crea transactor de base de datos con resource-safe
2. Elimina y recrea tablas
3. Lee CSV con FS2 streaming usando separador ';'
4. Valida y limpia registros crudos
5. Convierte Movie_Raw a Movie
6. Procesa en lotes de 100 películas
7. Ejecuta transacciones por lote
8. Muestra progreso en consola
9. Maneja errores globalmente

## 3. Proceso ETL Implementado

### 3.1 Inicialización de Base de Datos

**Eliminación de Tablas (DROP CASCADE):**
Se eliminan en orden inverso a las dependencias para evitar conflictos de foreign keys:
1. Tablas de relación (rate, casting, crewing, identify_by, etc.)
2. Tablas de catálogo (user, people, keywords, languages, etc.)
3. Tabla principal (movies)

**Creación de Tablas:**
Se crean 18 tablas con el siguiente esquema:

**Tablas Principales:**
- `movies` - 19 campos con información principal de películas
- `people` - 5 campos para actores y equipo técnico
- `user` - 1 campo para identificación de usuarios

**Tablas de Catálogo:**
- `genres` - Géneros cinematográficos (iso INT, name VARCHAR)
- `keywords` - Palabras clave (id_keyword INT, name VARCHAR)
- `companies` - Compañías productoras (id_company INT, name VARCHAR)
- `countries` - Países (iso VARCHAR, name VARCHAR)
- `languages` - Idiomas (iso VARCHAR, name VARCHAR)
- `collection` - Colecciones (id_collection BIGINT, name, backdrop_path, poster_path)

**Tablas de Relación Many-to-Many:**
- `contains_genres` - Películas ↔ Géneros
- `identify_by` - Películas ↔ Keywords
- `producer_companies` - Películas ↔ Compañías
- `producer_countries` - Películas ↔ Países
- `spoken_languages` - Películas ↔ Idiomas
- `belongs_to_collection` - Películas ↔ Colecciones
- `casting` - Películas ↔ Actores (con cast_id, personage, order_positions)
- `crewing` - Películas ↔ Crew (con job, department)
- `rate` - Películas ↔ Usuarios (con rating, timestamp)

### 3.2 Lectura de Datos

**Streaming con FS2:**
```scala
Files[IO]
  .readAll(filePath)
  .through(text.utf8.decode)
  .through(decodeUsingHeaders[Movie_Raw](';'))
```

**Características:**
- Lectura eficiente en streaming (no carga todo en memoria)
- Decodificación automática usando FS2 Data CSV
- Separador personalizado (punto y coma)
- Conversión directa a case class Movie_Raw

### 3.3 Validación y Limpieza de Datos

**Validaciones Críticas:**
- ID debe ser mayor a 0
- Título no puede ser nulo o vacío
- Fecha debe estar en formato YYYY-MM-DD
- Idioma debe tener 2-3 caracteres

**Limpiezas Aplicadas:**
- Valores negativos → Valores absolutos (budget, revenue, runtime, etc.)
- vote_average > 10.0 → 10.0
- Strings nulos o vacíos → Valores por defecto
- Fechas inválidas → "1900-01-01"
- Booleanos inconsistentes → "true"/"false"
- Idiomas inválidos → "en"
- Espacios extra → Eliminados con trim

**Proceso:**
```scala
val moviesToProcess = rawMovies.flatMap { raw =>
  if (ValidacionMovies.esPeliculaValida(raw)) {
    val rawLimpio = ValidacionMovies.limpiarUnaPelicula(raw)
    MovieConverter.rawToMovie(rawLimpio).map(m => (m, rawLimpio))
  } else None
}
```

### 3.4 Parseo de Columnas JSON

**Limpieza de JSON Malformado:**
1. Reemplazo de sintaxis Python:
    - `None` → `null`
    - `True` → `true`
    - `False` → `false`
    - Comillas simples → Comillas dobles

2. Corrección de estructura:
    - Asegurar corchetes de apertura y cierre
    - Corregir comillas dentro de nombres
    - Añadir comas faltantes entre objetos

3. Parches específicos por tipo:
    - Cast/Crew: Arreglar nombres con caracteres especiales
    - Companies: Corregir objetos concatenados
    - Countries: Corregir objetos mal cerrados

**Parseo con Circe:**
```scala
decode[List[Cast]](jsonLimpio) match {
  case Right(items) => items.map(normalizarCast)
  case Left(_) => List.empty[Cast]
}
```

**Manejo de Errores:**
- Strings vacíos → List.empty
- JSON inválido → List.empty (no interrumpe proceso)
- Normalización posterior al parseo exitoso

### 3.5 Conversión y Transformación

**De Movie_Raw (28 campos) a Movie (19 campos):**
- Conversión de tipos (Double → Int, String → Boolean)
- Renombrado de campos (original_language → oficial_language)
- Eliminación de campos JSON anidados (manejados aparte)
- Uso de campos calculados (poster_path usado para backdrop_path)

**Filtrado:**
Solo se procesan películas que pasan todas las validaciones, asegurando integridad de datos en BD.

### 3.6 Inserción en Base de Datos

**Procesamiento por Lotes:**
```scala
val batchSize = 100
val batches = moviesToProcess.grouped(batchSize).toList
```

**Orden de Inserción por Película:**
1. Película principal (movies)
2. Catálogos (collection, genres, companies, countries, languages, keywords, users)
3. Personas (people para cast y crew)
4. Relaciones (tablas intermedias)
5. Valoraciones (rate)

**Transaccionalidad:**
- Cada lote se ejecuta en una sola transacción
- Si falla una película, no afecta al resto del lote (INSERT IGNORE)
- ConnectionIO permite composición funcional de operaciones

**Ventajas del Modelo por Lotes:**
- Reduce sobrecarga de transacciones
- Mejora rendimiento significativamente
- Facilita seguimiento de progreso
- Permite reintentos granulares en caso de fallo

## 4. Tecnologías y Librerías Utilizadas

### 4.1 Lenguaje y Plataforma
- **Scala 3** - Lenguaje funcional con sistema de tipos avanzado
- **SBT (Scala Build Tool)** - Gestión de dependencias y compilación
- **JVM 11+** - Plataforma de ejecución

### 4.2 Programación Funcional
- **Cats Effect 3** - Sistema de efectos para programación funcional
    - IO monad para operaciones con side effects
    - Resource para manejo seguro de recursos
    - IOApp para aplicaciones funcionales

- **Cats Core** - Abstracciones funcionales
    - traverse para iteración funcional
    - implicits para operadores funcionales

### 4.3 Acceso a Datos
- **Doobie 1.0** - Librería funcional para JDBC
    - ConnectionIO para composición de queries
    - Transactor para ejecución de transacciones
    - Fragment para construcción segura de SQL
    - Free monad para separación de descripción y ejecución

- **HikariCP** - Pool de conexiones de alto rendimiento
    - Gestión eficiente de conexiones
    - Monitoreo y métricas
    - Configuración optimizada

### 4.4 Procesamiento de Datos
- **FS2 (Functional Streams 2)** - Streaming funcional
    - Stream para procesamiento lazy
    - Pipes para transformaciones
    - Resource-safe por diseño

- **FS2 Data CSV** - Parseo de archivos CSV
    - Decodificación automática a case classes
    - Soporte para headers
    - Separadores personalizados

- **Circe** - Librería JSON
    - Parseo automático con generic.auto
    - Codecs para case classes
    - Manejo de errores con Either

### 4.5 Base de Datos
- **MySQL 8.0+** - Sistema de gestión de bases de datos
    - Soporte para transacciones ACID
    - Foreign keys y constraints
    - Optimizaciones de queries

- **MySQL Connector/J** - Driver JDBC para MySQL
    - Implementación del protocolo MySQL
    - Pool de conexiones compatible

### 4.6 Configuración
- **Typesafe Config** - Manejo de archivos de configuración
    - Formato HOCON
    - Carga de application.conf
    - Valores por defecto

## 5. Esquema de Base de Datos Relacional

### 5.1 Diagrama de Relaciones
```
movies (1) ----< (N) contains_genres >---- (N) genres
movies (1) ----< (N) identify_by >---- (N) keywords
movies (1) ----< (N) producer_companies >---- (N) companies
movies (1) ----< (N) producer_countries >---- (N) countries
movies (1) ----< (N) spoken_languages >---- (N) languages
movies (1) ----< (N) belongs_to_collection >---- (N) collection
movies (1) ----< (N) casting >---- (N) people
movies (1) ----< (N) crewing >---- (N) people
movies (1) ----< (N) rate >---- (N) user
```

### 5.2 Descripción de Tablas

**movies** - Tabla principal de películas
- id_movie (INT, PK) - Identificador único
- imdb (VARCHAR) - Código IMDB
- status (VARCHAR) - Estado de producción
- vote_count (INT) - Total de votos
- revenue (DECIMAL) - Ingresos
- vote_average (DECIMAL) - Puntuación promedio
- title (VARCHAR) - Título
- tagline (VARCHAR) - Eslogan
- video (BOOLEAN) - Es video
- popularity (DECIMAL) - Índice de popularidad
- runtime (INT) - Duración
- adult (BOOLEAN) - Contenido adulto
- oficial_language (VARCHAR) - Idioma original
- poster_path (VARCHAR) - Ruta póster
- backdrop_path (VARCHAR) - Ruta fondo
- overview (TEXT) - Sinopsis
- original_title (VARCHAR) - Título original
- budget (INT) - Presupuesto
- homepage (VARCHAR) - Sitio web

**people** - Actores y equipo técnico
- id_people (INT, PK)
- gender (INT) - Género (0,1,2)
- name (VARCHAR) - Nombre completo
- profile_path (VARCHAR) - Ruta imagen perfil
- credit_id (VARCHAR) - ID de crédito

**user** - Usuarios que califican
- user_id (INT, PK)

**genres** - Géneros cinematográficos
- iso (INT, PK)
- name (VARCHAR)

**keywords** - Palabras clave descriptivas
- id_keyword (INT, PK)
- name (VARCHAR)

**companies** - Compañías productoras
- id_company (INT, PK)
- name (VARCHAR)

**countries** - Países de producción
- iso (VARCHAR, PK) - Código ISO 3166-1
- name (VARCHAR)

**languages** - Idiomas
- iso (VARCHAR, PK) - Código ISO 639-1
- name (VARCHAR)

**collection** - Colecciones de películas
- id_collection (BIGINT, PK)
- name (VARCHAR)
- backdrop_path (VARCHAR)
- poster_path (VARCHAR)

**casting** - Reparto de películas
- id_people (INT, FK)
- id_movie (INT, FK)
- cast_id (INT)
- personage (VARCHAR) - Personaje interpretado
- order_positions (INT) - Orden de aparición
- PK: (id_people, id_movie, cast_id)

**crewing** - Equipo técnico
- id_people (INT, FK)
- id_movie (INT, FK)
- job (VARCHAR) - Trabajo específico
- department (VARCHAR) - Departamento
- PK: (id_people, id_movie)

**rate** - Valoraciones de usuarios
- id_movie (INT, FK)
- user_id (INT, FK)
- rating (DECIMAL) - Puntuación
- timestamp (BIGINT) - Momento de valoración
- PK: (id_movie, user_id)

### 5.3 Normalización

El esquema está normalizado en **Tercera Forma Normal (3NF)**:

**1NF:** Todos los atributos son atómicos (no hay listas ni JSON en BD)

**2NF:** No hay dependencias parciales (todas las claves foráneas dependen de la clave primaria completa)

**3NF:** No hay dependencias transitivas (cada atributo no-clave depende solo de la clave primaria)

**Ventajas:**
- Eliminación de redundancia
- Integridad referencial garantizada
- Facilidad de actualización
- Consultas eficientes con joins

## 6. Flujo de Ejecución

### 6.1 Diagrama de Flujo
```
[Inicio]
   ↓
[Crear Transactor BD]
   ↓
[DROP Tablas Existentes]
   ↓
[CREATE Tablas Nuevas]
   ↓
[Leer CSV con FS2 Stream]
   ↓
[Decodificar a Movie_Raw]
   ↓
[Validar Integridad] ──→ [Descartar Inválidos]
   ↓
[Limpiar Datos]
   ↓
[Convertir a Movie]
   ↓
[Dividir en Lotes de 100]
   ↓
[Para cada Lote:]
   ├─[Parsear JSON de cada película]
   ├─[Insertar Movie]
   ├─[Insertar Catálogos]
   ├─[Insertar People]
   ├─[Insertar Relaciones]
   └─[Commit Transacción]
   ↓
[Mostrar Progreso]
   ↓
[Fin]
```

### 6.2 Paso a Paso Detallado

**Fase 1: Inicialización (5 segundos)**
1. Cargar configuración desde application.conf
2. Crear HikariCP pool con 10 conexiones
3. Crear transactor de Doobie
4. Ejecutar DROP TABLE en orden inverso
5. Ejecutar CREATE TABLE en orden de dependencias
6. Confirmar creación de 18 tablas

**Fase 2: Lectura y Validación (30 segundos)**
7. Abrir stream del archivo CSV
8. Decodificar cada línea a Movie_Raw
9. Compilar lista completa en memoria (~45,000 registros)
10. Aplicar validación a cada registro
11. Aplicar limpieza a registros válidos
12. Convertir Movie_Raw a Movie
13. Contar películas válidas para procesar

**Fase 3: Procesamiento (Variable según tamaño)**
14. Dividir lista en lotes de 100 películas
15. Para cada lote:
    a. Parsear 9 columnas JSON por película
    b. Crear programa ConnectionIO compuesto
    c. Insertar película principal
    d. Insertar en catálogos (con INSERT IGNORE)
    e. Insertar personas (cast/crew)
    f. Insertar relaciones en tablas intermedias
    g. Insertar valoraciones
    h. Ejecutar transacción completa
    i. Mostrar progreso en consola
16. Manejar errores sin interrumpir proceso

**Fase 4: Finalización (1 segundo)**
17. Cerrar todas las conexiones
18. Liberar recursos del transactor
19. Mostrar mensaje de completado
20. Terminar programa con éxito

## 7. Comandos de Ejecución

### 7.1 Compilación
```bash
# Compilar el proyecto
sbt compile

# Limpiar y compilar
sbt clean compile
```

### 7.2 Ejecución
```bash
# Ejecutar el programa principal
sbt run

# Ejecutar con logs detallados
sbt -Dlogback.configurationFile=logback.xml run
```

### 7.3 Empaquetado
```bash
# Crear JAR ejecutable
sbt assembly

# Ejecutar JAR
java -jar target/scala-3.x.x/PracticumMovies-assembly-1.0.jar
```

### 7.4 Testing
```bash
# Ejecutar tests (si existen)
sbt test

# Ejecutar test específico
sbt "testOnly *SuiteTest"
```

## 8. Salida Esperada del Programa
```
=== INICIANDO ETL (MODELO POR LOTES) ===
Tablas recreadas.
Leyendo CSV...
Leídos 3487 registros.
Películas válidas para insertar: 3480
Iniciando inserción en 35 lotes...
✓ Lote 1/35 procesado (100 películas)
✓ Lote 2/35 procesado (200 películas)
✓ Lote 3/35 procesado (300 películas)
...
✓ Lote 35/35 procesado (3480 películas)

PROCESO COMPLETADO EXITOSAMENTE
```

## 9. Mejoras Futuras

### 9.1 Optimizaciones de Rendimiento
- Implementar batch inserts con JDBC batch
- Índices en columnas frecuentemente consultadas
- Particionamiento de tablas grandes
- Caché de consultas frecuentes

### 9.2 Funcionalidades Adicionales
- API REST para consultas
- Dashboard de visualización
- Sistema de recomendaciones
- Análisis de tendencias

### 9.3 Calidad de Código
- Tests unitarios con ScalaTest
- Tests de integración con BD embebida
- Cobertura de código > 80%
- Documentación Scaladoc completa

### 9.4 Monitoreo y Logging
- Integración con Logback
- Métricas de rendimiento
- Alertas de errores
- Dashboard de monitoreo

## 10. Conclusiones

El proyecto PracticumMovies1.1 implementa un pipeline ETL robusto y eficiente para procesamiento de datos cinematográficos. Utiliza principios de programación funcional y técnicas modernas de Scala para garantizar:

- **Integridad de datos** mediante validación exhaustiva
- **Eficiencia** con procesamiento por lotes y streaming
- **Mantenibilidad** con arquitectura modular y separation of concerns
- **Escalabilidad** con diseño funcional y transacciones optimizadas
- **Confiabilidad** con manejo de errores y recuperación automática

El código está estructurado siguiendo mejores prácticas de Scala funcional, haciendo uso extensivo de Cats Effect, Doobie y FS2 para crear un sistema type-safe, composable y fácil de extender.
