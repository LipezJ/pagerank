# PageRank App (Spring Boot)

Esta app calcula PageRank sobre una red dirigida y expone una API mínima y dos vistas web.

## Cómo correrlo

Requisitos: JDK 25 y Python 3 (para generar datos opcionales).

1. Generar dataset de prueba (opcional):  
   `./gradlew generateDataset`
2. Ejecutar la app:  
   `./gradlew bootRun`
3. Abrir en el navegador:
   - Vista de búsqueda: `http://localhost:8080/search`
   - Grafo (D3): `http://localhost:8080/graph`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`

## Configuración

Parámetros en `settings/.env` (o variables de entorno):
- PageRank: `DAMPING`, `EPSILON`, `MAX_ITERS`, `Z` (límite ms), `K_TOP`
- Ingesta: `QUALITY_THRESHOLD`, `SPAM_PENALTY`
- Dataset: `DATA_PERSONS_PATH`, `DATA_FOLLOWS_PATH`
- DB: `PAGERANK_DB_PATH` (SQLite por defecto `pagerank.db`)

## Vistas

- **/search**: cuadro de texto y top-K ordenado por PageRank. Muestra score, aportantes principales y métricas de la última corrida (modo, iteraciones, delta, convergencia, duración).
- **/graph**: visualización D3 del grafo. Tamaño de nodos proporcional al score, flechas dirigidas y resaltado de aristas entrantes/salientes al pasar el cursor.

## API breve

- `GET /api/search?q=texto&k=K` → resultados ordenados por score.
- `POST /api/persons` → crea/actualiza persona (`name`, `spamScore`).
- `POST /api/follows` → crea/actualiza follow (`sourceId`, `targetId`, `quality`).
- `POST /api/pagerank/batch` o `/api/pagerank/incremental` → ejecuta PageRank y devuelve métricas.

Detalles interactivos en Swagger UI.
