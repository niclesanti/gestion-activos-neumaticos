# Bitácora histórica (append-only)

> Cada vez que se cierra una sesión, su resumen se añade aquí.
> No edites entradas anteriores. Solo añades al final.

---

## 2026-11-07 — Bootstrap del arnés SDD
- **Agente:** Claude Code (con el humano).
- **Cambios:** `AGENTS.md`, `CHECKPOINTS.md`, `init.sh`, `feature_list.json`, `docs/harness/`, `.claude/agents/`, `.claude/hooks/`, `.claude/settings.local.json`, sección de rol `leader` al inicio de `CLAUDE.md`.
- **Resultado:** arnés adaptado a Spring Boot + React/TS. `init.sh` replica los jobs de `.github/workflows/ci.yml`.

---

## 2026-11-09 — HU-27 Buscar unidades de transporte por patente (feature id 1, `done`)
- **Agentes:** leader, spec_author, implementer, reviewer (spec aprobado por el humano).
- **Cambios:** `GET /api/v1/unidades-transporte?patente=&page=&size=&sort=` (`hasRole('LECTOR')`, `UnidadTransporteListadoDTO` con `ConfiguracionUnidadListadoDTO`, solo activas, `@EntityGraph`, lista blanca de orden `patente`, sin migración). Paginación unificada 10 por defecto / 20 máximo (`PaginacionConfig`, también en los 7 `GET .../auditoria`). Frontend: `TransportUnitsTable` abierta al Lector; `TablePagination` y `SortableTableHead` extraídos a `components/common/` y usados por `features/audit`; `DEFAULT_PAGE_SIZE`. Docs: `CLAUDE.md` y `docs/Seguridad/seguridad.md` (2.10, 3.4, Pendiente).
- **Resultado:** `./init.sh` en `[OK]`; smoke de `curl` y de navegador en verde; reviewer aprobó (observaciones menores aplicadas: constantes de uso único inline, orden de imports).
- **Referencias:** `specs/buscar_unidades_transporte_por_patente/`, `progress/impl_buscar_unidades_transporte_por_patente.md`, `progress/smoke_buscar_unidades_transporte_por_patente.md`, `progress/review_buscar_unidades_transporte_por_patente.md`.
