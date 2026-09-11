# Registro y Gestión de Defectos (Semana 8)

## Proyecto: EntregaSegura - Control de Calidad
**Enfoque:** Shift-left Testing (Detección temprana de defectos en fases de unidad y API)

---

### Bitácora de Defectos Identificados y Subsanados Durante la Automatización

| ID Defecto | Descripción del Defecto | Severidad | Prioridad | Nivel de Detección | Causa Raíz | Acción Correctiva Implementada | Estado |
| :--- | :--- | :---: | :---: | :---: | :--- | :--- | :---: |
| **DEF-01** | **Excepción 500 no controlada ante falta de evidencia**<br>Al invocar `PATCH /api/orders/:id/status` con estado `DELIVERED` sin suministrar `evidenceUrl`, el servicio arrojaba una excepción no capturada que provocaba error 500 Internal Server Error en la API. | **Alta** | **Alta** | API (Supertest) | Ausencia de bloque `try/catch` discriminando instancias de `DomainError`. | Se implementó captura explícita de `DomainError` retornando HTTP 400 Bad Request con `{ code: 'EVIDENCE_REQUIRED' }`. | **Cerrado / Verificado** |
| **DEF-02** | **Transición inválida sin detalle de oráculo**<br>Al intentar pasar de `CANCELLED` a `DELIVERED`, la respuesta no especificaba el estado actual ni el solicitado, dificultando la depuración en el cliente PWA. | **Media** | **Media** | API / Integración | El handler HTTP únicamente devolvía un mensaje de texto genérico. | Se enriqueció el payload de error HTTP 409 con `{ code: 'INVALID_STATUS_TRANSITION', currentStatus, requestedStatus }`. | **Cerrado / Verificado** |
| **DEF-03** | **Polución de estado compartido en pruebas continuas**<br>En la segunda ejecución de la suite de Supertest, la prueba de entrega fallaba intermitentemente porque la orden `ORD-101` ya había mutado a estado `DELIVERED` en la prueba anterior. | **Crítica** (Flaky Test) | **Alta** | API (Repetibilidad) | El almacén `orderStore` en memoria mantenía mutaciones entre diferentes tests. | Se creó la función `resetOrderStore()` invocada en el hook `beforeEach()` para aislar completamente el estado de cada test. | **Cerrado / Verificado** |
| **DEF-04** | **Aceptación de URLs no seguras (HTTP) en evidencias**<br>Durante el ciclo TDD se evidenció que la validación primitiva de evidencias aceptaba URLs inseguras bajo protocolo `http://`, vulnerando la integridad del contenido. | **Media** | **Media** | Unidad / TDD | Expresión regular permisiva que no exigía prefijo `https://`. | Se ajustó la expresión regular estricta en `isValidEvidenceUrl` exigiendo `https://` y formatos `.jpg`, `.png`, `.webp`. | **Cerrado / Verificado** |

---

### Conclusión y Análisis de Deuda Técnica
La adopción temprana de pruebas automatizadas permitió corregir 4 defectos funcionales y de arquitectura antes del despliegue a producción. La eliminación de dependencias compartidas mediante fixtures y `resetOrderStore` garantizó un 0% de pruebas intermitentes (*flaky tests*).
