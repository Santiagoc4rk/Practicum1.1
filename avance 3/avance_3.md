# Poblado de Base de Datos - Sistema de Gestión de Películas

Este documento describe el proceso completo de poblado de la base de datos del sistema de gestión de películas, incluyendo la creación de tablas mediante scripts SQL desde Scala y la carga de datos a través de sentencias INSERT con Doobie.

---

## Índice

1. [Arquitectura de la Base de Datos](#arquitectura-de-la-base-de-datos)
2. [Creación de Tablas (DDL)](#creación-de-tablas-ddl)
3. [Validación y Limpieza de Datos](#validación-y-limpieza-de-datos)
4. [Conversión de Modelos](#conversión-de-modelos)
5. [DAO de Películas](#dao-de-películas)
6. [Proceso ETL Completo](#proceso-etl-completo)
7. [Resultados y Verificación](#resultados-y-verificación)

---

## Arquitectura de la Base de Datos

### Configuración de Conexión

**Archivo**: `src/main/resources/application.conf`
```hocon
db {
  driver = "com.mysql.cj.jdbc.Driver"
  url = "jdbc:mysql://localhost:3306/pimoviescomplete"
  user = "root"
  password = "Superpelu2025@x"
}
```

**Archivo**: `src/main/scala/config/Database.scala`
```scala
package config

import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory
import doobie.hikari.HikariTransactor
import scala.concurrent.ExecutionContext

object Database {
  private val connectEC: ExecutionContext = ExecutionContext.global

  def transactor: Resource[IO, HikariTransactor[IO]] = {
    val config = ConfigFactory.load().getConfig("db")
    HikariTransactor.newHikariTransactor[IO](
      config.getString("driver"),
      config.getString("url"),
      config.getString("user"),
      config.getString("password"),
      connectEC
    )
  }
}
```

---

### Estructura de Tablas

La base de datos está compuesta por **19 tablas** organizadas en tres categorías:

#### Tablas de Entidades Principales

| Tabla | Descripción | Campos Principales |
|-------|-------------|-------------------|
| **movies** | Información principal de películas | id_movie, title, budget, revenue, vote_average |
| **people** | Actores y miembros del equipo | id_people, name, gender, profile_path |
| **user** | Usuarios del sistema | user_id |

#### Tablas de Catálogos

| Tabla | Descripción | Campos Principales |
|-------|-------------|-------------------|
| **genres** | Géneros cinematográficos | iso, name |
| **countries** | Países de producción | iso, name |
| **companies** | Compañías productoras | id_company, name |
| **collection** | Colecciones/Sagas | id_collection, name, poster_path |
| **languages** | Idiomas | iso, name |
| **keywords** | Palabras clave temáticas | id_keyword, name |

#### Tablas Relacionales (Many-to-Many)

| Tabla | Relación | Propósito |
|-------|----------|-----------|
| **contains_genres** | movies ↔ genres | Géneros de cada película |
| **producer_countries** | movies ↔ countries | Países productores |
| **producer_companies** | movies ↔ companies | Compañías productoras |
| **belongs_to_collection** | movies ↔ collection | Pertenencia a sagas |
| **spoken_languages** | movies ↔ languages | Idiomas hablados |
| **identify_by** | movies ↔ keywords | Palabras clave |
| **casting** | people ↔ movies | Reparto de actores |
| **crewing** | people ↔ movies | Equipo técnico |
| **rate** | user ↔ movies | Calificaciones de usuarios |

---

## Creación de Tablas (DDL)

### Implementación con Doobie

**Archivo**: `src/main/scala/dao/SentenciasSQL.scala`
```scala
package dao

import cats.effect.IO
import config.Database
import doobie.*
import doobie.implicits.*

object SentenciasSQL {

  // Sentencias DROP TABLE
  def dropTablas(): IO[Unit] = {
    Database.transactor.use { xa =>
      for {
        _ <- sql"DROP TABLE IF EXISTS rate CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS casting CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS crewing CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS identify_by CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS spoken_languages CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS belongs_to_collection CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS producer_companies CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS producer_countries CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS contains_genres CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS user CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS people CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS movies CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS keywords CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS languages CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS collection CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS companies CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS countries CASCADE".update.run.transact(xa)
        _ <- sql"DROP TABLE IF EXISTS genres CASCADE".update.run.transact(xa)
      } yield ()
    }
  }

  // Sentencias CREATE TABLE
  def crearTablas(): IO[Unit] = {
    Database.transactor.use { xa =>
      for {
        // Tablas de Catálogos
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS genres (
              iso VARCHAR(10) PRIMARY KEY,
              name VARCHAR(100) NOT NULL
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS countries (
              iso VARCHAR(10) PRIMARY KEY,
              name VARCHAR(100) NOT NULL
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS companies (
              id_company INT PRIMARY KEY,
              name VARCHAR(200) NOT NULL
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS collection (
              id_collection INT PRIMARY KEY,
              name VARCHAR(200) NOT NULL,
              backdrop_path VARCHAR(255),
              poster_path VARCHAR(255)
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS languages (
              iso VARCHAR(10) PRIMARY KEY,
              name VARCHAR(100) NOT NULL
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS keywords (
              id_keyword INT PRIMARY KEY,
              name VARCHAR(100) NOT NULL
          )
        """.update.run.transact(xa)

        // Tabla Principal: Movies
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS Movies (
              id_movie INT PRIMARY KEY,
              imdb VARCHAR(20),
              status VARCHAR(50),
              vote_count INT,
              revenue DECIMAL(15,2),
              vote_average DECIMAL(4,1),
              title VARCHAR(255) NOT NULL,
              tagline VARCHAR(255),
              video BOOLEAN,
              popularity DECIMAL(10,2),
              runtime INT,
              adult BOOLEAN,
              oficial_language VARCHAR(10),
              poster_path VARCHAR(255),
              backdrop_path VARCHAR(255),
              overview TEXT,
              original_title VARCHAR(255),
              budget INT,
              homepage VARCHAR(255)
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS people (
              id_people INT PRIMARY KEY,
              gender VARCHAR(20),
              name VARCHAR(200) NOT NULL,
              profile_path VARCHAR(255),
              credit_id VARCHAR(50)
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS user (
              user_id INT PRIMARY KEY
          )
        """.update.run.transact(xa)

        // Tablas Relacionales
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS contains_genres (
              iso VARCHAR(10),
              id_movie INT,
              PRIMARY KEY (iso, id_movie),
              FOREIGN KEY (iso) REFERENCES Genres(iso),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS producer_countries (
              iso VARCHAR(10),
              id_movie INT,
              PRIMARY KEY (iso, id_movie),
              FOREIGN KEY (iso) REFERENCES Countries(iso),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS producer_companies (
              id_company INT,
              id_movie INT,
              PRIMARY KEY (id_company, id_movie),
              FOREIGN KEY (id_company) REFERENCES Companies(id_company),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS belongs_to_collection (
              id_collection INT,
              id_movie INT,
              PRIMARY KEY (id_collection, id_movie),
              FOREIGN KEY (id_collection) REFERENCES Collection(id_collection),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS spoken_languages (
              iso VARCHAR(10),
              id_movie INT,
              PRIMARY KEY (iso, id_movie),
              FOREIGN KEY (iso) REFERENCES Languages(iso),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS identify_by (
              id_keyword INT,
              id_movie INT,
              PRIMARY KEY (id_keyword, id_movie),
              FOREIGN KEY (id_keyword) REFERENCES keywords(id_keyword),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS crewing (
              id_people INT,
              id_movie INT,
              job VARCHAR(100),
              department VARCHAR(100),
              PRIMARY KEY (id_people, id_movie),
              FOREIGN KEY (id_people) REFERENCES People(id_people),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS casting (
              id_people INT,
              id_movie INT,
              cast_id INT,
              personage VARCHAR(200),
              order_positions VARCHAR(10),
              PRIMARY KEY (id_people, id_movie, cast_id),
              FOREIGN KEY (id_people) REFERENCES People(id_people),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        _ <- sql"""
          CREATE TABLE IF NOT EXISTS rate (
              id_movie INT,
              user_id INT,
              rating DECIMAL(3,2),
              timestamp INT,
              PRIMARY KEY (id_movie, user_id),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie),
              FOREIGN KEY (user_id) REFERENCES User(user_id)
          )
        """.update.run.transact(xa)
      } yield ()
    }
  }
}
```

### Orden de Eliminación (CASCADE)

El orden de eliminación es crítico debido a las dependencias de claves foráneas:
```
1. Tablas relacionales (rate, casting, crewing, identify_by, etc.)
   ↓
2. Tablas de entidades dependientes (user, people, movies)
   ↓
3. Tablas de catálogos (keywords, languages, collection, companies, countries, genres)
```

---

## Validación y Limpieza de Datos

### Estrategia de Limpieza

A diferencia del enfoque anterior que **filtraba** registros inválidos, la nueva implementación **transforma** todos los registros aplicando correcciones automáticas.

**Archivo**: `src/main/scala/untilies/ValidacionMovies.scala`
```scala
package untilies

import models.Movie_Raw

object ValidacionMovies {

  // ==========================================
  // FUNCIONES DE VALIDACIÓN
  // ==========================================
  def isValidString(s: String): Boolean =
    s != null && s.trim.nonEmpty && !s.equalsIgnoreCase("null")

  def isValidBooleanString(s: String): Boolean =
    val normalized = s.trim.toLowerCase
    normalized == "true" || normalized == "false" || normalized == "1" || normalized == "0"

  // ==========================================
  // FUNCIONES DE LIMPIEZA
  // ==========================================
  
  /** Limpia valores numéricos: convierte negativos a positivos */
  def limpiarNumerico(valor: Double): Double =
    if (valor < 0) valor * -1 else valor

  def limpiarNumerico(valor: Int): Int =
    if (valor < 0) valor * -1 else valor

  /** Limpia strings: si es nulo/vacío/"null", retorna valor por defecto */
  def limpiarString(s: String, default: String): String =
    if (isValidString(s)) s.trim else default

  /** Limpia strings opcionales */
  def limpiarStringOpcional(s: String): String =
    if (s == null || s.trim.isEmpty || s.equalsIgnoreCase("null")) "" else s.trim

  /** Limpia booleanos como string */
  def limpiarBooleano(s: String): String =
    if (isValidBooleanString(s)) s.trim.toLowerCase else "false"

  /** Valida y limpia fecha en formato YYYY-MM-DD */
  def limpiarFecha(s: String): String =
    if (s != null && s.matches("\\d{4}-\\d{2}-\\d{2}")) s else "1900-01-01"

  /** Valida y limpia código de idioma (2-3 caracteres) */
  def limpiarIdioma(s: String): String =
    if (isValidString(s) && s.length >= 2 && s.length <= 3) s.trim else "en"

  // ==========================================
  // LIMPIEZA COMPLETA
  // ==========================================
  def limpiarDatosCompletos(lista: List[Movie_Raw]): List[Movie_Raw] =
    lista.map { m =>
      m.copy(
        // Numéricos: convertir negativos a positivos
        id = limpiarNumerico(m.id),
        budget = limpiarNumerico(m.budget),
        revenue = limpiarNumerico(m.revenue),
        runtime = limpiarNumerico(m.runtime),
        popularity = limpiarNumerico(m.popularity),
        vote_average = math.min(10.0, limpiarNumerico(m.vote_average)),
        vote_count = limpiarNumerico(m.vote_count),

        // Strings obligatorios con valores por defecto
        title = limpiarString(m.title, "Unknown Title"),
        original_title = limpiarString(m.original_title, "Unknown"),
        overview = limpiarString(m.overview, "No overview available"),
        status = limpiarString(m.status, "Released"),

        // Fecha con validación de formato
        release_date = limpiarFecha(m.release_date),

        // Booleanos como string
        adult = limpiarBooleano(m.adult),
        video = limpiarBooleano(m.video),

        // Código de idioma
        original_language = limpiarIdioma(m.original_language),

        // Campos opcionales
        homepage = limpiarStringOpcional(m.homepage),
        imdb_id = limpiarStringOpcional(m.imdb_id),
        poster_path = limpiarStringOpcional(m.poster_path),
        tagline = limpiarStringOpcional(m.tagline)
      )
    }
}
```

### Transformaciones Aplicadas

| Tipo de Dato | Problema | Solución |
|--------------|----------|----------|
| **Numéricos negativos** | budget = -1000 | budget = 1000 (×-1) |
| **Strings vacíos** | title = "" | title = "Unknown Title" |
| **Strings nulos** | overview = null | overview = "No overview available" |
| **Fechas inválidas** | release_date = "invalid" | release_date = "1900-01-01" |
| **Booleanos inválidos** | adult = "maybe" | adult = "false" |
| **Idiomas inválidos** | original_language = "x" | original_language = "en" |
| **vote_average > 10** | vote_average = 15.0 | vote_average = 10.0 |

---

## Conversión de Modelos

### Transformación Movie_Raw → Movie

**Archivo**: `src/main/scala/untilies/MovieConverter.scala`
```scala
package untilies

import models.{Movie_Raw, Movie}

object MovieConverter {

  def rawToMovie(raw: Movie_Raw): Option[Movie] = {
    try {
      Some(Movie(
        id_movie = raw.id.toInt,
        imdb = raw.imdb_id,
        status = raw.status,
        vote_count = raw.vote_count.toInt,
        revenue = raw.revenue,
        vote_average = raw.vote_average,
        title = raw.title,
        tagline = raw.tagline,
        video = parseBoolean(raw.video),
        popularity = raw.popularity,
        runtime = raw.runtime.toInt,
        adult = parseBoolean(raw.adult),
        oficial_language = raw.original_language,
        poster_path = raw.poster_path,
        backdrop_path = raw.poster_path,
        overview = raw.overview,
        original_title = raw.original_title,
        budget = raw.budget.toInt,
        homepage = raw.homepage
      ))
    } catch {
      case _: Exception => None
    }
  }

  private def parseBoolean(s: String): Boolean = {
    val normalized = s.trim.toLowerCase
    normalized == "true" || normalized == "1"
  }
}
```

### Mapeo de Campos

| Campo Movie_Raw | Campo Movie | Transformación |
|-----------------|-------------|----------------|
| id | id_movie | `.toInt` |
| imdb_id | imdb | Directo |
| status | status | Directo |
| vote_count | vote_count | `.toInt` |
| revenue | revenue | Directo (Double → DECIMAL) |
| vote_average | vote_average | Directo (Double → DECIMAL) |
| title | title | Directo |
| tagline | tagline | Directo |
| video | video | `parseBoolean()` |
| popularity | popularity | Directo (Double → DECIMAL) |
| runtime | runtime | `.toInt` |
| adult | adult | `parseBoolean()` |
| original_language | oficial_language | Directo |
| poster_path | poster_path | Directo |
| poster_path | backdrop_path | Reutilizado |
| overview | overview | Directo (String → TEXT) |
| original_title | original_title | Directo |
| budget | budget | `.toInt` |
| homepage | homepage | Directo |

---

## DAO de Películas

### Implementación del Data Access Object

**Archivo**: `src/main/scala/dao/moviesDAO.scala`
```scala
package dao

import cats.effect.IO
import cats.implicits.*
import config.Database
import doobie.*
import doobie.implicits.*
import models.Movie

object moviesDAO {
  
  /** Inserta una película en la base de datos */
  def insert(m: Movie): ConnectionIO[Int] = {
    sql"""
      INSERT IGNORE INTO movies (id_movie, imdb, status, vote_count, revenue,
      vote_average, title, tagline, video, popularity, runtime, adult,
       oficial_language, poster_path, backdrop_path, overview, original_title,
        budget, homepage)
      VALUES (
        ${m.id_movie}, ${m.imdb}, ${m.status}, ${m.vote_count}, ${m.revenue},
        ${m.vote_average}, ${m.title}, ${m.tagline}, ${m.video}, ${m.popularity},
        ${m.runtime}, ${m.adult}, ${m.oficial_language}, ${m.poster_path}, ${m.backdrop_path},
        ${m.overview}, ${m.original_title}, ${m.budget}, ${m.homepage}
     )
      """.update.run
  }

  /** Consulta si existe una película por ID */
  def consultarID(id: Int): ConnectionIO[Option[Int]] = {
    sql"""
      SELECT id_movie
      FROM movies
      WHERE id_movie = $id
      """.query[Int]
      .option
  }
}
```

### Características del DAO

| Función | Propósito | Retorno |
|---------|-----------|---------|
| **insert** | Inserta película usando INSERT IGNORE | `ConnectionIO[Int]` (filas afectadas) |
| **consultarID** | Verifica existencia de película por ID | `ConnectionIO[Option[Int]]` |

### INSERT IGNORE

La cláusula `INSERT IGNORE` tiene un comportamiento especial:

- **Si el registro NO existe**: Se inserta normalmente y retorna 1
- **Si el registro YA existe**: Se ignora la operación y retorna 0
- **Sin excepciones**: No lanza errores por duplicados de PRIMARY KEY

**Ventajas**:
- Previene errores de duplicados
- Permite operaciones idempotentes
- Facilita la reinserción de datos sin conflictos

**Ejemplo**:
```scala
// Primera inserción: retorna 1
moviesDAO.insert(movie1) // Éxito: 1 fila insertada

// Segunda inserción del mismo ID: retorna 0
moviesDAO.insert(movie1) // Ignorado: 0 filas afectadas (sin error)
```

### Uso en el Proceso ETL
```scala
Database.transactor.use { xa =>
  convertedMovies.traverse { movie =>
    moviesDAO.insert(movie).transact(xa).attempt
  }
}
```

El método `.attempt` convierte el resultado en `Either[Throwable, Int]`:
- **`Right(1)`**: Inserción exitosa
- **`Right(0)`**: Registro duplicado (ignorado)
- **`Left(error)`**: Error de base de datos

---

## Proceso ETL Completo

### Flujo de Datos
```
CSV (3,487 registros)
    ↓
LECTURA (FS2 Stream + CSV Decoder)
    ↓
LIMPIEZA (Transformación de valores inválidos)
    ↓
CONVERSIÓN (Movie_Raw → Movie)
    ↓
INSERCIÓN (Doobie transact + INSERT IGNORE)
    ↓
BASE DE DATOS (3,487 registros)
```

### Implementación del Main

**Archivo**: `src/main/scala/Main/Main.scala`
```scala
package Main

import cats.effect.{IO, IOApp}
import cats.implicits.*
import fs2.text
import fs2.io.file.{Files, Path}
import fs2.data.csv.*
import fs2.data.csv.generic.semiauto.*
import doobie.*
import doobie.implicits.*
import models.{Movie_Raw, Movie}
import untilies.{ValidacionMovies, MovieConverter}
import dao.{moviesDAO, SentenciasSQL}
import config.Database

given CsvRowDecoder[Movie_Raw, String] = deriveCsvRowDecoder[Movie_Raw]

object Main extends IOApp.Simple {

  val filePath: Path =
    Path("C:\\Users\\Usuario iTC\\Desktop\\PracticumMovies1.1\\src\\main\\resources\\data\\pi-movies-complete-2026-01-14.csv")

  private def movieStream: fs2.Stream[IO, Movie_Raw] =
    Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(decodeUsingHeaders[Movie_Raw](';'))

  val run: IO[Unit] =
    (for {
      // FASE 0: INICIALIZACIÓN DE BASE DE DATOS
      _ <- IO.println("\n" + "=" * 100)
      _ <- IO.println("  FASE 0: INICIALIZANDO BASE DE DATOS")
      _ <- IO.println("=" * 100)

      _ <- IO.println("  Eliminando tablas existentes...")
      _ <- SentenciasSQL.dropTablas()
      _ <- IO.println("  Tablas eliminadas exitosamente")

      _ <- IO.println("  Creando nuevas tablas...")
      _ <- SentenciasSQL.crearTablas()
      _ <- IO.println("  Tablas creadas exitosamente")
      _ <- IO.println("=" * 100 + "\n")

      // LEER CSV EN MEMORIA
      _ <- IO.println("=" * 100)
      _ <- IO.println("  CARGANDO DATOS DEL CSV")
      _ <- IO.println("=" * 100)

      allRawMovies <- movieStream.compile.toList
      _ <- IO.println(s"  Total registros leídos del CSV: ${allRawMovies.size}")
      _ <- IO.println("=" * 100 + "\n")

      // FASE 1: LIMPIEZA Y PROCESAMIENTO
      _ <- IO.println("=" * 100)
      _ <- IO.println("  FASE 1: LIMPIEZA Y PROCESAMIENTO DE PELÍCULAS")
      _ <- IO.println("=" * 100)

      cleanRawMovies = ValidacionMovies.limpiarDatosCompletos(allRawMovies)
      _ <- IO.println(s"  Registros limpiados: ${cleanRawMovies.size}")
      _ <- IO.println(s"  Nota: Todos los registros fueron preservados y transformados")

      _ <- Database.transactor.use { xa =>
        val convertedMovies = cleanRawMovies.flatMap(MovieConverter.rawToMovie)

        for {
          _ <- IO.println(s"  Convertidos a Movie: ${convertedMovies.size}")
          _ <- IO.println(s"  No convertibles: ${cleanRawMovies.size - convertedMovies.size}")

          _ <- IO.println(s"\n  Insertando ${convertedMovies.size} películas en la base de datos...")
          resultados <- convertedMovies.traverse { movie =>
            moviesDAO.insert(movie).transact(xa).attempt
          }

          exitosos = resultados.count(_.isRight)
          fallidos = resultados.count(_.isLeft)

          _ <- if (fallidos > 0) {
            IO.println(s"\n  Errores en inserción:") >>
              resultados.zipWithIndex.collect {
                case (Left(error), idx) => (error, idx)
              }.take(10).traverse_ { case (error, idx) =>
                IO.println(s"    - Registro ${idx + 1}: ${error.getMessage}")
              }
          } else IO.unit

          _ <- IO.println(s"\n  Películas insertadas: $exitosos")
          _ <- IO.println(s"  Fallos en inserción: $fallidos")
          _ <- IO.println("=" * 100)
        } yield ()
      }

      // RESUMEN FINAL
      _ <- IO.println("\n" + "=" * 100)
      _ <- IO.println("  PROCESO ETL COMPLETADO EXITOSAMENTE")
      _ <- IO.println("=" * 100)
      _ <- IO.println("  Fases ejecutadas:")
      _ <- IO.println("    1. Inicialización de base de datos")
      _ <- IO.println("    2. Limpieza y transformación de datos")
      _ <- IO.println("    3. Carga de películas")
      _ <- IO.println("=" * 100)
      _ <- IO.println(s"  Total registros procesados: ${cleanRawMovies.size}")
      _ <- IO.println("=" * 100 + "\n")

    } yield ()).handleErrorWith { e =>
      IO.println(s"\n ERROR CRÍTICO: ${e.getMessage}") >>
        IO.println(s"\nStack Trace:\n${e.getStackTrace.mkString("\n")}") >>
        IO.raiseError(e)
    }
}
```

### Fases del ETL

#### Fase 0: Inicialización de Base de Datos
1. Eliminación de tablas existentes (en orden inverso)
2. Creación de nuevas tablas (respetando dependencias)

#### Fase 1: Carga de Datos desde CSV
1. Lectura del archivo CSV usando FS2
2. Decodificación automática a `Movie_Raw` usando `fs2-data-csv`
3. Compilación del stream a lista en memoria

#### Fase 2: Limpieza y Transformación
1. Aplicación de reglas de limpieza a cada registro
2. Corrección de valores negativos, nulos e inválidos
3. Preservación del 100% de los registros

#### Fase 3: Conversión y Carga
1. Conversión de `Movie_Raw` a `Movie`
2. Inserción en la base de datos usando transacciones
3. Manejo de errores individuales sin detener el proceso

---

## Resultados y Verificación

### Salida de Ejecución
```
====================================================================================================
  FASE 0: INICIALIZANDO BASE DE DATOS
====================================================================================================
  Eliminando tablas existentes...
  Tablas eliminadas exitosamente
  Creando nuevas tablas...
  Tablas creadas exitosamente
====================================================================================================

====================================================================================================
  CARGANDO DATOS DEL CSV
====================================================================================================
  Total registros leídos del CSV: 3487
====================================================================================================

====================================================================================================
  FASE 1: LIMPIEZA Y PROCESAMIENTO DE PELÍCULAS
====================================================================================================
  Registros limpiados: 3487
  Nota: Todos los registros fueron preservados y transformados
  Convertidos a Movie: 3487
  No convertibles: 0

  Insertando 3487 películas en la base de datos...
  Películas insertadas: 3487
  Fallos en inserción: 0
====================================================================================================

====================================================================================================
  PROCESO ETL COMPLETADO EXITOSAMENTE
====================================================================================================
  Fases ejecutadas:
    1. Inicialización de base de datos
    2. Limpieza y transformación de datos
    3. Carga de películas
====================================================================================================
  Total registros procesados: 3487
====================================================================================================
```

### Estadísticas del Proceso

| Métrica | Valor |
|---------|-------|
| **Registros originales (CSV)** | 3,487 |
| **Registros transformados** | 3,487 |
| **Tasa de preservación** | 100% |
| **Conversiones exitosas** | 3,487 |
| **Conversiones fallidas** | 0 |
| **Tasa de éxito de conversión** | 100% |
| **Inserciones exitosas** | 3,487 |
| **Inserciones fallidas** | 0 |
| **Tasa de éxito de inserción** | 100% |

### Verificación en MySQL

#### Consulta 1: Top 5 Películas por Calificación
```sql
SELECT id_movie, title, vote_average, budget, revenue, runtime 
FROM movies 
ORDER BY vote_average DESC
LIMIT 5;
```

**Resultado**:

| id_movie | title | vote_average | budget | revenue | runtime |
|----------|-------|--------------|---------|----------|---------|
| 278 | The Shawshank Redemption | 8.5 | 25000000 | 28341469 | 142 |
| 238 | The Godfather | 8.5 | 6000000 | 245066411 | 175 |
| 240 | The Godfather: Part II | 8.3 | 13000000 | 57300000 | 200 |
| 424 | Schindler's List | 8.3 | 22000000 | 321265768 | 195 |
| 19404 | Dilwale Dulhania Le Jayenge | 8.3 | 500000 | 20000000 | 181 |

#### Consulta 2: Conteo Total de Películas
```sql
SELECT COUNT(*) as total_movies FROM movies;
```

**Resultado**:
```
total_movies: 3487
```

#### Consulta 3: Estadísticas de Budget y Revenue
```sql
SELECT 
  COUNT(*) as total,
  AVG(budget) as avg_budget,
  AVG(revenue) as avg_revenue,
  MAX(budget) as max_budget,
  MAX(revenue) as max_revenue
FROM movies;
```

**Resultado**:

| total | avg_budget | avg_revenue | max_budget | max_revenue |
|-------|------------|-------------|------------|-------------|
| 3487 | 29,854,295 | 82,445,923 | 380,000,000 | 2,787,965,087 |

#### Consulta 4: Distribución por Estado
```sql
SELECT status, COUNT(*) as count
FROM movies
GROUP BY status
ORDER BY count DESC;
```

**Resultado**:

| status | count |
|--------|-------|
| Released | 3386 |
| Post Production | 45 |
| Rumored | 32 |
| In Production | 24 |

---

## Conclusiones

### Ventajas del Enfoque Actual

1. **Preservación total de datos**
   - No se descartan registros
   - Todos los datos se transforman y limpian
   - Tasa de éxito: 100%

2. **Transformaciones automáticas**
   - Valores negativos corregidos
   - Campos nulos reemplazados con defaults
   - Formatos validados y normalizados

3. **Robustez del proceso**
   - INSERT IGNORE previene duplicados
   - Manejo de errores individuales
   - Transacciones atómicas por película

4. **Eficiencia del proceso**
   - Pool de conexiones Hikari
   - Lectura única del CSV
   - Procesamiento en memoria

5. **Trazabilidad completa**
   - Logs detallados por fase
   - Estadísticas de procesamiento
   - Identificación de problemas

6. **Código mantenible**
   - Funciones modulares y reutilizables
   - Separación de responsabilidades (DAO, Converter, Validator)
   - Fácil de testear y extender

### Diferencias con Enfoque Anterior

| Aspecto | Enfoque Anterior | Enfoque Actual |
|---------|-----------------|----------------|
| **Estrategia** | Filtrado + Validación | Transformación + Limpieza |
| **Registros procesados** | 3,288 (94.29%) | 3,487 (100%) |
| **Registros descartados** | 199 (5.71%) | 0 (0%) |
| **Valores negativos** | Rechazados | Convertidos a positivos |
| **Campos vacíos** | Rechazados | Reemplazados con defaults |
| **Outliers IQR** | Filtrados | Preservados |
| **Manejo de duplicados** | Errores | INSERT IGNORE |

### Arquitectura del Sistema
```
┌─────────────────────────────────────────────────────────────┐
│                         Main.scala                          │
│                     (Orquestador ETL)                       │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
   │ SentenciasSQL│  │ ValidacionM..│  │MovieConverter│
   │   (DDL/DML)  │  │  (Limpieza)  │  │ (Transform)  │
   └──────────────┘  └──────────────┘  └──────────────┘
              │               │               │
              └───────────────┼───────────────┘
                              ▼
                     ┌──────────────┐
                     │  moviesDAO   │
                     │  (INSERT)    │
                     └──────────────┘
                              │
                              ▼
                     ┌──────────────┐
                     │   Database   │
                     │   (MySQL)    │
                     └──────────────┘
```

---

## Referencias

- [Doobie Documentation](https://tpolecat.github.io/doobie/)
- [Cats Effect](https://typelevel.org/cats-effect/)
- [FS2 - Functional Streams](https://fs2.io/)
- [fs2-data-csv](https://fs2-data.gnieh.org/documentation/csv/)
- [MySQL Reference Manual](https://dev.mysql.com/doc/)
- [HikariCP Connection Pool](https://github.com/brettwooldridge/HikariCP)
