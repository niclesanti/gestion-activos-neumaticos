# CHECKPOINTS — Evaluación del estado final

> En sistemas multi-agente no se evalúa el camino, se evalúa el destino.
> Estos son los checkpoints objetivos que un juez (humano o IA) puede usar
> para decidir si el proyecto está sano.

## C1 — El arnés está completo

- [ ] Existen los 4 archivos base: `AGENTS.md`, `init.sh`, `feature_list.json`,
      `progress/current.md`.
- [ ] Existen los 4 docs: `docs/harness/architecture.md`, `docs/harness/conventions.md`,
      `docs/harness/specs.md`, `docs/harness/verification.md`.
- [ ] `./init.sh` termina con exit code 0 (replica los jobs `backend` y `frontend`
      de `.github/workflows/ci.yml`).

## C2 — El estado es coherente

- [ ] Como mucho una feature en `in_progress` en `feature_list.json`.
- [ ] Toda feature `done` tiene tests asociados que pasan (backend JUnit; el frontend
      no tiene runner de tests, ver `docs/harness/verification.md`).
- [ ] `progress/current.md` está vacío o describe la sesión activa
      (no contiene basura de sesiones anteriores).

## C3 — El código respeta la arquitectura

- [ ] Backend: el código nuevo vive en el módulo Modulith que corresponde y
      `ModularityTests` (`ApplicationModules.verify()`) pasa.
- [ ] Backend: cada endpoint nuevo tiene `@PreAuthorize`, ruta `/api/v1/...`,
      y su fila en la tabla de `docs/Seguridad/seguridad.md` 2.10.
- [ ] Backend: cada tabla nueva tiene su migración Flyway con RLS, políticas y
      privilegios mínimos (y auditoría si es raíz de agregado).
- [ ] Frontend: `npm run lint` pasa (incluye `boundaries/dependencies` e `import-x/order`);
      archivos en kebab-case, solo exports nombrados, strings en i18n es/en.
- [ ] No hay dependencias nuevas sin justificación en `design.md`.
- [ ] No hay `System.out.println`, `console.log` de debug ni TODOs sin contexto.

## C4 — La verificación es real

- [ ] Todo servicio/controlador/mapper nuevo del backend tiene su test
      (unitario con Mockito, `@WebMvcTest` o integración con Testcontainers).
- [ ] Los tests usan `TestDataFactory` para fixtures y PostgreSQL real
      (Testcontainers), no H2 ni mocks de la base.
- [ ] `./mvnw verify` pasa con cobertura LINE y BRANCH ≥ 85 % (JaCoCo).
- [ ] `npm run lint`, `npm run typecheck`, `npm run build` y
      `npm audit --omit=dev --audit-level=high` pasan.

## C4b — Prueba en el stack real

- [ ] Toda feature `done` con `"sdd": true` tiene `progress/smoke_<name>.md`: `curl` si fue solo
      backend, Claude in Chrome si tocó el frontend.

## C5 — La sesión se cerró bien

- [ ] No hay archivos sin trackear sospechosos (`*.tmp`, `target/`, `dist/`,
      `node_modules/` fuera del `.gitignore`).
- [ ] `progress/history.md` tiene una entrada por la última sesión.
- [ ] La última feature trabajada está reflejada en su estado correcto.
- [ ] `CLAUDE.md` ("Current state") refleja la feature si cambió el estado del proyecto.

## C6 — Spec Driven Development

- [ ] Toda feature con `"sdd": true` en estado `spec_ready`, `in_progress`
      o `done` tiene su carpeta `specs/<name>/` con los 3 archivos:
      `historia-usuario.md`, `design.md`, `tasks.md`.
- [ ] `historia-usuario.md` tiene la historia (Como… quiero… para…) y criterios de aceptación Dado/Cuando/Entonces (ver `docs/harness/specs.md`).
- [ ] Toda feature `done` con `"sdd": true` tiene todas sus tasks marcadas
      `[x]` en `tasks.md`.
- [ ] Cada `CA<n>` de `historia-usuario.md` está cubierto por al menos un test
      concreto (o, solo para criterios de UI sin runner de tests, por la
      verificación documentada en `progress/impl_<name>.md`).

---

**Cómo usar este archivo:** un agente revisor (`.claude/agents/reviewer.md`)
recorre cada checkbox, marca `[x]` o `[ ]`, y rechaza el cierre de sesión
si quedan boxes vacíos en C1-C6.
