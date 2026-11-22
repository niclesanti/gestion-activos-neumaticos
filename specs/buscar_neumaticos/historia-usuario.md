# HU-28 - Buscar neumáticos por número de serie o atributos técnicos

**Historia**: Como usuario del sistema (cualquier nivel de acceso), quiero buscar neumáticos por su número de serie o por atributos técnicos secundarios (marca, estado), de forma combinada y con campos opcionales, para localizar rápidamente un neumático específico o un conjunto de neumáticos que cumplan ciertas características.

**Criterios de Aceptación** (CA3 ajustado por decisión del humano, el resto literal; valen para `GET /api/v1/neumaticos` y para la página `/tires`):

- CA1: Dado que un usuario ingresa una coincidencia parcial del número de serie, entonces el sistema debe retornar todos los neumáticos activos cuyo número de serie contenga dicha cadena.
- CA2: Dado que un usuario combina el filtro de número de serie con otros atributos técnicos opcionales (por ejemplo, marca y estado), cuando ejecuta la búsqueda, entonces el sistema debe retornar únicamente los neumáticos que cumplan simultáneamente con todos los criterios indicados.
- CA3: Dado que un usuario elige una situación operativa (En stock, Montado, En reparación o En espera), cuando ejecuta la búsqueda, entonces el sistema debe retornar únicamente los neumáticos activos con esa situación, pudiendo combinarse con los demás filtros (número de serie, marca, estado técnico).
- CA4: Dado que un usuario no completa ninguno de los campos de búsqueda, cuando ejecuta la búsqueda, entonces el sistema debe retornar el listado completo de neumáticos activos.
- CA5: La lista de resultados debe soportar paginación.

Precisiones verificables de los criterios anteriores:

- CA6: Dado un neumático cuyo número de serie coincide pero con baja lógica, o con `estadoActual = DE_BAJA`, o con `situacionOperativaVigente = DE_BAJA`, cuando se busca, entonces no aparece en `content` ni cuenta en `page.totalElements`. [CA1, CA4: "activos"]
- CA7: Dado un neumático activo con `enEspera = true` y `situacionOperativaVigente` distinta de `EN_ESPERA`, cuando se busca con `situacion=EN_ESPERA`, entonces no aparece. [CA3]
- CA8: Dado que hay más neumáticos activos que el tamaño de página, cuando se pide `page=<n>&size=<s>`, entonces la página trae a lo sumo `<s>` (10 sin `size`, acotado a 20) con `page.number`, `page.size`, `page.totalElements` y `page.totalPages`; ordenar por un campo no permitido responde 400 con `ExceptionInfo`. [CA5]
- CA9: Dado un usuario Lector, Editor o Administrador, cuando busca, entonces responde 200 (también con el rol de base `gn_lector`); sin token o con token inválido responde 401 con `ExceptionInfo`. ["cualquier nivel de acceso"]
- CA10: Dado un Administrador o Editor en `/tires`, cuando cambia "Número de serie", "Marca", "Estado" o "Situación operativa", entonces la interfaz pide el listado con todos los filtros completos y `page=0`; con todos vacíos, lo pide sin parámetros de filtro. [CA1–CA4]
- CA11: Dado que el resultado tiene más de una página, entonces `/tires` muestra "N neumáticos · página X de Y" y Anterior / Siguiente (deshabilitados en la primera y la última página) y pide `size=10`. [CA5]

**Notas / Supuestos**:

- **Neumático activo** (decisión del humano): sin baja lógica, con `estadoActual` distinto de `DE_BAJA` y con `situacionOperativaVigente` distinta de `DE_BAJA`.
- **Situación operativa** (decisión del humano): reemplaza la casilla "Solo en espera" de la historia. Filtra por `situacionOperativaVigente` (no por el booleano `enEspera`), un solo valor.
- **Mayúsculas** (decisión del humano): la búsqueda por número de serie no las distingue.
- Un filtro por número de serie nunca devuelve neumáticos sin número de serie.
- **Fuera de alcance — decide el humano**: abrir la página Neumáticos al Lector. En esta HU `/tires` sigue solo para administrador y editor; el endpoint sí admite al Lector (CA9).
- Fuera de alcance: ficha del neumático (HU-14; la fila no navega todavía).
- Sin runner de tests en el frontend: CA10 y CA11 se verifican con lint + typecheck + build y prueba en navegador.
