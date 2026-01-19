package LimpiezaCirse

import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import fs2.text
import scala.io.Source
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._

//Clases Principales
case class Crew(
                 credit_id: Option[String],
                 department: Option[String],
                 id: Option[Int],
                 job: Option[String],
                 name: Option[String],
                 profile_path: Option[String]
               )

case class MovieWithCrew(
                          movieId: Int,
                          title: String,
                          crew: List[Crew]
                        )

object Funciones_Limpieza:

  def prepararJSONCrewParaParseo(crew: String): String =
    if (crew == null || crew.trim.isEmpty) return "[]"
    crew.trim
      .replaceAll("None", "null")
      .replaceAll("True", "true")
      .replaceAll("False", "false")
      .replace("\"", "\\\"")
      .replaceAll("(?<![a-zA-Z0-9])'|'(?![a-zA-Z0-9])", "\"")
      .replaceAll("""\\\\""", "\\\\")


  def normalizarTexto(txt: String): Option[String] =
    val limpio = txt.trim.replaceAll("\\s+", " ")
    if (limpio.isEmpty) None else Some(limpio)

  // Normalizar un miembro del crew completo
  def normalizarCrewMember(c: Crew): Crew =
    c.copy(
      credit_id = c.credit_id.flatMap(normalizarTexto),
      department = c.department.flatMap(normalizarTexto),
      id = c.id,
      job = c.job.flatMap(normalizarTexto),
      name = c.name.flatMap(normalizarTexto),
      profile_path = c.profile_path.flatMap(normalizarTexto)
    )

  // Parsear CSV respetando comillas
  def parseCSVLine(line: String): Array[String] =
    val (fields, lastBuilder, _) = line.foldLeft(
      (Vector.empty[String], new StringBuilder, false)
    ) {
      case ((fields, current, inQuotes), char) => char match {
        case '"' =>
          (fields, current, !inQuotes)
        case ';' if !inQuotes =>
          (fields :+ current.toString, new StringBuilder, false)
        case _ =>
          current.append(char)
          (fields, current, inQuotes)
      }
    }
    (fields :+ lastBuilder.toString).toArray

  // Parsear crew de una celda CSV
  def parsearCrewDeCelda(crewStr: String): List[Crew] =
    if (crewStr.trim.isEmpty || crewStr == "[]") then
      List.empty
    else
      try {
        val jsonLimpio = prepararJSONCrewParaParseo(crewStr)
        decode[List[Crew]](jsonLimpio) match {
          case Right(crews) => crews.map(normalizarCrewMember)
          case Left(error) =>
            List.empty[Crew]
        }
      } catch {
        case e: Exception => List.empty
      }

object limpieza_Crew extends IOApp.Simple {
  val filePath = Path("C:\\Users\\Usuario iTC\\Desktop\\Practicum\\src\\main\\resources\\Data\\pi-movies-complete-2026-01-14.csv")
  val run: IO[Unit] =
    Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(text.lines)
      .compile
      .toList
      .flatMap { lines =>
        val headers = lines.head.split(";").map(_.trim)
        val crewIndex = headers.indexOf("crew")
        val idIndex = headers.indexOf("id")
        val titleIndex = headers.indexOf("title")

        val moviesConCrew: List[MovieWithCrew] = lines.tail.flatMap { line =>
          val parts = Funciones_Limpieza.parseCSVLine(line)

          if (parts.length > crewIndex && parts.length > idIndex && parts.length > titleIndex) {
            val movieId =
              val idStr = parts(idIndex).trim
              idStr.toInt

            val title = parts(titleIndex)
            val crewList = Funciones_Limpieza.parsearCrewDeCelda(parts(crewIndex))

            if (crewList.nonEmpty && movieId > 0) then
              Some(MovieWithCrew(movieId, title, crewList))
            else
              None
          } else {
            None
          }
        }

        val totalMovies = moviesConCrew.length
        val totalCrewMembers = moviesConCrew.flatMap(_.crew).distinctBy(c => (c.id, c.name, c.job)).length

        // Contar directores, productores, escritores DESDE las películas
        val peliculasConDirector = moviesConCrew.count(_.crew.exists(_.job.exists(_.contains("Director"))))
        val peliculasConProductores = moviesConCrew.count(_.crew.exists(_.department.exists(_.contains("Production"))))
        val peliculasConEscritores = moviesConCrew.count(_.crew.exists(_.department.exists(_.contains("Writing"))))

        // Top 10 directores (desde las películas)
        val topDirectores = moviesConCrew
          .flatMap(m => m.crew.find(_.job.exists(_.contains("Director"))).flatMap(_.name).map(_ -> m.title))
          .groupBy(_._1)
          .view.mapValues(_.size)
          .toList
          .sortBy(-_._2)
          .take(10)

        // Película con más crew
        val movieConMasCrew = if (moviesConCrew.nonEmpty) then
          moviesConCrew.maxBy(_.crew.length)
        else null

        for
          _ <- IO.println("=" * 80)
          _ <- IO.println("     REPORTE DE LIMPIEZA DE CREW (CON RELACIÓN PELÍCULA-CREW)")
          _ <- IO.println("=" * 80)
          _ <- IO.println("")
          _ <- IO.println("1. ESTADÍSTICAS GENERALES")
          _ <- IO.println("-" * 80)
          _ <- IO.println(f"Películas procesadas:                    ${totalMovies}%,7d")
          _ <- IO.println(f"Total crew members (únicos):             ${totalCrewMembers}%,7d")
          _ <- IO.println(f"Películas con director:                  ${peliculasConDirector}%,7d")
          _ <- IO.println(f"Películas con productores:               ${peliculasConProductores}%,7d")
          _ <- IO.println(f"Películas con escritores:                ${peliculasConEscritores}%,7d")
          _ <- IO.println("")
          _ <- IO.println("2. TOP 10 DIRECTORES (MÁS PELÍCULAS DIRIGIDAS)")
          _ <- IO.println("-" * 80)
          _ <- topDirectores.zipWithIndex.traverse_ { case ((nombre, count), i) =>
            IO.println(f"  ${i + 1}%2d. ${nombre}%-45s ${count}%4d películas")
          }
          _ <- IO.println("")
          _ <- IO.println("3. PELÍCULAS CON MÁS CREW")
          _ <- IO.println("-" * 80)
          _ <- if (movieConMasCrew != null) then
            for
              _ <- IO.println(s" ${movieConMasCrew.title} (ID: ${movieConMasCrew.movieId})")
              _ <- IO.println(s"   Crew total: ${movieConMasCrew.crew.length} personas")
              _ <- IO.println(s"   Director: ${movieConMasCrew.crew.find(_.job.exists(_.contains("Director"))).flatMap(_.name).getOrElse("No especificado")}")
            yield ()
          else
            IO.println("   No hay datos")
          _ <- IO.println("")
          _ <- IO.println("4. EJEMPLO DE DATOS PROCESADOS (Primeras 5 películas)")
          _ <- IO.println("-" * 80)
          _ <- moviesConCrew.take(5).traverse_ { movie =>
            for
              _ <- IO.println(s"\n ${movie.title} (ID: ${movie.movieId})")
              _ <- IO.println(s"   Crew total: ${movie.crew.length} personas")
              director = movie.crew.find(_.job.exists(_.contains("Director"))).flatMap(_.name).getOrElse("No especificado")
              _ <- IO.println(s"   Director: $director")
              _ <- IO.println(s"   Primeros 5 miembros del crew:")
              _ <- movie.crew.take(5).traverse_ { c =>
                IO.println(f"     - ${c.name.getOrElse("Unknown")}%-30s | ${c.job.getOrElse("Unknown")}%-20s | ${c.department.getOrElse("Unknown")}")
              }
            yield ()
          }
        yield ()
      }
}