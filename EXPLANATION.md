# Explicacion

## Servicios principales
- `DatasetBootstrapper`: al iniciar, si no hay personas, lee rutas configuradas de CSV y dispara la ingesta inicial.
- `DatasetIngestionService`: abre CSV de personas y follows, convierte filas en observaciones y las pasa a `IngestionService`; registra cuantos se cargaron y tolera archivos faltantes.
- `GraphService`: CRUD basico del grafo en BD: crea/actualiza personas y follows, actualiza `Rank` y `RankDelta` cuando se piden, y expone lecturas de aristas salientes/entrantes.
- `IngestionService`: reglas de negocio para datos observados: normaliza nombres, filtra follows de baja calidad, penaliza calidad segun spam, respeta ventana de refresco y, al cambiar algo, dispara PageRank incremental.
- `PageRankBootstrapper`: tras cargar datos, si hay personas pero no hay ranks previos, corre un PageRank batch inicial.
- `PageRankService`: arma el grafo en memoria, ejecuta PageRank (batch o incremental), persiste scores y deltas, y guarda la ultima metrica de ejecucion.
- `SearchService`: busca personas ordenadas por score; sin query devuelve el top global. Calcula contribuidores principales segun peso relativo de las aristas que recibe cada nodo.

## Como funciona `PageRankService`
- **Snapshot del grafo**: trae todas las `Person`, arma `indexMap` id->posicion, listas de adyacencia y entrantes, y la suma de pesos salientes. Solo considera aristas `Follow` con `quality` > 0.
- **Batch vs incremental**: `runBatchComputation` recalcula siempre desde vector uniforme. `runIncrementalUpdate` usa scores previos como punto de partida y menos iteraciones; expande los ids tocados con sus vecinos entrantes/salientes para dimensionar el impacto y, si el cambio abarca una gran parte del grafo, fuerza batch.
- **Estado inicial**: `buildInitialScores` toma `Rank` guardados, normaliza a suma 1; si no hay, usa vector uniforme.
- **Iteracion PageRank (compute)**:
  - Parametros: `damping`, `epsilon`, `maxIterations`, `maxUpdateDuration` (corta por tiempo y marca `timeLimited`).
  - Por iteracion: inicia `next` con teletransporte `(1-damping)/N`; acumula `danglingMass` (nodos sin salidas); reparte `damping * score / sumaPesosSalientes` a vecinos ponderado por el peso; reparte `danglingMass` uniforme; calcula `averageDelta` (promedio de |next-current|); copia `next` a `current`; corta si `averageDelta <= epsilon` o se agotan iteraciones/tiempo.
- **Persistencia**: `persistRanks` compara score nuevo vs previo y actualiza/crea `Rank` y `RankDelta` (delta = nuevo - previo) en bloque; actualiza `lastResult`.
- **Optimizacion incremental**: parte del vector previo y encola los nodos tocados (y sus vecinos entrantes/salientes). Recalcula el score local y, si el delta supera `epsilon/4`, lo aplica, ajusta la masa de colgantes si corresponde y propaga encolando esos vecinos; se detiene al vaciar la cola o al alcanzar `maxUpdateDuration`, normaliza los scores y calcula el delta promedio frente al baseline.

## Modelo de datos (SQLite)
- `persons` (`Person`): `id` autoincrement, `name` (indice), `spam_score` (0-1), `last_seen`. Representa nodos.
- `follows` (`Follow`): `id`, `src_id` y `dst_id` a `Person`, `quality` (peso de arista), `last_seen`. Unicidad `(src_id, dst_id)` e indices por fuente y destino.
- `ranks` (`Rank`): PK = `person_id` (1:1 con `Person`), `score` (PageRank normalizado), `updated_at`; indice por score descendente para obtener el top.
- `rank_deltas` (`RankDelta`): PK = `person_id`, `delta` (score nuevo - score previo) para ver subidas/bajadas tras la ultima corrida.
- `PageRankResult`: record en memoria con metricas de la ultima ejecucion (no tabla).
