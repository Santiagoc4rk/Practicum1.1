package Limpieza

import cats.effect.{IO, IOApp}
import fs2.text
import fs2.io.file.{Files, Path}
import fs2.data.csv.*
import fs2.data.csv.generic.semiauto.*

// ==========================================
// CASE CLASS COMPLETA - 28 COLUMNAS
// ==========================================
case class MovieCompleta(
                          adult: String,
                          budget: Double,
                          homepage: String,
                          id: Double,
                          imdb_id: String,
                          original_language: String,
                          original_title: String,
                          overview: String,
                          popularity: Double,
                          poster_path: String,
                          release_date: String,
                          revenue: Double,
                          runtime: Double,
                          status: String,
                          tagline: String,
                          title: String,
                          video: String,
                          vote_average: Double,
                          vote_count: Double,
                        )

given CsvRowDecoder[MovieCompleta, String] = deriveCsvRowDecoder[MovieCompleta]

// ==========================================
// MAIN
// ==========================================
object LimpiezaCompletaMovies extends IOApp.Simple:

  val filePath: Path =
    Path("C:\\Users\\Usuario iTC\\Desktop\\Practicum\\src\\main\\resources\\Data\\pi-movies-complete-2026-01-14.csv")

  // ==========================================
  // FUNCIONES DE VALIDACIÓN
  // ==========================================
  def isValidString(s: String): Boolean =
    s != null && s.trim.nonEmpty && !s.equalsIgnoreCase("null")

  def isValidOptionalUrl(s: String): Boolean =
    s == null || s.trim.isEmpty || s.equalsIgnoreCase("null") || s.startsWith("http")

  def isValidOptionalImdbId(s: String): Boolean =
    s == null || s.trim.isEmpty || s.equalsIgnoreCase("null") || s.startsWith("tt")

  def isValidOptionalPosterPath(s: String): Boolean =
    s == null || s.trim.isEmpty || s.equalsIgnoreCase("null") || s.startsWith("/")

  def isValidOptionalTagline(s: String): Boolean =
    s == null || s.trim.isEmpty || s.equalsIgnoreCase("null") || isValidString(s)

  def isValidBoolean(s: String): Boolean =
    val normalized = s.trim.toLowerCase
    normalized == "true" || normalized == "false" || normalized == "1" || normalized == "0"

  // ==========================================
  // CÁLCULO DE LÍMITES IQR
  // ==========================================
  def calcularCuartil(ordenados: List[Double], percentil: Double): Double =
    if ordenados.isEmpty then return 0.0
    val pos = percentil * (ordenados.size - 1)
    val lower = ordenados(pos.toInt)
    val upper = if pos.toInt + 1 < ordenados.size then ordenados(pos.toInt + 1) else lower
    val fraction = pos - pos.toInt
    lower + fraction * (upper - lower)

  def obtenerLimitesIQR(datos: List[Double]): (Double, Double) =
    if datos.isEmpty || datos.size < 4 then (0.0, Double.MaxValue)
    else
      val sorted = datos.sorted
      val q1 = calcularCuartil(sorted, 0.25)
      val q3 = calcularCuartil(sorted, 0.75)
      val iqr = q3 - q1
      val limiteInferior = math.max(0, q1 - 1.5 * iqr)
      val limiteSuperior = q3 + 1.5 * iqr
      (limiteInferior, limiteSuperior)

  // ==========================================
  // LIMPIEZA COMPLETA CON VALIDACIÓN INTELIGENTE
  // ==========================================
  def limpiarDatosCompletos(lista: List[MovieCompleta]): List[MovieCompleta] =
    // PASO 1: FILTROS DE VALIDEZ BÁSICA
    val paso1 = lista.filter { m =>
      // Numéricos obligatorios
      m.id > 0 &&
        m.budget >= 0 &&
        m.revenue >= 0 &&
        m.runtime > 0 && m.runtime <= 300 &&
        m.popularity >= 0 &&
        m.vote_average >= 0 && m.vote_average <= 10 &&
        m.vote_count >= 0 &&
        // Texto obligatorios
        isValidString(m.title) &&
        isValidString(m.original_title) &&
        isValidString(m.overview) &&  // OBLIGATORIO - crítico para análisis
        isValidString(m.release_date) &&
        m.release_date.matches("\\d{4}-\\d{2}-\\d{2}") &&
        // Booleanos (validación flexible)
        isValidBoolean(m.adult) &&
        isValidBoolean(m.video) &&
        // Códigos
        isValidString(m.status) &&
        isValidString(m.original_language) &&
        m.original_language.length >= 2 && m.original_language.length <= 3 &&
        // Opcionales con validación de formato
        isValidOptionalUrl(m.homepage) &&
        isValidOptionalImdbId(m.imdb_id) &&
        isValidOptionalPosterPath(m.poster_path) &&
        isValidOptionalTagline(m.tagline)
    }

    // PASO 2: FILTROS DE OUTLIERS (IQR) - Solo si hay suficientes datos
    if paso1.size < 50 then
      paso1  // No aplicar IQR con pocos datos
    else
      // Solo aplicar IQR a Budget y Revenue si son > 0
      val budgetsNoZero = paso1.map(_.budget).filter(_ > 0)
      val revenuesNoZero = paso1.map(_.revenue).filter(_ > 0)

      val (limInfBudget, limSupBudget) = obtenerLimitesIQR(budgetsNoZero)
      val (limInfRev, limSupRev) = obtenerLimitesIQR(revenuesNoZero)

      // Permitir hasta 1 outlier (budget o revenue pueden ser outliers)
      // Solo evaluar outliers en valores > 0
      paso1.filter { m =>
        val fueraDeRango = Seq(
          m.budget > 0 && (m.budget < limInfBudget || m.budget > limSupBudget),
          m.revenue > 0 && (m.revenue < limInfRev || m.revenue > limSupRev)
        ).count(identity)

        fueraDeRango <= 1  // Máximo 1 de las 2 variables puede ser outlier
      }

  // ==========================================
  // ESTADÍSTICAS
  // ==========================================
  def calcularEstadisticas(datos: List[Double]): Map[String, Double] =
    if datos.isEmpty then Map.empty
    else
      val ordenados = datos.sorted
      val n = ordenados.size
      val media = datos.sum / n
      val varianza = datos.map(x => math.pow(x - media, 2)).sum / n
      val mediana = if n % 2 == 1 then ordenados(n / 2)
      else (ordenados(n / 2 - 1) + ordenados(n / 2)) / 2.0

      Map(
        "min" -> ordenados.head,
        "max" -> ordenados.last,
        "media" -> media,
        "mediana" -> mediana,
        "desv_std" -> math.sqrt(varianza),
        "q1" -> calcularCuartil(ordenados, 0.25),
        "q3" -> calcularCuartil(ordenados, 0.75)
      )

  def contarNulos(lista: List[MovieCompleta], extractor: MovieCompleta => String): Int =
    lista.count(m => !isValidString(extractor(m)))

  // ==========================================
  // FORMATO DE NÚMEROS
  // ==========================================
  def formatNumber(n: Double): String =
    if n >= 1000000 then f"${n / 1000000}%.2fM"
    else if n >= 1000 then f"${n / 1000}%.2fK"
    else f"$n%.2f"

  // ==========================================
  // RUN
  // ==========================================
  val run: IO[Unit] =
    Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(decodeUsingHeaders[MovieCompleta](';'))
      .attempt
      .collect { case Right(movie) => movie }
      .compile
      .toList
      .flatMap { rawMovies =>

        val cleanMovies = limpiarDatosCompletos(rawMovies)

        val totalRaw = rawMovies.size
        val totalClean = cleanMovies.size
        val eliminados = totalRaw - totalClean
        val porcentajeRetenido = if totalRaw > 0 then (totalClean.toDouble / totalRaw * 100) else 0.0

        // Estadísticas ANTES y DESPUÉS
        val statsBudgetRaw = calcularEstadisticas(rawMovies.map(_.budget).filter(_ > 0))
        val statsRevenueRaw = calcularEstadisticas(rawMovies.map(_.revenue).filter(_ > 0))
        val statsRuntimeRaw = calcularEstadisticas(rawMovies.map(_.runtime).filter(_ > 0))
        val statsVoteRaw = calcularEstadisticas(rawMovies.map(_.vote_average).filter(_ >= 0))

        val statsBudgetClean = calcularEstadisticas(cleanMovies.map(_.budget).filter(_ > 0))
        val statsRevenueClean = calcularEstadisticas(cleanMovies.map(_.revenue).filter(_ > 0))
        val statsRuntimeClean = calcularEstadisticas(cleanMovies.map(_.runtime))
        val statsVoteClean = calcularEstadisticas(cleanMovies.map(_.vote_average))

        // Contadores
        val nulosTitle = contarNulos(rawMovies, _.title)
        val nulosOriginalTitle = contarNulos(rawMovies, _.original_title)
        val nulosOverview = contarNulos(rawMovies, _.overview)
        val nulosTagline = contarNulos(rawMovies, _.tagline)
        val nulosHomepage = contarNulos(rawMovies, _.homepage)
        val nulosImdbId = contarNulos(rawMovies, _.imdb_id)
        val nulosPoster = contarNulos(rawMovies, _.poster_path)
        val nulosStatus = contarNulos(rawMovies, _.status)
        val nulosLanguage = contarNulos(rawMovies, _.original_language)
        val nulosReleaseDate = contarNulos(rawMovies, _.release_date)

        val invalidId = rawMovies.count(_.id <= 0)
        val invalidBudget = rawMovies.count(_.budget < 0)
        val invalidRevenue = rawMovies.count(_.revenue < 0)
        val invalidRuntime = rawMovies.count(m => m.runtime <= 0 || m.runtime > 300)
        val invalidPopularity = rawMovies.count(_.popularity < 0)
        val invalidVoteCount = rawMovies.count(_.vote_count < 0)
        val invalidAdult = rawMovies.count(m => !isValidBoolean(m.adult))
        val invalidVideo = rawMovies.count(m => !isValidBoolean(m.video))
        val invalidHomepage = rawMovies.count(m => !isValidOptionalUrl(m.homepage))
        val invalidImdbId = rawMovies.count(m => !isValidOptionalImdbId(m.imdb_id))
        val invalidPoster = rawMovies.count(m => !isValidOptionalPosterPath(m.poster_path))
        val invalidTagline = rawMovies.count(m => !isValidOptionalTagline(m.tagline))
        val invalidReleaseDate = rawMovies.count(m => !m.release_date.matches("\\d{4}-\\d{2}-\\d{2}"))

        for
          _ <- IO.println("\n" + "=" * 100)
          _ <- IO.println(" " * 20 + "REPORTE DE LIMPIEZA - DATASET DE PELICULAS (28 COLUMNAS)")
          _ <- IO.println("=" * 100)

          _ <- IO.println(f"  Total registros originales:  $totalRaw%5d")
          _ <- IO.println(f"  Total registros limpios:     $totalClean%5d")
          _ <- IO.println(f"  Registros eliminados:        $eliminados%5d")
          _ <- IO.println(f"  Porcentaje retenido:         $porcentajeRetenido%5.2f%%")

          _ <- IO.println("\n" + "-" * 100)
          _ <- IO.println("  1. ESTADISTICAS COMPARATIVAS (ANTES -> DESPUES)")
          _ <- IO.println("-" * 100)

          _ <- IO.println("\n  [BUDGET]")
          _ <- IO.println(f"     Media:        ${formatNumber(statsBudgetRaw.getOrElse("media", 0.0))}%12s  ->  ${formatNumber(statsBudgetClean.getOrElse("media", 0.0))}%12s")
          _ <- IO.println(f"     Mediana:      ${formatNumber(statsBudgetRaw.getOrElse("mediana", 0.0))}%12s  ->  ${formatNumber(statsBudgetClean.getOrElse("mediana", 0.0))}%12s")
          _ <- IO.println(f"     Minimo:       ${formatNumber(statsBudgetRaw.getOrElse("min", 0.0))}%12s  ->  ${formatNumber(statsBudgetClean.getOrElse("min", 0.0))}%12s")
          _ <- IO.println(f"     Maximo:       ${formatNumber(statsBudgetRaw.getOrElse("max", 0.0))}%12s  ->  ${formatNumber(statsBudgetClean.getOrElse("max", 0.0))}%12s")

          _ <- IO.println("\n  [REVENUE]")
          _ <- IO.println(f"     Media:        ${formatNumber(statsRevenueRaw.getOrElse("media", 0.0))}%12s  ->  ${formatNumber(statsRevenueClean.getOrElse("media", 0.0))}%12s")
          _ <- IO.println(f"     Mediana:      ${formatNumber(statsRevenueRaw.getOrElse("mediana", 0.0))}%12s  ->  ${formatNumber(statsRevenueClean.getOrElse("mediana", 0.0))}%12s")
          _ <- IO.println(f"     Minimo:       ${formatNumber(statsRevenueRaw.getOrElse("min", 0.0))}%12s  ->  ${formatNumber(statsRevenueClean.getOrElse("min", 0.0))}%12s")
          _ <- IO.println(f"     Maximo:       ${formatNumber(statsRevenueRaw.getOrElse("max", 0.0))}%12s  ->  ${formatNumber(statsRevenueClean.getOrElse("max", 0.0))}%12s")

          _ <- IO.println("\n  [RUNTIME]")
          _ <- IO.println(f"     Media:        ${statsRuntimeRaw.getOrElse("media", 0.0)}%7.2f min  ->  ${statsRuntimeClean.getOrElse("media", 0.0)}%7.2f min")
          _ <- IO.println(f"     Mediana:      ${statsRuntimeRaw.getOrElse("mediana", 0.0)}%7.2f min  ->  ${statsRuntimeClean.getOrElse("mediana", 0.0)}%7.2f min")

          _ <- IO.println("\n  [VOTE AVERAGE]")
          _ <- IO.println(f"     Media:        ${statsVoteRaw.getOrElse("media", 0.0)}%7.2f     ->  ${statsVoteClean.getOrElse("media", 0.0)}%7.2f")
          _ <- IO.println(f"     Mediana:      ${statsVoteRaw.getOrElse("mediana", 0.0)}%7.2f     ->  ${statsVoteClean.getOrElse("mediana", 0.0)}%7.2f")

          _ <- IO.println("\n" + "-" * 100)
          _ <- IO.println("  2. VALORES NULOS Y VACIOS (Datos Originales)")
          _ <- IO.println("-" * 100)
          _ <- IO.println(f"     Title:                    $nulosTitle%5d")
          _ <- IO.println(f"     Original Title:           $nulosOriginalTitle%5d")
          _ <- IO.println(f"     Overview:                 $nulosOverview%5d")
          _ <- IO.println(f"     Release Date:             $nulosReleaseDate%5d")
          _ <- IO.println(f"     Tagline:                  $nulosTagline%5d")
          _ <- IO.println(f"     Homepage:                 $nulosHomepage%5d")
          _ <- IO.println(f"     IMDB ID:                  $nulosImdbId%5d")
          _ <- IO.println(f"     Poster Path:              $nulosPoster%5d")
          _ <- IO.println(f"     Status:                   $nulosStatus%5d")
          _ <- IO.println(f"     Original Language:        $nulosLanguage%5d")

          _ <- IO.println("\n" + "-" * 100)
          _ <- IO.println("  3. VALORES INVALIDOS EN CAMPOS NUMERICOS")
          _ <- IO.println("-" * 100)
          _ <- IO.println(f"     ID (<= 0):                $invalidId%5d")
          _ <- IO.println(f"     Budget (< 0):             $invalidBudget%5d")
          _ <- IO.println(f"     Revenue (< 0):            $invalidRevenue%5d")
          _ <- IO.println(f"     Runtime (<= 0 o > 300):   $invalidRuntime%5d")
          _ <- IO.println(f"     Popularity (< 0):         $invalidPopularity%5d")
          _ <- IO.println(f"     Vote Count (< 0):         $invalidVoteCount%5d")

          _ <- IO.println("\n" + "-" * 100)
          _ <- IO.println("  4. VALORES INVALIDOS EN CAMPOS BOOLEANOS")
          _ <- IO.println("-" * 100)
          _ <- IO.println(f"     Adult (formato inválido):    $invalidAdult%5d")
          _ <- IO.println(f"     Video (formato inválido):    $invalidVideo%5d")

          _ <- IO.println("\n" + "-" * 100)
          _ <- IO.println("  5. FORMATOS INVALIDOS EN CAMPOS OPCIONALES")
          _ <- IO.println("-" * 100)
          _ <- IO.println(f"     Homepage (formato URL):   $invalidHomepage%5d")
          _ <- IO.println(f"     IMDB ID (formato tt):     $invalidImdbId%5d")
          _ <- IO.println(f"     Poster Path (formato /):  $invalidPoster%5d")
          _ <- IO.println(f"     Tagline (formato):        $invalidTagline%5d")
          _ <- IO.println(f"     Release Date (YYYY-MM-DD):$invalidReleaseDate%5d")

          _ <- IO.println("\n" + "=" * 100)
          _ <- IO.println("  [OK] Limpieza completada exitosamente")
          _ <- IO.println("  [OK] Dataset listo para analisis")
          _ <- IO.println("  [VALIDADO] 15 campos obligatorios + 4 campos opcionales con formato")
          _ <- IO.println("  [MEJORAS] Budget/Revenue permiten 0, Runtime 0-300, IQR condicional (>50 registros)")
          _ <- IO.println("  [NOTA] Los campos JSON seran limpiados con Circe posteriormente")
          _ <- IO.println("=" * 100 + "\n")
        yield ()
      }

end LimpiezaCompletaMovies