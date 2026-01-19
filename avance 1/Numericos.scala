package Proyecto

import cats.effect.{IO, IOApp}
import fs2.text
import fs2.io.file.{Files, Path}
import fs2.data.csv.*
import fs2.data.csv.generic.semiauto.*

case class MovieNumericos(
                           id: Double,
                           budget: Double,
                           popularity: Double,
                           revenue: Double,
                           runtime: Double,
                           vote_average: Double,
                           vote_count: Double
                         )

given CsvRowDecoder[MovieNumericos, String] =
  deriveCsvRowDecoder[MovieNumericos]

object LeerMoviesNumericos extends IOApp.Simple:

  val filePath: Path =
    Path("C:\\Users\\Usuario iTC\\Desktop\\Practicum\\src\\main\\resources\\Data\\pi-movies-complete-2026-01-14.csv")

  val run: IO[Unit] =
    Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(decodeUsingHeaders[MovieNumericos](';'))
      .attempt
      .collect { case Right(movie) => movie }
      .compile
      .toList
      .flatMap { movies =>

        // ===============================
        // 5.2 Lectura de columnas numéricas
        // ===============================
        val ids          = movies.map(_.id)
        val budgets      = movies.map(_.budget)
        val popularities = movies.map(_.popularity)
        val revenues     = movies.map(_.revenue)
        val runtimes     = movies.map(_.runtime)
        val votes        = movies.map(_.vote_average)
        val voteCounts   = movies.map(_.vote_count)

        val linea = "=" * 70
        val separador = "-" * 70

        // 🖨 Mostrar lectura de columnas
        IO.println("\n" + linea) >>
          IO.println(" 5.2 LECTURA DE COLUMNAS NUMERICAS") >>
          IO.println(linea) >>
          IO.println(s"Total peliculas validas: ${movies.length}") >>
          IO.println(s"\nMuestra de IDs (primeros 10): ${ids.take(10).mkString(", ")}") >>
          IO.println(s"Muestra de Budgets (primeros 10): ${budgets.take(10).map(_.toInt).mkString(", ")}") >>
          IO.println(s"Muestra de Popularities (primeros 10): ${popularities.take(10).map(p => f"$p%.2f").mkString(", ")}") >>
          IO.println(s"Muestra de Revenues (primeros 10): ${revenues.take(10).map(_.toInt).mkString(", ")}") >>
          IO.println(s"Muestra de Runtimes (primeros 10): ${runtimes.take(10).map(_.toInt).mkString(", ")}") >>
          IO.println(s"Muestra de Vote Averages (primeros 10): ${votes.take(10).map(v => f"$v%.1f").mkString(", ")}") >>
          IO.println(s"Muestra de Vote Counts (primeros 10): ${voteCounts.take(10).map(_.toInt).mkString(", ")}") >>
          {
            // ===============================
            // 5.3 Análisis de datos en columnas numéricas (estadísticas básicas)
            // ===============================
            def stats(values: List[Double]): (Double, Double, Double) =
              if values.isEmpty then (0.0, 0.0, 0.0)
              else (values.sum / values.length, values.min, values.max)

            val (promBudget, minBudget, maxBudget) = stats(budgets)
            val (promPopularity, minPop, maxPop) = stats(popularities)
            val (promRevenue, minRev, maxRev) = stats(revenues)
            val (promRuntime, minRun, maxRun) = stats(runtimes)
            val (promVote, minVote, maxVote) = stats(votes)
            val (promVoteCount, minVC, maxVC) = stats(voteCounts)

            // 🖨 Mostrar estadísticas básicas
            IO.println("\n" + linea) >>
              IO.println(" 5.3 ANALISIS DE DATOS - ESTADISTICAS BASICAS") >>
              IO.println(linea) >>
              IO.println(f"${"Columna"}%-20s | ${"Promedio"}%-15s | ${"Minimo"}%-15s | ${"Maximo"}%-15s") >>
              IO.println(separador) >>
              IO.println(f"${"BUDGET"}%-20s | ${promBudget}%-15.2f | ${minBudget}%-15.2f | ${maxBudget}%-15.2f") >>
              IO.println(f"${"POPULARITY"}%-20s | ${promPopularity}%-15.2f | ${minPop}%-15.2f | ${maxPop}%-15.2f") >>
              IO.println(f"${"REVENUE"}%-20s | ${promRevenue}%-15.2f | ${minRev}%-15.2f | ${maxRev}%-15.2f") >>
              IO.println(f"${"RUNTIME"}%-20s | ${promRuntime}%-15.2f | ${minRun}%-15.2f | ${maxRun}%-15.2f") >>
              IO.println(f"${"VOTE_AVERAGE"}%-20s | ${promVote}%-15.2f | ${minVote}%-15.2f | ${maxVote}%-15.2f") >>
              IO.println(f"${"VOTE_COUNT"}%-20s | ${promVoteCount}%-15.2f | ${minVC}%-15.2f | ${maxVC}%-15.2f") >>
              IO.println(linea + "\n")
          }
      }