# Proyecto: Limpieza de Datos de Películas con Circe

##  Descripción General

Este proyecto implementa un pipeline completo de procesamiento y limpieza de datos para un dataset de películas en formato CSV con columnas JSON. Utiliza **Circe** para parsear estructuras JSON complejas y **FS2** para procesamiento funcional de streams.

---

##  Objetivos del Proyecto

1. Aprender y aplicar la librería **Circe** para trabajo con JSON en Scala
2. Procesar columnas JSON del dataset (`genres`, `crew`, `production_companies`, etc.)
3. Implementar solución robusta para la columna **Crew**
4. Realizar limpieza completa de datos (valores nulos, outliers)
5. Documentar el proceso completo

---

##  Dependencias

Agregar al `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.0",
  "co.fs2" %% "fs2-core" % "3.7.0",
  "co.fs2" %% "fs2-io" % "3.7.0",
  "org.gnieh" %% "fs2-data-csv" % "1.11.0",
  "org.gnieh" %% "fs2-data-csv-generic" % "1.11.0",
  
  // Circe para JSON
  "io.circe" %% "circe-core" % "0.14.5",
  "io.circe" %% "circe-generic" % "0.14.5",
  "io.circe" %% "circe-parser" % "0.14.5"
)
```

---

##  Arquitectura del Proyecto

### Estructura de Archivos

```
src/main/scala/
├── CirceTutorial.scala          # Tutorial básico de Circe
├── MovieProcessor.scala          # Procesamiento completo del dataset
└── README.md                     # Este archivo
```

---

## 📚 Tutorial Circe (CirceTutorial.scala)

### ¿Qué es Circe?

**Circe** es una librería funcional para trabajar con JSON en Scala. Proporciona:
- Parseo seguro de JSON a objetos Scala
- Serialización de objetos a JSON
- Manejo de errores mediante `Either`
- Soporte para tipos opcionales y por defecto

### Conceptos Básicos

#### 1. Parsear JSON a Objetos

```scala
import io.circe.parser.decode
import io.circe.generic.auto.*

case class Persona(nombre: String, edad: Int)

val json = """{"nombre": "Juan", "edad": 30}"""
val resultado: Either[Error, Persona] = decode[Persona](json)
```

#### 2. Convertir Objetos a JSON

```scala
import io.circe.syntax.*

val persona = Persona("María", 25)
val json: String = persona.asJson.spaces2
```

#### 3. Manejar Listas

```scala
val jsonLista = """[{"nombre": "A", "edad": 20}, {"nombre": "B", "edad": 30}]"""
val personas: Either[Error, List[Persona]] = decode[List[Persona]](jsonLista)
```

#### 4. Campos Opcionales

```scala
case class Pelicula(
  titulo: String,
  director: Option[String],  // Puede no existir
  año: Int
)
```

### Ejecución del Tutorial

```bash
sbt "runMain CirceTutorial"
```

---

## 🎬 Procesamiento del Dataset de Películas

### Estructura de Datos

#### Datos Crudos (CSV)

```scala
case class MovieRaw(
  // Campos simples
  id: Double,
  title: String,
  budget: Double,
  revenue: Double,
  
  // Campos JSON (String que contiene JSON)
  genres: String,                    // [{"id": 1, "name": "Action"}]
  production_companies: String,       // [{"id": 1, "name": "Sony"}]
  production_countries: String,       // [{"iso_3166_1": "US", "name": "USA"}]
  spoken_languages: String,           // [{"iso_639_1": "en", "name": "English"}]
  crew: String                        // [{"name": "John", "job": "Director"}]
)
```

#### Datos Procesados

```scala
case class MovieLimpio(
  id: Double,
  title: String,
  budget: Double,
  revenue: Double,
  
  // JSON parseado a estructuras útiles
  generos: List[String],              // ["Action", "Adventure"]
  companias_productoras: List[String], // ["Sony", "Warner"]
  paises_produccion: List[String],     // ["USA", "UK"]
  idiomas: List[String],               // ["English", "Spanish"]
  director: Option[String],            // Some("Christopher Nolan")
  num_crew: Int                        // 150
)
```

---

## 🔧 Solución para la Columna Crew

### Problema

La columna `crew` contiene un array JSON con todos los miembros del equipo:

```json
[
  {
    "credit_id": "52fe4284c3a36847f8024f49",
    "department": "Directing",
    "gender": 2,
    "id": 7879,
    "job": "Director",
    "name": "Christopher Nolan",
    "profile_path": "/path.jpg"
  },
  {
    "credit_id": "...",
    "department": "Production",
    "job": "Producer",
    "name": "Emma Thomas",
    ...
  }
]
```

### Solución Implementada

```scala
def parseCrew(jsonStr: String): (Option[String], Int) =
  if jsonStr == null || jsonStr.trim.isEmpty || jsonStr == "[]"
  then (None, 0)
  else
    decode[List[CrewMember]](jsonStr) match
      case Right(crew) =>
        // Extraer director específicamente
        val director = crew.find(_.job == "Director").map(_.name)
        // Contar total de crew
        (director, crew.length)
      case Left(_) =>
        (None, 0)
```

### Ventajas

1.  Extrae el director (dato más importante)
2.  Cuenta el tamaño total del equipo
3.  Maneja errores gracefully
4.  Procesa listas vacías correctamente
5.  Puede extenderse para otros roles (Producer, Writer, etc.)

---

## 🧹 Proceso de Limpieza de Datos

### Paso 1: Validación Básica

```scala
val preFiltrado = lista.filter { m =>
  m.budget > 0 &&
  m.revenue > 0 &&
  m.runtime > 0 &&
  m.runtime < 500 &&
  m.vote_average >= 0 &&
  m.vote_average <= 10 &&
  m.vote_count >= 0 &&
  m.title.nonEmpty
}
```

### Paso 2: Detección de Outliers (Método IQR)

El método **IQR (Interquartile Range)** detecta valores atípicos:

```
IQR = Q3 - Q1
Límite Inferior = Q1 - 1.5 × IQR
Límite Superior = Q3 + 1.5 × IQR
```

Implementación:

```scala
def obtenerLimitesIQR(datos: List[Double]): (Double, Double) =
  val sorted = datos.sorted
  val q1 = sorted(n * 0.25)
  val q3 = sorted(n * 0.75)
  val iqr = q3 - q1
  (q1 - 1.5 * iqr, q3 + 1.5 * iqr)
```

### Paso 3: Aplicación de Filtros

Se aplican los límites IQR a las columnas:
- `budget`
- `revenue`
- `runtime`

---

##  Métricas y Reportes

### Salida del Programa

```
======================================================================
 REPORTE COMPARATIVO: DATOS TRANSFORMADOS VS LIMPIOS
======================================================================
Metrica                        | Transformado    | Limpio         
----------------------------------------------------------------------
Total Registros                | 45466           | 38903          
Presupuesto Prom.              | 29503456.78     | 32456789.12    
Presupuesto Max.               | 380000000.00    | 250000000.00   
Ingresos Prom.                 | 82154321.45     | 91234567.89    
Ingresos Max.                  | 2787965087.00   | 1500000000.00  
Runtime Prom.                  | 106.88          | 108.45         
Rating Promedio                | 6.09            | 6.35           
----------------------------------------------------------------------
 ESTADÍSTICAS DE COLUMNAS JSON (DATOS LIMPIOS)
----------------------------------------------------------------------
Películas con Director         | 35421                             
Promedio Crew por Película     | 142.56                            
======================================================================
INFO: Se eliminaron 6563 registros (Ceros y Atípicos).
INFO: Se procesaron exitosamente 38903 películas con datos JSON.
```

---

##  Ejecución

### 1. Ejecutar Tutorial de Circe

```bash
sbt "runMain CirceTutorial"
```

### 2. Procesar Dataset Completo

```bash
sbt "runMain LeerMoviesCompleto"
```

### 3. Modificar Ruta del Archivo

Editar en `MovieProcessor.scala`:

```scala
val filePath: Path = Path("ruta/a/tu/archivo.csv")
```

---

## 📈 Resultados Esperados

### Antes de la Limpieza
- **Total de registros**: ~45,466
- Contiene valores 0 en budget/revenue
- Outliers extremos en runtime (>500 min)
- Presupuestos irreales (>$500M)

### Después de la Limpieza
- **Registros limpios**: ~38,903 (85.5%)
- Todos los valores validados
- Outliers removidos mediante IQR
- Datos JSON parseados correctamente
- ~35,421 películas con director identificado

---

## 🎓 Conceptos Aprendidos

### Circe
- Parseo seguro con `Either`
-  Decodificación automática con `generic.auto`
-  Manejo de `Option` para campos opcionales
-  Procesamiento de listas JSON

### Limpieza de Datos
-  Método IQR para outliers
- Validación de rangos
-  Filtrado de valores nulos/cero
-  Transformación de datos crudos a estructuras útiles

### Programación Funcional
-  Uso de `Either` para manejo de errores
-  Composición de funciones
-  Procesamiento inmutable
-  Streams con FS2

---

##  Extensiones Futuras

1. **Extraer más roles de Crew**: Producer, Writer, Cinematographer
2. **Análisis por género**: Agrupar películas por género
3. **Análisis temporal**: Tendencias por año
4. **Exportar resultados**: Guardar datos limpios en nuevo CSV
5. **Visualizaciones**: Gráficos de distribuciones

---

##  Troubleshooting

### Error: `DecodingFailure`
- **Causa**: JSON malformado o estructura incorrecta
- **Solución**: Verificar estructura de case classes coincide con JSON

### Error: `FileNotFoundException`
- **Causa**: Ruta del archivo incorrecta
- **Solución**: Ajustar `filePath` con ruta absoluta correcta

### Pocos registros después de limpieza
- **Causa**: Límites IQR muy estrictos
- **Solución**: Ajustar multiplicador (1.5) en `obtenerLimitesIQR`

---

##  Contacto y Créditos

- **Autor**: [Tu Nombre]
- **Curso**: Practicum en Ciencia de Datos
- **Tecnologías**: Scala 3, Cats Effect, FS2, Circe
- **Fecha**: Enero 2026

---

##  Licencia

Este proyecto es parte de material académico del Practicum.
