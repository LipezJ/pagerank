# PageRank Web API

Este proyecto expone una API REST sencilla para operar el grafo social y ejecutar PageRank. A continuación se listan los endpoints disponibles y ejemplos de uso rápido (todas las respuestas/solicitudes usan JSON).

## `/api/search`

- `GET /api/search?q={texto}&k={limite}`  
  Devuelve la lista de personas ordenadas por puntaje de PageRank. Si `q` es vacío, se entrega el top-K global (por defecto `K_TOP`, configurable vía `.env`).  
  **Respuesta**: arreglo de objetos con `name`, `score` y `explanation`.

## `/api/persons`

- `POST /api/persons`  
  Crea o actualiza una persona en la base. Cada alta gatilla una actualización incremental del PageRank.  
  **Body**:
  ```json
  {
    "name": "Ana Isabel Lopez",
    "spamScore": 0.12
  }
  ```
  **Respuesta**:  
  ```json
  {
    "id": 1,
    "name": "Ana Isabel Lopez",
    "spamScore": 0.12,
    "lastSeen": "2025-11-15T19:30:12.123Z"
  }
  ```

## `/api/follows`

- `POST /api/follows`  
  Registra o actualiza una relación dirigida `source -> target`. Filtra automáticamente relaciones con `quality` por debajo del umbral configurado (`QUALITY_THRESHOLD`) y penaliza las cuentas con `spam_score` alto.  
  **Body**:
  ```json
  {
    "sourceId": 1,
    "targetId": 4,
    "quality": 0.8
  }
  ```
  **Respuesta**: `200 OK` con el follow creado (campos `id`, `sourceId`, `targetId`, `quality`, `lastSeen`) o `202 Accepted` si la observación fue descartada (p. ej. calidad baja o ventana de recolección activa).

## `/api/pagerank`

- `POST /api/pagerank/batch`  
  Fuerza una corrida completa de PageRank (iteración de potencias) usando los parámetros `DAMPING`, `EPSILON` y `MAX_ITERS`. Útil si se quiere recalcular todo sin esperar al bootstrap.

- `POST /api/pagerank/incremental`  
  Ejecuta una actualización incremental sobre el subgrafo afectado (máx. ~10 iteraciones). Opcionalmente recibe un listado de `personIds` para restringir los nodos a recalcular:
  ```json
  { "personIds": [1, 4, 7] }
  ```
  Si no se proveen IDs, recalcula en base a los nodos recientemente modificados por otros servicios.

Cada llamada a `/api/pagerank/*` retorna un objeto `PageRankResult` con `iterations`, `averageDelta`, `nodeCount`, `converged` y `elapsed` (duración).

---

**Tips**

- Los parámetros (`K_TOP`, `QUALITY_THRESHOLD`, `DAMPING`, etc.) se configuran en `settings/.env`.
- El dataset inicial puede generarse con `python scripts/generate_data.py` y se importa automáticamente al arrancar si la base está vacía.
