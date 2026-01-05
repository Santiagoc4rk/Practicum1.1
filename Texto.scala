package Semana11

import cats.effect.{IO, IOApp}
import fs2.text
import fs2.io.file.{Files, Path}
import fs2.data.csv.*
import fs2.data.csv.generic.semiauto.*

case class MovieText(
                      original_title: String,
                      original_language: String,
                      belongs_to_collection: String,
                      tagline: String,
                      status: String
                    )

given CsvRowDecoder[MovieText, String] =
  deriveCsvRowDecoder[MovieText]

object LeerMoviesTexto extends IOApp.Simple:

  val filePath: Path =
    Path("C:\\Users\\Usuario iTC\\Desktop\\Practicum\\src\\main\\resources\\Data\\pi_movies_complete.csv")

  val run: IO[Unit] =
    Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(decodeUsingHeaders[MovieText](';'))
      .attempt
      .collect { case Right(movie) => movie }
      .compile
      .toList
      .flatMap { movies =>

        def frecuencia(values: List[String]): Map[String, Int] =
          values.groupBy(identity).view.mapValues(_.size).toMap

        val freqLanguages = frecuencia(movies.map(_.original_language))
        val topLanguages = freqLanguages.toSeq.sortBy(-_._2).take(10)

        val collectionsLimpias =
          movies.map(_.belongs_to_collection).filter(c => c.nonEmpty && c != "null")

        val freqCollections = frecuencia(collectionsLimpias)
        val topCollections = freqCollections.toSeq.sortBy(-_._2).take(10)

        val freqStatus = frecuencia(movies.map(_.status))

        val totalConTagline =
          movies.map(_.tagline).count(t => t.nonEmpty && t != "null")

        val totalSinTagline = movies.size - totalConTagline

        val totalTitulosUnicos =
          movies.map(_.original_title).distinct.size

        val muestraTitulos =
          movies.take(20).map(_.original_title)

        (
          IO.println("\n" + "=" * 70) >>
            IO.println(" 5.4 ANALISIS DE DATOS EN COLUMNAS TIPO TEXTO") >>
            IO.println("=" * 70) >>
            IO.println(s"Total de peliculas validas: ${movies.size}") >>

            IO.println("\n--- IDIOMAS (Top 10) ---") >>
            IO.println(topLanguages.map { case (l, c) => s"$l -> $c" }.mkString("\n")) >>

            IO.println("\n--- COLECCIONES (Top 10) ---") >>
            IO.println(topCollections.map { case (c, n) => s"${c.take(50)} -> $n" }.mkString("\n")) >>

            IO.println("\n--- STATUS DE PELICULAS ---") >>
            IO.println(freqStatus.map { case (s, c) => s"$s -> $c" }.mkString("\n")) >>

            IO.println("\n--- TAGLINE ---") >>
            IO.println(s"Peliculas con tagline: $totalConTagline") >>
            IO.println(s"Peliculas sin tagline: $totalSinTagline") >>

            IO.println("\n--- TITULOS ---") >>
            IO.println(s"Total de titulos unicos: $totalTitulosUnicos") >>
            IO.println("\nMuestra de titulos (20):") >>
            IO.println(muestraTitulos.map(t => s"- $t").mkString("\n")) >>

            IO.println("=" * 70 + "\n")
          )
      }
