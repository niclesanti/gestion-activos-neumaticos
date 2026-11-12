# Flujo SDD — protocolo del leader

> Lo sigue la **sesión principal** (no es un subagente: un subagente no puede lanzar otros).
> Es la única descripción del flujo; los agentes de `.claude/agents/` solo conocen su parte.

```
pending → spec_author → spec_ready → ⏸ HUMANO → in_progress → implementer
        → leader: ./verify.sh (una vez; si falla, vuelve al implementer)
        → leader: navegador (solo si cambió frontend/)
        → reviewer (lee diff + resultados; no corre verify)
        → leader: marca done + entrada en progress/history.md
```

## Arranque

1. Lee `feature_list.json` y toma la primera feature (o en su defecto la indicada por el usuario) no `done` / no `blocked`.
2. **No corras `verify.sh` al arrancar**: se parte de una rama que pasa el CI. Solo se corre al terminar la implementación.
3. No leas `progress/history.md` ni `docs/estado-actual.md` salvo que el contexto lo exija.

## Según el estado de la feature

**`pending`** — lanza 1 `spec_author`. Cuando devuelve `spec_ready -> specs/<name>/`, **paras** y le dices al humano:
"Spec listo en `specs/<name>/`. Revisalo y decime 'aprobado' o pedí cambios."

**`spec_ready` + el humano aprobó** —
1. Pasa la feature a `in_progress` en `feature_list.json`.
2. Lanza 1 `implementer` con la ruta `specs/<name>/`. Devuelve `done -> progress/impl_<name>.md` o `blocked -> ...`.
3. Corre `./verify.sh` **una vez**. Si da rojo, devuelve al implementer el extracto del error (no lo arregles tú) y repite. Anota `verify.sh: [OK]` en `progress/impl_<name>.md`.
4. Si cambió algo en `frontend/gestion-neumaticos/`: levanta el stack (`docker compose up -d --build`) y recorre en el navegador, con Claude in Chrome (skill `claude-in-chrome`), los pasos de `impl_<name>.md`. Anota el resultado en su sección "Prueba en el stack real". Si hay defectos, vuelven al implementer. Si no se puede probar (Docker o extensión no disponibles), díselo al humano.
5. Lanza 1 `reviewer`. Devuelve `APPROVED` o `CHANGES_REQUESTED -> progress/review_<name>.md`; si pide cambios, vuelve al implementer.
6. Con `APPROVED`, **cierra tú** (ver abajo).

**`spec_ready` sin aprobación** — no continúes; recuérdale al humano que debe revisar el spec.

**`in_progress`** — sesión interrumpida: pregunta al humano si reanudas o abortas.

## Cierre (lo hace el leader, sin lanzar al implementer)

1. `status: "done"` en `feature_list.json`.
2. Agrega al final de `progress/history.md` una entrada de 3–5 líneas (fecha, HU, qué cambió, referencias). Es memoria compartida entre sesiones: no la releas salvo que haga falta contexto histórico.
3. Si la feature cambió el estado del proyecto, actualiza `docs/estado-actual.md` (y `docs/Seguridad/seguridad.md` si tocó auth).
4. Deja el repo limpio: sin temporales, `System.out.println`, `console.log` de debug ni TODOs sin contexto.
5. Si algo bloquea: `status: "blocked"` y la razón en `progress/impl_<name>.md` (o `progress/spec_<name>.md`).

## Regla anti-teléfono-descompuesto

Instruye a los subagentes para que escriban sus resultados en archivos y te devuelvan **una sola línea** con la referencia. Rechaza resultados que vengan en chat sin referencia a archivo.

## Escalado

| Complejidad          | Subagentes                                                          |
|----------------------|---------------------------------------------------------------------|
| Trivial (1 archivo)  | spec_author → ⏸ → implementer                                       |
| Media (2-3 archivos) | spec_author → ⏸ → implementer → reviewer                            |
| Compleja (refactor)  | 2-3 Explore → spec_author → ⏸ → implementer → reviewer              |
| Muy compleja         | Dividir en historias y volver a aplicar la tabla                    |

Los topes de tamaño de los specs (ver `specs.md`) escalan con esta complejidad: no llegues al tope si la HU es chica.

## Reglas duras

- Una feature a la vez (WIP = 1).
- No editas `backend/gestion-neumaticos/src/**` ni `frontend/gestion-neumaticos/src/**`.
- No saltas la fase de spec ni la aprobación humana.
- No cierras una feature sin `verify.sh` verde y `APPROVED` del reviewer.
