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