# Limpieza de la Columna Crew - Documentación

##  Descripción General

Este módulo procesa y limpia la columna `crew` del dataset de películas, que contiene información del equipo de producción en formato JSON. El objetivo es extraer los nombres y roles de cada miembro del crew **manteniendo la relación con cada película**.

---

##  Objetivo Principal

**Convertir esto:**
```csv
id;title;crew
19995;Avatar;"[{'name': 'James Cameron', 'job': 'Director', 'id': None}, ...]"
```

**En esto:**
```scala
MovieWithCrew(
  movieId = 19995.0,
  title = "Avatar",
  crew = List(
    Crew(name = Some("James Cameron"), job = Some("Director"), ...),
    Crew(name = Some("Jon Landau"), job = Some("Producer"), ...),
    ...
  )
)
```

---

##  Estructura del Código

### 1. Modelos de Datos

```scala
// Representa un miembro del crew
case class Crew(
  credit_id: Option[String],
  department: Option[String],
  id: Option[Int],
  job: Option[String],
  name: Option[String],
  profile_path: Option[String]
)

// Representa una película con su crew asociado
case class MovieWithCrew(
  movieId: Double,
  title: String,
  crew: List[Crew]  // ← Mantiene la relación
)
```

**¿Por qué `Option`?** 
Porque algunos campos pueden estar vacíos o ser `null` en el JSON original.

---

##  Funciones Principales

### 1. Preparar JSON para Parseo

**Problema:** El JSON en el CSV viene en formato Python, no JSON válido.

```scala
def prepararJSONCrewParaParseo(crew: String): String =
  crew.trim
    .replaceAll("None", "null")      // Python → JSON
    .replaceAll("True", "true")
    .replaceAll("False", "false")
    .replace("\"", "\\\"")           // Escapar comillas
    .replaceAll("(?<![a-zA-Z0-9])'|'(?![a-zA-Z0-9])", "\"")  // ' → "
```

**Entrada:**
```python
[{'name': 'John Doe', 'id': None, 'job': True}]
```

**Salida:**
```json
[{"name": "John Doe", "id": null, "job": true}]
```

---

### 2. Normalizar Texto

Limpia espacios múltiples y convierte strings vacíos en `None`:

```scala
def normalizarTexto(txt: String): Option[String] =
  val limpio = txt.trim.replaceAll("\\s+", " ")
  if (limpio.isEmpty) None else Some(limpio)
```

**Ejemplos:**
- `"  John   Smith  "` → `Some("John Smith")`
- `""` → `None`
- `"   "` → `None`

---

### 3. Parsear CSV Respetando Comillas

**Problema:** El CSV usa `;` como separador, pero el JSON también puede contener `;`.

```scala
def parseCSVLine(line: String): Array[String] =
  val (fields, lastBuilder, _) = line.foldLeft(
    (Vector.empty[String], new StringBuilder, false)
  ) {
    case ((fields, current, inQuotes), char) => char match {
      case '"' => (fields, current, !inQuotes)
      case ';' if !inQuotes => (fields :+ current.toString, new StringBuilder, false)
      case _ => current.append(char); (fields, current, inQuotes)
  }
```



---

### 4. Parsear Crew de una Celda

Pipeline completo de limpieza y parseo:

```scala
def parsearCrewDeCelda(crewStr: String): List[Crew] =
  if (crewStr.trim.isEmpty || crewStr == "[]") then
    List.empty
  else
    try {
      val jsonLimpio = prepararJSONCrewParaParseo(crewStr)
      decode[List[Crew]](jsonLimpio) match {
        case Right(crews) => crews.map(normalizarCrewMember)
        case Left(error) => List.empty
      }
    } catch {
      case e: Exception => List.empty
    }
```

**Flujo:**
1. Verificar si está vacío
2. Limpiar JSON (formato Python → JSON válido)
3. Decodificar con Circe
4. Normalizar cada miembro
5. Si hay error, retornar lista vacía

---

**Pasos:**
1. Leer archivo con FS2 (seguro, funcional)
2. Dividir en líneas
3. Obtener índices de columnas del header
4. Para cada línea:
   - Parsear respetando comillas
   - Extraer ID, título, crew
   - Parsear JSON de crew
   - Crear `MovieWithCrew` si tiene crew válido

---


##  Estadísticas Generadas

### 1. Contadores Generales

```scala
val totalMovies = moviesConCrew.length
val totalCrewMembers = moviesConCrew
  .flatMap(_.crew)
  .distinctBy(c => (c.id, c.name, c.job))
  .length

val peliculasConDirector = moviesConCrew.count(
  _.crew.exists(_.job.exists(_.contains("Director")))
)
```





##  Tecnologías Utilizadas

- **Scala 3**: Lenguaje funcional
- **Cats Effect**: Manejo de efectos (IO)
- **FS2**: Streams funcionales para archivos
- **Circe**: Parseo de JSON
- **Pattern Matching**: Manejo elegante de casos

---

 
**Proyecto**: Practicum - Limpieza de Datos de Películas  
message.txt
5 KB


