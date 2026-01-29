package untilies

import models.Movie_Raw

object ValidacionMovies {

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

  def isValidBooleanString(s: String): Boolean =
    val normalized = s.trim.toLowerCase
    normalized == "true" || normalized == "false" || normalized == "1" || normalized == "0"

  // ==========================================
  // FUNCIONES DE LIMPIEZA DE DATOS
  // ==========================================

  /** Limpia valores numéricos: convierte negativos a positivos, mantiene 0 */
  def limpiarNumerico(valor: Double): Double =
    if (valor < 0) valor * -1 else valor

  def limpiarNumerico(valor: Int): Int =
    if (valor < 0) valor * -1 else valor

  /** Limpia strings: si es nulo/vacío/"null", retorna valor por defecto */
  def limpiarString(s: String, default: String): String =
    if (isValidString(s)) s.trim else default

  /** Limpia strings opcionales: si es nulo/vacío/"null", retorna cadena vacía */
  def limpiarStringOpcional(s: String): String =
    if (s == null || s.trim.isEmpty || s.equalsIgnoreCase("null")) "" else s.trim

  /** Limpia booleanos como string: si no es válido, retorna "false" */
  def limpiarBooleano(s: String): String =
    if (isValidBooleanString(s)) s.trim.toLowerCase else "false"

  /** Valida y limpia fecha en formato YYYY-MM-DD */
  def limpiarFecha(s: String): String =
    if (s != null && s.matches("\\d{4}-\\d{2}-\\d{2}")) s else "1900-01-01"

  /** Valida y limpia código de idioma (2-3 caracteres) */
  def limpiarIdioma(s: String): String =
    if (isValidString(s) && s.length >= 2 && s.length <= 3) s.trim else "en"

  // ==========================================
  // CÁLCULO DE LÍMITES IQR
  // ==========================================
  def calcularCuartil(ordenados: List[Double], percentil: Double): Double =
    if (ordenados.isEmpty) return 0.0
    val pos = percentil * (ordenados.size - 1)
    val lower = ordenados(pos.toInt)
    val upper = if (pos.toInt + 1 < ordenados.size) then ordenados(pos.toInt + 1) else lower
    val fraction = pos - pos.toInt
    lower + fraction * (upper - lower)

  def obtenerLimitesIQR(datos: List[Double]): (Double, Double) =
    if (datos.isEmpty || datos.size < 4) then (0.0, Double.MaxValue)
    else
      val sorted = datos.sorted
      val q1 = calcularCuartil(sorted, 0.25)
      val q3 = calcularCuartil(sorted, 0.75)
      val iqr = q3 - q1
      val limiteInferior = math.max(0, q1 - 1.5 * iqr)
      val limiteSuperior = q3 + 1.5 * iqr
      (limiteInferior, limiteSuperior)

  // ==========================================
  // LIMPIEZA COMPLETA CON TRANSFORMACIÓN DE DATOS
  // ==========================================
  def limpiarDatosCompletos(lista: List[Movie_Raw]): List[Movie_Raw] =
    // TRANSFORMACIÓN Y LIMPIEZA DE DATOS (mantiene TODOS los registros)
    lista.map { m =>
      m.copy(
        // Numéricos: convertir negativos a positivos
        id = limpiarNumerico(m.id),
        budget = limpiarNumerico(m.budget),
        revenue = limpiarNumerico(m.revenue),
        runtime = limpiarNumerico(m.runtime),
        popularity = limpiarNumerico(m.popularity),
        vote_average = math.min(10.0, limpiarNumerico(m.vote_average)), // Máximo 10
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