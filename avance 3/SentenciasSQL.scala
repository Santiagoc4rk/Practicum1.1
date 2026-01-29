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
        // Tabla de Géneros
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS genres (
              iso VARCHAR(10) PRIMARY KEY,
              name VARCHAR(100) NOT NULL
          )
        """.update.run.transact(xa)

        // Tabla de Países
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS countries (
              iso VARCHAR(10) PRIMARY KEY,
              name VARCHAR(100) NOT NULL
          )
        """.update.run.transact(xa)

        // Tabla de Compañías
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS companies (
              id_company INT PRIMARY KEY,
              name VARCHAR(200) NOT NULL
          )
        """.update.run.transact(xa)

        // Tabla de Colecciones
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS collection (
              id_collection INT PRIMARY KEY,
              name VARCHAR(200) NOT NULL,
              backdrop_path VARCHAR(255),
              poster_path VARCHAR(255)
          )
        """.update.run.transact(xa)

        // Tabla de Idiomas
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS languages (
              iso VARCHAR(10) PRIMARY KEY,
              name VARCHAR(100) NOT NULL
          )
        """.update.run.transact(xa)

        // Tabla de Palabras Clave
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS keywords (
              id_keyword INT PRIMARY KEY,
              name VARCHAR(100) NOT NULL
          )
        """.update.run.transact(xa)

        // Tabla de Películas
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

        // Tabla de Personas
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS people (
              id_people INT PRIMARY KEY,
              gender VARCHAR(20),
              name VARCHAR(200) NOT NULL,
              profile_path VARCHAR(255),
              credit_id VARCHAR(50)
          )
        """.update.run.transact(xa)

        // Tabla de Usuarios
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS user (
              user_id INT PRIMARY KEY
          )
        """.update.run.transact(xa)

        // Tabla relacional: Películas contienen Géneros
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS contains_genres (
              iso VARCHAR(10),
              id_movie INT,
              PRIMARY KEY (iso, id_movie),
              FOREIGN KEY (iso) REFERENCES Genres(iso),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        // Tabla relacional: Países Productores
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS producer_countries (
              iso VARCHAR(10),
              id_movie INT,
              PRIMARY KEY (iso, id_movie),
              FOREIGN KEY (iso) REFERENCES Countries(iso),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        // Tabla relacional: Compañías Productoras
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS producer_companies (
              id_company INT,
              id_movie INT,
              PRIMARY KEY (id_company, id_movie),
              FOREIGN KEY (id_company) REFERENCES Companies(id_company),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        // Tabla relacional: Películas pertenecen a Colecciones
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS belongs_to_collection (
              id_collection INT,
              id_movie INT,
              PRIMARY KEY (id_collection, id_movie),
              FOREIGN KEY (id_collection) REFERENCES Collection(id_collection),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        // Tabla relacional: Idiomas Hablados en Películas
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS spoken_languages (
              iso VARCHAR(10),
              id_movie INT,
              PRIMARY KEY (iso, id_movie),
              FOREIGN KEY (iso) REFERENCES Languages(iso),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        // Tabla relacional: Identificación por Palabras Clave
        _ <- sql"""
          CREATE TABLE IF NOT EXISTS identify_by (
              id_keyword INT,
              id_movie INT,
              PRIMARY KEY (id_keyword, id_movie),
              FOREIGN KEY (id_keyword) REFERENCES keywords(id_keyword),
              FOREIGN KEY (id_movie) REFERENCES Movies(id_movie)
          )
        """.update.run.transact(xa)

        // Tabla relacional: Equipo de Producción (Crewing)
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

        // Tabla relacional: Reparto (Casting)
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

        // Tabla de Valoraciones/Ratings
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

  // Función principal que primero dropea y luego crea las tablas
  def inicializarBaseDatos(): IO[Unit] = {
    for {
      _ <- dropTablas()
      _ <- IO.println("Tablas eliminadas exitosamente")
      _ <- crearTablas()
      _ <- IO.println("Tablas creadas exitosamente")
    } yield ()
  }

  // Función alternativa para uso individual
  def entradaTablas(): IO[Unit] = {
    inicializarBaseDatos()
  }
}