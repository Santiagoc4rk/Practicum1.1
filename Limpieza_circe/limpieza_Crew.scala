package LimpiezaCirse

import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Path}
import fs2.text
import scala.io.Source
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._

//Clases Principales
case class Crew(
                 credit_id : Option[String],
                 department : Option[String],
                 id : Option[Int],
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
            List.empty
        }
      } catch {
        case e: Exception =>
          println(s"Excepción parseando crew: ${e.getMessage}")
          List.empty
      }
object limpieza_Crew extends IOApp.Simple {
  val filePath = Path("C:\\Users\\Usuario iTC\\Desktop\\Practicum\\src\\main\\resources\\Data\\pi_movies_complete.csv")
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
            val movieId = try parts(idIndex).toInt catch {
              case _: Exception => 0
            }
            val title = parts(titleIndex)
            val crewList = Funciones_Limpieza.parsearCrewDeCelda(parts(crewIndex))

            if (crewList.nonEmpty) then
              Some(MovieWithCrew(movieId, title, crewList))
            else
              None
          } else {
            None
          }


        }

        val crewTotalLimpio: List[Crew] = moviesConCrew.flatMap(_.crew)
          .distinctBy(c => (c.id, c.name, c.job))


        val totalMovies = moviesConCrew.length
        val totalCrewMembers = crewTotalLimpio.length
        val directores = crewTotalLimpio.count(_.job.contains("Director"))
        val productores = crewTotalLimpio.count(_.department.contains("Production"))
        val escritores = crewTotalLimpio.count(_.department.contains("Writing"))

        for

          _ <- IO.println("     REPORTE DE LIMPIEZA DE CREW")
          _ <- IO.println("=" * 80)
          _ <- IO.println("")
          _ <- IO.println("ESTADÍSTICAS GENERALES")
          _ <- IO.println("-" * 80)
          _ <- IO.println(f"Películas procesadas:              ${totalMovies}%,7d")
          _ <- IO.println(f"Total crew members (únicos):       ${totalCrewMembers}%,7d")
          _ <- IO.println(f"Directores:                        ${directores}%,7d")
          _ <- IO.println(f"Miembros de Producción:            ${productores}%,7d")
          _ <- IO.println(f"Escritores:                        ${escritores}%,7d")
          _ <- IO.println("")
          _ <- IO.println(s"   - moviesConCrew: List[MovieWithCrew] (${moviesConCrew.length} películas)")
          _ <- IO.println(s"   - crewTotalLimpio: List[Crew] (${crewTotalLimpio.length} miembros)")
        yield()
      }
}