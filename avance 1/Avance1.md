---

# TRABAJO PRÁCTICO: ANÁLISIS Y LIMPIEZA DE DATASET DE PELÍCULAS


---

## TABLA DE CONTENIDOS

### Ítems Desarrollados

1. [Tablas de datos (nombre de columna, tipo, propósito y observaciones)](#1-tablas-de-datos-estructura-del-dataset)
2. [Lectura de columnas numéricas](#2-lectura-de-columnas-numéricas)
3. [Análisis de datos en columnas numéricas (estadísticas básicas)](#3-análisis-de-datos-en-columnas-numéricas)
4. [Análisis de datos en columnas tipo texto (distribución de frecuencia)](#4-análisis-de-datos-en-columnas-tipo-texto)
5. [Limpieza de datos (valores nulos, valores atípicos, etc.)](#5-limpieza-de-datos)

---

# 1. TABLAS DE DATOS: ESTRUCTURA DEL DATASET

### Información General
- **Total de columnas**: 28
- **Formato**: CSV con separador `;`
- **Encoding**: UTF-8
- **Fuente**: The Movie Database (TMDB)

---

## DICCIONARIO DE DATOS

| Nombre de la Columna | Tipo | Propósito | Descripción / Observaciones |
|----------------------|------|-----------|------------------------------|
| **adult** | Boolean | Indicar si la película es solo para adultos | `true` si el contenido es para adultos (+18), `false` si es apta para público general. |
| **belongs_to_collection** | JSON / String | Información de colección | Contiene datos de la saga o colección a la que pertenece la película (id, nombre, póster y fondo). Si está vacío, la película es independiente. **Formato JSON - No incluido en análisis de texto.** |
| **budget** | Double | Presupuesto de producción | Presupuesto total invertido en la producción de la película, expresado en dólares USD. Valores >= 0. |
| **genres** | JSON | Clasificación por género | Lista de géneros asociados a la película (ej: Acción, Drama, Ciencia Ficción), cada uno con un `id` y un `nombre`. **Formato JSON - No incluido en análisis de texto.** |
| **homepage** | String | Sitio web oficial | URL oficial de la película, usada para información promocional o comercial. Campo opcional. Formato validado: debe iniciar con `http`. |
| **id** | Double | Identificador único | Identificador interno del dataset, correspondiente al ID de TMDB. Valor único y obligatorio > 0. |
| **imdb_id** | String | Identificador IMDb | Código único que identifica la película en la plataforma IMDb. Campo opcional. Formato validado: debe iniciar con `tt`. |
| **original_language** | String | Idioma original | Código ISO del idioma original de la película (ej: `en`, `es`, `fr`). Longitud: 2-3 caracteres. Campo obligatorio. |
| **original_title** | String | Título original | Título original de la película, sin traducciones ni adaptaciones locales. Campo obligatorio y no vacío. |
| **overview** | String | Sinopsis | Descripción general de la trama o argumento principal de la película. **Campo crítico y obligatorio** para análisis de contenido. |
| **popularity** | Double | Índice de popularidad | Índice de popularidad calculado por TMDB en base a visitas, búsquedas y actividad del público. Valores >= 0. |
| **poster_path** | String | Ruta del póster | Dirección relativa de la imagen del póster oficial de la película. Campo opcional. Formato validado: debe iniciar con `/`. |
| **production_companies** | JSON | Empresas productoras | Lista de compañías que participaron en la producción, incluyendo país de origen y logotipo. **Formato JSON - No incluido en análisis de texto.** |
| **production_countries** | JSON | Países de producción | Países donde se produjo la película, identificados por código ISO y nombre. **Formato JSON - No incluido en análisis de texto.** |
| **release_date** | String (Date) | Fecha de estreno | Fecha oficial de estreno de la película. Formato obligatorio: `YYYY-MM-DD`. |
| **revenue** | Double | Recaudación total | Ingresos totales obtenidos por la película a nivel mundial, expresados en dólares USD. Valores >= 0. |
| **runtime** | Double | Duración en minutos | Duración total de la película en minutos. Rango válido: 1-300 minutos. Campo obligatorio. |
| **spoken_languages** | JSON | Idiomas hablados | Lista de idiomas que se hablan en la película, con código ISO y nombre. **Formato JSON - No incluido en análisis de texto.** |
| **status** | String | Estado de producción | Indica el estado actual de la película (ej: `Released`, `Post Production`, `In Production`). Campo obligatorio. |
| **tagline** | String | Eslogan promocional | Frase promocional o lema utilizado para promocionar la película. Campo opcional. |
| **title** | String | Título comercial | Título con el que la película es conocida comercialmente (puede estar traducido). Campo obligatorio y no vacío. |
| **video** | Boolean / String | Tipo de contenido | Indica si el registro corresponde a un video especial. Generalmente `false` para películas estándar. |
| **vote_average** | Double | Calificación promedio | Puntaje promedio otorgado por los usuarios en TMDB. Rango válido: 0.0 - 10.0. |
| **vote_count** | Double | Total de votos | Número total de votos recibidos para calcular la calificación promedio. Valores >= 0. |
| **keywords** | JSON | Palabras clave temáticas | Lista de palabras clave asociadas a la temática o elementos importantes de la película. **Formato JSON - No incluido en análisis de texto.** |
| **cast** | JSON | Elenco de actores | Lista de actores que participaron en la película, junto con sus personajes y orden de aparición. **Formato JSON - No incluido en análisis de texto.** |
| **crew** | JSON | Equipo técnico | Información del equipo técnico (director, productor, guionista, etc.) con su rol correspondiente. **Formato JSON - No incluido en análisis de texto.** |
| **ratings** | JSON | Evaluaciones del público | Conjunto de calificaciones individuales del público. Estructura variable según dataset. **Formato JSON - No incluido en análisis de texto.** |

---

### Notas Importantes

**Campos Obligatorios (15):**
- `id`, `title`, `original_title`, `overview`, `release_date`, `status`, `original_language`, `runtime`, `budget`, `revenue`, `popularity`, `vote_average`, `vote_count`, `adult`, `video`

**Campos Opcionales con Validación de Formato (4):**
- `homepage` (URL válida)
- `imdb_id` (formato `tt` + dígitos)
- `poster_path` (formato `/` + ruta)
- `tagline` (texto opcional)

**Campos en Formato JSON (9):**
- Estos campos NO se incluyen en el análisis de texto básico
- Requieren procesamiento especial con librerías como Circe
- Campos: `belongs_to_collection`, `genres`, `production_companies`, `production_countries`, `spoken_languages`, `keywords`, `cast`, `crew`, `ratings`

---

# 2. LECTURA DE COLUMNAS NUMÉRICAS

## Implementación en Scala

Se creó un `case class` específico para extraer únicamente las columnas numéricas del dataset:
```scala
case class MovieNumericos(
  id: Double,
  budget: Double,
  popularity: Double,
  revenue: Double,
  runtime: Double,
  vote_average: Double,
  vote_count: Double
)
```

---

## Proceso de Lectura

1. **Carga del archivo CSV** con separador `;`
2. **Decodificación** usando `fs2.data.csv` con headers automáticos
3. **Filtrado de errores** manteniendo solo registros válidos
4. **Extracción** de columnas numéricas en listas separadas

---

## Resultados de la Lectura
```
======================================================================
 5.2 LECTURA DE COLUMNAS NUMERICAS
======================================================================
Total peliculas validas: 301

Muestra de IDs (primeros 10): 
100.0, 100010.0, 100017.0, 100032.0, 100042.0, 100046.0, 100085.0, 
100089.0, 100167.0, 100183.0

Muestra de Budgets (primeros 10): 
1350000, 0, 0, 0, 40000000, 0, 0, 0, 0, 3000000

Muestra de Popularities (primeros 10): 
4.61, 0.77, 2.96, 1.32, 15.40, 3.40, 0.04, 2.55, 0.57, 2.44

Muestra de Revenues (primeros 10): 
3897569, 0, 0, 0, 169837010, 0, 0, 0, 0, 0

Muestra de Runtimes (primeros 10): 
105, 116, 87, 180, 110, 94, 85, 90, 102, 101

Muestra de Vote Averages (primeros 10): 
7.5, 6.0, 4.8, 6.8, 5.4, 6.1, 6.0, 3.2, 7.6, 4.5

Muestra de Vote Counts (primeros 10): 
1671, 1, 7, 5, 1140, 54, 1, 38, 5, 32
======================================================================
```

---

## Columnas Numéricas Extraídas

| Columna | Descripción | Unidad | Rango Observado |
|---------|-------------|--------|-----------------|
| **id** | Identificador único | N/A | 100.0 - 100183.0 (muestra) |
| **budget** | Presupuesto de producción | USD | 0 - 40,000,000 (muestra) |
| **popularity** | Índice de popularidad TMDB | Score | 0.04 - 15.40 (muestra) |
| **revenue** | Recaudación total | USD | 0 - 169,837,010 (muestra) |
| **runtime** | Duración de la película | Minutos | 85 - 180 (muestra) |
| **vote_average** | Calificación promedio | Score | 3.2 - 7.6 (muestra) |
| **vote_count** | Total de votos | Cantidad | 1 - 1671 (muestra) |

---

## Observaciones de la Muestra

- **IDs**: Valores únicos y consecutivos del dataset TMDB
- **Budget**: Gran variación, con muchos valores en 0 (sin datos o películas independientes)
- **Popularity**: Valores bajos en general, con picos en películas destacadas (15.40)
- **Revenue**: Similar al presupuesto, muchos valores en 0
- **Runtime**: Duración típica de películas entre 85-180 minutos
- **Vote Average**: Calificaciones variadas entre 3.2 y 7.6 en la muestra
- **Vote Count**: Desde 1 voto hasta 1671, indicando diferentes niveles de engagement

---

# 3. ANÁLISIS DE DATOS EN COLUMNAS NUMÉRICAS

## Estadísticas Básicas Calculadas

Se implementó una función de estadísticas que calcula para cada columna numérica:
```scala
def stats(values: List[Double]): (Double, Double, Double) =
  if values.isEmpty then (0.0, 0.0, 0.0)
  else (values.sum / values.length, values.min, values.max)
```

---

## Resultados del Análisis
```
======================================================================
 5.3 ANALISIS DE DATOS - ESTADISTICAS BASICAS
======================================================================
Columna              | Promedio        | Minimo          | Maximo         
----------------------------------------------------------------------
BUDGET               | 2,068,405.32    | 0.00            | 130,000,000.00   
POPULARITY           | 1.97            | 0.00            | 26.88          
REVENUE              | 10,107,551.19   | 0.00            | 847,423,452.00   
RUNTIME              | 93.18           | 0.00            | 360.00         
VOTE_AVERAGE         | 5.46            | 0.00            | 9.50           
VOTE_COUNT           | 171.99          | 0.00            | 6,768.00        
======================================================================
```

---

## Interpretación de las Estadísticas

### BUDGET (Presupuesto)
- **Promedio**: $2,068,405.32 USD
- **Mínimo**: $0 (películas sin datos o independientes)
- **Máximo**: $130,000,000 USD (superproducción)
- **Análisis**: 
  - Presupuesto promedio relativamente bajo sugiere presencia significativa de películas independientes
  - Gran variabilidad entre producciones (rango de $130M)
  - Muchas películas sin datos de presupuesto reportados (valor 0)

---

### POPULARITY (Popularidad)
- **Promedio**: 1.97
- **Mínimo**: 0.00
- **Máximo**: 26.88
- **Análisis**: 
  - Popularidad promedio baja indica dataset con películas de nicho o menos conocidas
  - Valor máximo de 26.88 representa películas con alto engagement
  - Métrica dinámica que cambia con el tiempo según actividad de usuarios

---

### REVENUE (Recaudación)
- **Promedio**: $10,107,551.19 USD
- **Mínimo**: $0 (sin datos de taquilla)
- **Máximo**: $847,423,452 USD (blockbuster)
- **Análisis**: 
  - Recaudación promedio superior al presupuesto promedio (ROI positivo)
  - Película más exitosa recaudó más de $847M
  - Gran disparidad entre éxitos comerciales y películas sin recaudación reportada
  - Ratio Revenue/Budget promedio: ~4.9x (rentabilidad significativa)

---

### RUNTIME (Duración)
- **Promedio**: 93.18 minutos
- **Mínimo**: 0 minutos (datos inválidos o cortos especiales)
- **Máximo**: 360 minutos (6 horas - película épica o miniserie)
- **Análisis**: 
  - Duración promedio cercana a 90 minutos (estándar de la industria)
  - Presencia de valores extremos que requieren limpieza
  - Rango válido establecido en limpieza: 1-300 minutos

---

### VOTE_AVERAGE (Calificación Promedio)
- **Promedio**: 5.46 / 10
- **Mínimo**: 0.0 (películas sin votos o muy mal evaluadas)
- **Máximo**: 9.5 / 10 (obra maestra)
- **Análisis**: 
  - Calificación promedio ligeramente por debajo del punto medio (5.5)
  - Indica una distribución relativamente normal de calidad
  - Presencia de películas tanto mal evaluadas como aclamadas

---

### VOTE_COUNT (Número de Votos)
- **Promedio**: 171.99 votos
- **Mínimo**: 0 votos (películas nuevas o muy oscuras)
- **Máximo**: 6,768 votos (película muy popular)
- **Análisis**: 
  - Promedio bajo sugiere muchas películas con poco engagement
  - Gran dispersión indica mix de películas populares y desconocidas
  - Películas con más votos tienen calificaciones más confiables estadísticamente

---

## Relaciones Observadas

### Budget vs Revenue
- **Ratio promedio**: Revenue es ~4.9x mayor que Budget
- **Indicador**: Rentabilidad general positiva del conjunto de películas

### Popularity vs Vote Count
- Correlación esperada: Mayor popularidad generalmente implica más votos
- Película más votada (6,768 votos) probablemente tiene alta popularidad

### Vote Average vs Vote Count
- Confiabilidad: Calificaciones con más votos son estadísticamente más robustas
- Películas con pocos votos pueden tener calificaciones menos representativas

---

# 4. ANÁLISIS DE DATOS EN COLUMNAS TIPO TEXTO

## Implementación

Se creó un `case class` para las columnas de texto principales (**excluyendo campos JSON**):
```scala
case class MovieText(
  original_title: String,
  original_language: String,
  belongs_to_collection: String,
  tagline: String,
  status: String
)
```

---

## Función de Análisis de Frecuencia
```scala
def frecuencia(values: List[String]): Map[String, Int] =
  values.groupBy(identity).view.mapValues(_.size).toMap
```

---

## Resultados del Análisis
```
======================================================================
 5.4 ANALISIS DE DATOS EN COLUMNAS TIPO TEXTO
======================================================================
Total de peliculas validas: 301

--- IDIOMAS (Top 10) ---
en -> 199
it -> 20
fr -> 20
ja -> 13
es -> 7
zh -> 6
de -> 5
da -> 5
ru -> 3
hi -> 3

--- COLECCIONES (Top 10) ---
                                                   -> 281
{'id': 399, 'name': 'Predator Collection', 'poster -> 1
{'id': 104774, 'name': 'Tomtar och Trolltyg Collec -> 1
{'id': 463068, 'name': 'Täällä Pohjantähden alla ( -> 1
{'id': 264, 'name': 'Back to the Future Collection -> 1
{'id': 105075, 'name': 'The Shadow Collection', 'p -> 1
{'id': 422834, 'name': 'Ant-Man Collection', 'post -> 1
{'id': 460292, 'name': 'The Lone Ranger (Clayton M -> 1
{'id': 415931, 'name': 'The Bowery Boys', 'poster_ -> 1
{'id': 418219, 'name': 'Carmina - Colección', 'pos -> 1

--- STATUS DE PELICULAS ---
Post Pro -> 1
         -> 2
Rumored  -> 2
Released -> 296

--- TAGLINE ---
Peliculas con tagline: 301
Peliculas sin tagline: 0

--- TITULOS ---
Total de titulos unicos: 293

Muestra de titulos (20):
- Lock, Stock and Two Smoking Barrels                                     
- Flight Command                                                          
- Verfolgt                                                                
- The Great Los Angeles Earthquake                                        
- Dumb and Dumber To                                                      
- The Giant Mechanical Man                                                
- El tren de la memoria                                                   
- Nazis at the Center of the Earth                                        
- No Way Home                                                             
- Outpost: Black Sun                                                      
- Return Home                                                             
- Julian Po                                                               
- Baby Snakes                                                             
- Gertie the Dinosaur                                                     
- Quartet                                                                 
- Lobos de Arga                                                           
- Harold's Going Stiff                                                    
- Eddie: The Sleepwalking Cannibal                                        
- Min Avatar og mig                                                       
- Trapped in the Closet: Chapters 1-12                                    
======================================================================
```

---

## Análisis Detallado por Campo

### ORIGINAL_LANGUAGE (Idioma Original)

#### Top 10 Idiomas Más Frecuentes

| Código ISO | Idioma | Cantidad | Porcentaje |
|------------|--------|----------|------------|
| **en** | Inglés | 199 | 66.11% |
| **it** | Italiano | 20 | 6.64% |
| **fr** | Francés | 20 | 6.64% |
| **ja** | Japonés | 13 | 4.32% |
| **es** | Español | 7 | 2.33% |
| **zh** | Chino | 6 | 1.99% |
| **de** | Alemán | 5 | 1.66% |
| **da** | Danés | 5 | 1.66% |
| **ru** | Ruso | 3 | 1.00% |
| **hi** | Hindi | 3 | 1.00% |

#### Interpretación
- **Dominancia del inglés**: 66% de las películas son en inglés, reflejando la influencia de Hollywood
- **Diversidad europea**: Italiano y francés empatan en segundo lugar (20 películas cada uno)
- **Presencia asiática**: Japonés (13), chino (6) e hindi (3) representan el cine asiático
- **Mercados nórdicos y europeos**: Danés, alemán y ruso completan el top 10
- **Total de idiomas**: Al menos 10 idiomas diferentes en 301 películas

---

### BELONGS_TO_COLLECTION (Colección/Saga)

#### Distribución

| Tipo | Cantidad | Porcentaje |
|------|----------|------------|
| **Sin colección** (vacío) | 281 | 93.35% |
| **Con colección** | 20 | 6.65% |

#### Colecciones Identificadas (muestra)
1. Predator Collection
2. Tomtar och Trolltyg Collection
3. Täällä Pohjantähden alla Collection
4. Back to the Future Collection
5. The Shadow Collection
6. Ant-Man Collection
7. The Lone Ranger (Clayton Moore) Collection
8. The Bowery Boys
9. Carmina - Colección
10. *(Y más...)*

#### Interpretación
- **Mayoría de películas independientes**: 93% no pertenecen a ninguna saga
- **Presencia de franquicias conocidas**: Back to the Future, Predator, Ant-Man
- **Diversidad internacional**: Colecciones en diferentes idiomas (español, finlandés, sueco)
- **Dato en formato JSON**: Requiere parsing especializado para análisis profundo

---

### STATUS (Estado de Producción)

#### Distribución Completa

| Estado | Cantidad | Porcentaje |
|--------|----------|------------|
| **Released** | 296 | 98.34% |
| **Rumored** | 2 | 0.66% |
| **(vacío)** | 2 | 0.66% |
| **Post Pro** | 1 | 0.33% |

#### Interpretación
- **Dataset principalmente de películas estrenadas**: 98% con status "Released"
- **Películas en desarrollo**: 2 en rumores, 1 en post-producción
- **Datos incompletos**: 2 registros sin status (requieren limpieza)
- **Dataset actualizado**: Predominio de películas con ciclo de producción completo

---

### TAGLINE (Eslogan Promocional)

#### Estadísticas

| Métrica | Valor |
|---------|-------|
| **Películas CON tagline** | 301 |
| **Películas SIN tagline** | 0 |
| **Cobertura** | 100% |

#### Interpretación
- **Cobertura completa**: Todas las películas en este subset tienen tagline
- **Campo importante para marketing**: Indica dataset con información promocional completa
- **Contrasta con dataset completo**: Más adelante veremos que el dataset completo tiene 1,872 películas sin tagline

---

### ORIGINAL_TITLE (Título Original)

#### Estadísticas

| Métrica | Valor |
|---------|-------|
| **Total de películas** | 301 |
| **Títulos únicos** | 293 |
| **Títulos duplicados** | 8 |
| **Tasa de unicidad** | 97.34% |

#### Muestra de Títulos (Primeros 20)

1. Lock, Stock and Two Smoking Barrels
2. Flight Command
3. Verfolgt
4. The Great Los Angeles Earthquake
5. Dumb and Dumber To
6. The Giant Mechanical Man
7. El tren de la memoria
8. Nazis at the Center of the Earth
9. No Way Home
10. Outpost: Black Sun
11. Return Home
12. Julian Po
13. Baby Snakes
14. Gertie the Dinosaur
15. Quartet
16. Lobos de Arga
17. Harold's Going Stiff
18. Eddie: The Sleepwalking Cannibal
19. Min Avatar og mig
20. Trapped in the Closet: Chapters 1-12

#### Interpretación
- **Alta diversidad**: 97% de títulos únicos
- **8 títulos repetidos**: Posibles remakes, reediciones o errores de datos
- **Diversidad de géneros**: Desde comedias británicas hasta terror y ciencia ficción
- **Títulos multiidioma**: Español (El tren de la memoria, Lobos de Arga), alemán (Verfolgt), danés (Min Avatar og mig)

---

## Exclusión de Campos JSON

**Campos NO incluidos en este análisis de texto**:
- `belongs_to_collection` (análisis superficial únicamente - formato JSON)
- `genres`
- `production_companies`
- `production_countries`
- `spoken_languages`
- `keywords`
- `cast`
- `crew`
- `ratings`

**Razón**: Estos campos requieren parsing JSON especializado con librerías como **Circe** para un análisis estructurado adecuado.

---

# 5. LIMPIEZA DE DATOS

## Estrategia de Limpieza Implementada

Se desarrolló un proceso de limpieza en **2 fases** con validación inteligente:

1. **Fase 1**: Filtros de validez básica
2. **Fase 2**: Detección y manejo de outliers (método IQR)

---

## FASE 1: VALIDACIÓN BÁSICA

### Funciones de Validación Implementadas
```scala
// Validación de strings obligatorios
def isValidString(s: String): Boolean =
  s != null && s.trim.nonEmpty && !s.equalsIgnoreCase("null")

// Validación de URLs opcionales
def isValidOptionalUrl(s: String): Boolean =
  s == null || s.trim.isEmpty || s.equalsIgnoreCase("null") || s.startsWith("http")

// Validación de IMDB IDs opcionales
def isValidOptionalImdbId(s: String): Boolean =
  s == null || s.trim.isEmpty || s.equalsIgnoreCase("null") || s.startsWith("tt")

// Validación de Poster Paths opcionales
def isValidOptionalPosterPath(s: String): Boolean =
  s == null || s.trim.isEmpty || s.equalsIgnoreCase("null") || s.startsWith("/")

// Validación de booleanos
def isValidBoolean(s: String): Boolean =
  val normalized = s.trim.toLowerCase
  normalized == "true" || normalized == "false" || normalized == "1" || normalized == "0"
```

---

### Criterios de Validación por Tipo de Campo

#### CAMPOS NUMÉRICOS OBLIGATORIOS

| Campo | Criterio de Validación | Justificación |
|-------|------------------------|---------------|
| **id** | > 0 | Identificador único positivo |
| **budget** | >= 0 | Permite películas sin presupuesto reportado |
| **revenue** | >= 0 | Permite películas sin ingresos reportados |
| **runtime** | > 0 y <= 300 | Duración realista de películas (5h máximo) |
| **popularity** | >= 0 | Métrica no negativa |
| **vote_average** | >= 0 y <= 10 | Escala de calificación TMDB |
| **vote_count** | >= 0 | Conteo no negativo de votos |

---

#### CAMPOS DE TEXTO OBLIGATORIOS

| Campo | Criterio de Validación | Justificación |
|-------|------------------------|---------------|
| **title** | No vacío, != "null" | Crítico para identificación |
| **original_title** | No vacío, != "null" | Crítico para identificación |
| **overview** | No vacío, != "null" | **Crítico para análisis de contenido** |
| **release_date** | Formato `YYYY-MM-DD` | Fecha válida y parseable |
| **status** | No vacío | Estado de producción requerido |
| **original_language** | 2-3 caracteres | Código ISO válido |

---

#### CAMPOS BOOLEANOS

| Campo | Criterio de Validación |
|-------|------------------------|
| **adult** | `true`, `false`, `1`, `0` |
| **video** | `true`, `false`, `1`, `0` |

---

#### CAMPOS OPCIONALES CON VALIDACIÓN DE FORMATO

| Campo | Formato Requerido | Ejemplo Válido |
|-------|-------------------|----------------|
| **homepage** | Inicia con `http` | `https://www.example.com` |
| **imdb_id** | Inicia con `tt` | `tt1234567` |
| **poster_path** | Inicia con `/` | `/abc123.jpg` |
| **tagline** | Texto libre o vacío | `"The adventure begins"` |

---

## FASE 2: DETECCIÓN Y MANEJO DE OUTLIERS (MÉTODO IQR)

### Cálculo del Rango Intercuartílico (IQR)
```scala
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
```

---

---

### Aplicación Condicional del IQR

**Condición**: Solo se aplica IQR si hay más de 50 registros válidos

**Variables analizadas**:
- `budget` (solo valores > 0)
- `revenue` (solo valores > 0)

**Criterio de aceptación**:
- Se permite **máximo 1 outlier por registro** (budget O revenue pueden ser outliers, pero no ambos)

**Justificación**:
- En la industria cinematográfica, los outliers son comunes y valiosos (blockbusters)
- Budget y Revenue de 0 son válidos (películas independientes o datos no reportados)

---

## REPORTE COMPLETO DE LIMPIEZA
```
====================================================================================================
                    REPORTE DE LIMPIEZA - DATASET DE PELICULAS (28 COLUMNAS)
====================================================================================================
  Total registros originales:  3,487
  Total registros limpios:     3,288
  Registros eliminados:          199
  Porcentaje retenido:         94.29%
```

---

### 1. ESTADÍSTICAS COMPARATIVAS (ANTES → DESPUÉS)

#### [BUDGET]

| Métrica | ANTES | DESPUÉS | Cambio |
|---------|-------|---------|--------|
| Media | $29.99M | $23.63M | -21.21% |
| Mediana | $13.00M | $12.00M | -7.69% |
| Mínimo | $1.00 | $1.00 | 0% |
| Máximo | $380.00M | $190.00M | -50.00% |

**Análisis**:
- Reducción significativa del presupuesto máximo ($380M → $190M)
- Eliminación de outliers extremos en presupuestos
- Media y mediana se acercan más, indicando distribución más normal
- Se eliminaron producciones con presupuestos excesivamente altos

---

#### [REVENUE]

| Métrica | ANTES | DESPUÉS | Cambio |
|---------|-------|---------|--------|
| Media | $99.34M | $72.15M | -27.37% |
| Mediana | $33.70M | $28.20M | -16.32% |
| Mínimo | $1.00 | $1.00 | 0% |
| Máximo | $1,118.89M | $1,118.89M | 0% |

**Análisis**:
- Recaudación máxima se mantiene ($1,118M - probablemente película legítimamente exitosa)
- Reducción en media y mediana por eliminación de datos anómalos
- Distribución más realista post-limpieza

---

#### [RUNTIME]

| Métrica | ANTES | DESPUÉS | Cambio |
|---------|-------|---------|--------|
| Media | 98.54 min | 96.98 min | -1.58% |
| Mediana | 96.00 min | 96.00 min | 0% |

**Análisis**:
- Cambio mínimo en estadísticas centrales
- Indica que la mayoría de los datos de duración eran válidos
- Limpieza efectiva de valores extremos sin afectar distribución general

---

#### [VOTE AVERAGE]

| Métrica | ANTES | DESPUÉS | Cambio |
|---------|-------|---------|--------|
| Media | 5.67 | 5.72 | +0.88% |
| Mediana | 6.10 | 6.10 | 0% |

**Análisis**:
- Ligero aumento en calificación promedio
- Sugiere que se eliminaron películas con calificaciones muy bajas o sin votos
- Mediana se mantiene, indicando distribución central estable

---

### 2. VALORES NULOS Y VACÍOS (Datos Originales)

| Campo | Cantidad de Nulos | Porcentaje |
|-------|-------------------|------------|
| Homepage | 2,935 | 84.17% |
| Tagline | 1,872 | 53.67% |
| Overview | 75 | 2.15% |
| Poster Path | 15 | 0.43% |
| Release Date | 6 | 0.17% |
| Status | 5 | 0.14% |
| IMDB ID | 1 | 0.03% |
| Original Language | 1 | 0.03% |
| Title | 0 | 0.00% |
| Original Title | 0 | 0.00% |

#### Análisis de Nulos

**Campos con Alto Porcentaje de Nulos (Opcionales)**:
- **Homepage (84%)**: La mayoría de películas no tienen sitio web oficial reportado
- **Tagline (54%)**: Más de la mitad sin eslogan promocional

**Campos Críticos con Nulos**:
- **Overview (75)**: 2.15% sin sinopsis - estos registros fueron eliminados por ser campo crítico
- **Release Date (6)**: 0.17% sin fecha - eliminados por ser campo obligatorio
- **Status (5)**: 0.14% sin estado - eliminados

**Campos con Integridad Alta**:
- **Title y Original Title**: 100% de cobertura

---

### 3. VALORES INVÁLIDOS EN CAMPOS NUMÉRICOS

| Validación | Cantidad | Porcentaje | Acción Tomada |
|------------|----------|------------|---------------|
| Runtime (<= 0 o > 300) | 123 | 3.53% | ELIMINADOS |
| ID (<= 0) | 0 | 0.00% | N/A |
| Budget (< 0) | 0 | 0.00% | N/A |
| Revenue (< 0) | 0 | 0.00% | N/A |
| Popularity (< 0) | 0 | 0.00% | N/A |
| Vote Count (< 0) | 0 | 0.00% | N/A |

#### Análisis

**Runtime (Duración)**:
- **123 registros inválidos (3.53%)**:
  - Valores <= 0 minutos (datos faltantes o errores)
  - Valores > 300 minutos (poco realistas para películas comerciales)
- **Acción**: Eliminados todos los registros fuera del rango 1-300 minutos

**Otros Campos Numéricos**:
- Excelente calidad: 0 valores negativos en `id`, `budget`, `revenue`, `popularity`, `vote_count`
- Indica buena calidad del dataset original de TMDB

---

### 4. VALORES INVÁLIDOS EN CAMPOS BOOLEANOS

| Campo | Cantidad Inválida | Porcentaje | Estado |
|-------|-------------------|------------|--------|
| Adult | 0 | 0.00% |  Perfecto |
| Video | 0 | 0.00% |  Perfecto |

#### Análisis

- 100% de validez en campos booleanos
- Todos los valores en formato correcto: `true`, `false`, `1`, o `0`
- No se requirió limpieza en estos campos

---

### 5. FORMATOS INVÁLIDOS EN CAMPOS OPCIONALES

| Campo | Validación | Cantidad Inválida | Porcentaje | Acción |
|-------|------------|-------------------|------------|--------|
| Release Date | Formato YYYY-MM-DD | 6 | 0.17% | ELIMINADOS |
| Homepage | Formato URL | 0 | 0.00% | N/A |
| IMDB ID | Formato tt* | 0 | 0.00% | N/A |
| Poster Path | Formato /* | 0 | 0.00% | N/A |
| Tagline | Formato válido | 0 | 0.00% | N/A |

#### Análisis

**Release Date (Fecha de Estreno)**:
- 6 registros con formato inválido (0.17%)
- Ejemplos de formatos incorrectos: fechas parciales, formatos no estándar
- **Acción**: Eliminados por ser campo obligatorio para análisis temporal

**Campos Opcionales con Validación de Formato**:
- Excelente calidad: Homepage, IMDB ID, Poster Path y Tagline sin errores de formato
- Cuando están presentes, siguen los estándares esperados

---

## RESUMEN DE LA LIMPIEZA
```
====================================================================================================
  [OK] Limpieza completada exitosamente
  [OK] Dataset listo para análisis
  [VALIDADO] 15 campos obligatorios + 4 campos opcionales con formato
  [MEJORAS] Budget/Revenue permiten 0, Runtime 0-300, IQR condicional (>50 registros)
  [NOTA] Los campos JSON serán limpiados con Circe posteriormente
====================================================================================================
```

---

### DESGLOSE DE LOS 199 REGISTROS ELIMINADOS

#### Causas de Eliminación (pueden haber múltiples causas por registro)

| Causa | Registros Afectados (aprox.) |
|-------|------------------------------|
| Runtime inválido (<= 0 o > 300) | 123 |
| Overview vacío | 75 |
| Release_date inválida | 6 |
| Status vacío | 5 |
| Original_language inválido | 1 |
| IMDB_ID inválido | 1 |
| Otros criterios | Variable |

> **Nota**: Un mismo registro puede haber sido eliminado por múltiples causas.

---

## CRITERIOS DE CALIDAD FINALES

### Campos Garantizados Post-Limpieza

#### Numéricos:
-  Todos los valores están en rangos válidos
-  No hay valores negativos donde no corresponde
-  Outliers controlados mediante IQR condicional
-  Runtime entre 1-300 minutos (rango realista)

#### Texto:
-  No hay strings vacíos en campos obligatorios
-  Formatos de fecha validados (YYYY-MM-DD)
-  Códigos ISO válidos para idiomas (2-3 caracteres)
-  Overview presente en todos los registros

#### Opcionales:
-  URLs válidas o vacías (formato `http`)
-  IDs de IMDB con formato correcto o vacías (formato `tt*`)
-  Poster paths con formato correcto o vacías (formato `/*`)
-  Taglines válidas o vacías

#### Calidad General:
-  94.29% de retención de datos originales
-  Cada registro tiene información mínima completa y válida
-  Dataset listo para análisis estadístico
-  Dataset listo para visualizaciones
-  Dataset listo para modelado predictivo
-  Distribuciones más normales y realistas post-limpieza

---

## IMPACTO DE LA LIMPIEZA EN ESTADÍSTICAS

### Mejoras Observadas

#### 1. Distribución de Budget más realista
- Eliminación de outlier extremo ($380M → $190M máximo)
- Media más representativa del dataset

#### 2. Revenue más coherente
- Reducción de media por eliminación de anomalías
- Máximo conservado (película legítima de $1,118M)

#### 3. Runtime normalizado
- Todos los valores entre 1-300 minutos
- Eliminados 123 registros con duraciones imposibles

#### 4. Vote Average ligeramente mejorado
- Aumento de 5.67 a 5.72
- Eliminación de películas sin engagement real

#### 5. Integridad de datos críticos
- 100% de registros con `title`, `original_title`, `overview`
- 100% con fechas válidas en formato correcto

---

## PRÓXIMOS PASOS

-  Limpieza básica completada
-  **Pendiente**: Limpieza de campos JSON con Circe
-  **Pendiente**: Visualizaciones de distribuciones y correlaciones

---

## VALIDACIÓN FINAL

**Dataset limpio**: `pi-movies-complete-2026-01-14.csv`

### Características
- **Total de registros**: 3,288 películas
- **Total de columnas**: 28 campos
- **Calidad de datos**: Alta
- **Listo para**: Análisis, visualización y modelado

---


