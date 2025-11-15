# Plan de trabajo: App PageRank para búsqueda de personas

## 1) Alcance y supuestos

En este proyecto haremos una aplicación web full-stack con **Spring** que calcula PageRank sobre una mini red social dirigida (persona A → sigue a → persona B) y lo usa para ordenar resultados al buscar personas. Vamos a parametrizar tres valores: **X** (número de personas), **Y** (número de relaciones) y **Z** (tiempo máximo para completar una actualización del ranking). Para mantenerlo manejable en segundo año, pensamos inicialmente en un tamaño de ejemplo como **X ≈ 1,000** y **Y ≈ 5,000**, y un objetivo de **Z ≤ 2–5 segundos** por actualización en máquina local; si el curso o el profe piden otros valores, solo ajustamos parámetros y límites sin cambiar el diseño.

## 2) Entregables mínimos

Entregaremos una **app Spring** con: un modelo de datos para Personas y Relaciones, un proceso que calcule PageRank con factor de amortiguación configurable, manejo de nodos sin salidas, criterio de convergencia y **actualización incremental** cuando cambie el grafo. Expondremos un endpoint de búsqueda que devuelva el **top-K** y una explicación corta de por qué cada persona rankea alto (por ejemplo: “alto PageRank + enlaces entrantes desde usuarios con buen puntaje”). Sin dashboards, sin features extra.

## 3) Etapa 1: Preparación del proyecto

Primero creamos el repo y el **esqueleto de Spring**. Configuramos una paquete `network` para los modelos y una paquete `search` para la vista/endpoint de búsqueda. Dejamos en `settings` un archivo `.env` con los parámetros `DAMPING`, `EPSILON`, `MAX_ITERS`, `K_TOP`, ventana de recolección y `Z`. Vamos a usar **SQLite** al inicio por sencillez; si notamos lentitud con los tamaños elegidos, pasamos a **PostgreSQL** sin cambiar el código de negocio.

## 4) Etapa 2: Modelo de datos

Creamos el modelo `Person(id, name, spam_score)` y el modelo `Follow(src -> dst, quality)`. El grafo es dirigido y simple; evitamos relaciones duplicadas con una **restricción de unicidad `(src, dst)`**. Guardamos un campo `last_seen` para aplicar la “ventana de recolección” y no consultar la misma entidad más de una vez por ventana. Agregamos una tabla `Rank(person, score, updated_at)` para almacenar el PageRank vigente y una tabla `RankDelta(person, delta)` para apoyar la actualización incremental.

## 5) Etapa 3: Ingesta y restricciones operativas

Cargamos entidades y relaciones desde los identificadores/fuentes provistos por el profe (o un dataset de ejemplo). Implementamos un pequeño servicio de recolección que, al recibir nuevas personas o follows, respete la regla de no consultar la misma entidad más de 1 vez por ventana. Para filtrar spam y relaciones de baja calidad, aplicamos dos filtros simples: **ignorar follows** con `quality` por debajo de un umbral y **bajar el peso** de cuentas con `spam_score` alto al normalizar sus salidas. Mantendremos esta lógica **sencilla y explicable**.

## 6) Etapa 4: PageRank base (batch)

Implementamos PageRank en un servicio `pagerank` usando **iteración de potencias**. Elegimos un **factor de amortiguación** por defecto de `0.85` y lo dejamos configurable. Para **nodos sin salidas** (dangling), redistribuimos su masa uniformemente en cada iteración. Definimos un **criterio de convergencia**: detener cuando la norma L1 del cambio medio por nodo baje de `EPSILON` (por ejemplo `1e-6`) o al llegar a `MAX_ITERS` (por ejemplo `50`). Este cálculo se corre completo la primera vez para inicializar `Rank`.

## 7) Etapa 5: Actualización incremental

Cuando cambie el grafo (aparece/desaparece un follow o una persona), haremos **actualización incremental** para no recalcular desde cero. La idea será **marcar solo los nodos afectados** y correr un número acotado de iteraciones locales: recalculamos la contribución saliente de los nodos tocados y propagamos el cambio en 1–2 “capas” de vecinos, luego una o dos pasadas globales de ajuste fino. Si hay una falla o un **lote de cambios muy grande**, aceptamos que el costo pueda **duplicarse** como dice la restricción; en ese caso, forzamos un batch completo pero siempre respetando `Z` con un **límite de iteraciones** y dejando el resto para la próxima ventana.

## 8) Etapa 6: API y lógica de búsqueda

Creamos un endpoint `GET /search?q=...&k=K` que devuelva las personas cuyo nombre haga **match básico** con `q`, ordenadas por `Rank.score` descendente. Para cada resultado incluimos una **explicación breve** generada así: mostramos su puntaje y citamos 1–3 “mejores aportantes” (usuarios que la siguen y tienen alto rank) con su **contribución estimada**. Si el grafo o el rank están desactualizados al momento de la consulta, disparamos una **actualización incremental non-blocking** y devolvemos lo mejor que tengamos.

## 9) Etapa 7: Interfaz mínima

Implementamos una vista simple con un cuadro de búsqueda y una lista de resultados **top-K**. Cada tarjeta mostrará **nombre, puntaje de PageRank y la explicación de una línea**. No agregaremos dashboards ni visualizaciones adicionales para no salirnos del objetivo.

## 10) Etapa 8: Parámetros, tiempos y Z

Mediremos el **tiempo de una actualización** con un decorador que registre duración. Si la actualización **supera Z** (configurable), **limitamos iteraciones** y diferimos el resto a la próxima ronda. Documentamos en el README los **parámetros recomendados** según X e Y. Si el tamaño sube, aumentamos `EPSILON` o reducimos `MAX_ITERS` para mantener `Z`.

## 11) Etapa 9: Pruebas básicas

Haremos **pruebas unitarias** simples del cálculo de PageRank en grafos pequeños conocidos (por ejemplo, triángulos y cadenas) y una **prueba de integración** que verifique que, al agregar o quitar un follow, el score de los involucrados cambia en la **dirección esperada**. No haremos **pruebas de estrés profesionales**; solo lo suficiente para validar **correctitud y convergencia**. Si nos da el tiempo, hacemos pruebas unitarias con **cargas más grandes**.

## 12) Roles

Repartimos el trabajo para avanzar en paralelo y evitar bloqueos:

* **Modelo & Migraciones:** define `Person`, `Follow`, `Rank` y constraints.
* **Motor PageRank:** implementa batch e incremental y perfila tiempos.
* **API & Búsqueda:** arma el endpoint y la lógica de explicación.
* **Ingesta & Filtros:** prepara carga inicial.
* **UI mínima:** hace la vista de búsqueda y estilo sencillo.

## 13) Cronograma tentativo (2–3 semanas)

* **Semana 1:** esqueleto de Spring, modelos y carga inicial; al cierre, PageRank batch corriendo en datasets chicos.
* **Semana 2:** actualización incremental, endpoint de búsqueda y explicación; al cierre, interfaz mínima.
* **Semana 3 (opcional):** pulir parámetros, asegurar `Z` y escribir el README con instrucciones.

## 14) Criterios de término (definidos y medibles)

Daremos por terminado cuando podamos **cargar X personas y Y relaciones**, **calcular el PageRank inicial**, **aceptar cambios en el grafo**, **actualizar en menos de Z por operación**, y **devolver top-K con explicación breve** en el buscador. También dejaremos un **script de ejemplo** que genera un dataset sintético para que cualquiera del equipo pueda probar en su máquina.

