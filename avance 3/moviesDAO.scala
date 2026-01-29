package dao

import cats.effect.IO
import cats.implicits.*
import config.Database
import doobie.*
import doobie.implicits.*
import models.Movie

object moviesDAO {
  def insert(m: Movie): ConnectionIO[Int] = {
    sql"""
      INSERT IGNORE INTO movies (id_movie, imdb, status, vote_count, revenue,
      vote_average, title, tagline, video, popularity, runtime, adult,
       oficial_language, poster_path, backdrop_path, overview, original_title,
        budget, homepage)
      VALUES (
        ${m.id_movie}, ${m.imdb}, ${m.status}, ${m.vote_count}, ${m.revenue},
        ${m.vote_average}, ${m.title}, ${m.tagline}, ${m.video}, ${m.popularity},
        ${m.runtime}, ${m.adult}, ${m.oficial_language}, ${m.poster_path},${m.backdrop_path},
        ${m.overview},${m.original_title}, ${m.budget},${m.homepage}
     )
      """.update.run
  }

  def consultarID(id: Int): ConnectionIO[Option[Int]] = {
    sql"""
      SELECT id_movie
      FROM movies
      WHERE id_movie = ${id}
      """.query[Int]
      .option
  }

}
