# Sprint Backlog - Sprint 4 (Semana 8)

## Proyecto: EntregaSegura
**Sprint Goal:** Infraestructura de automatización de pruebas unitarias, de integración y API con TDD y trazabilidad completa.

---

### Desglose de Historias de Usuario y Tareas Técnicas de Calidad

| ID Ítem / Tarea | Descripción de la Tarea Técnica | Tipo de Tarea | Estimación (Horas) | Responsable | Estado | Contribución al Sprint Goal |
| :--- | :--- | :---: | :---: | :--- | :---: | :--- |
| **US-01** | **Configuración del Runner de Pruebas y Cobertura** | Infraestructura | 1h | Jhostyn | **Hecho (Done)** | Permite ejecutar pruebas en milisegundos con Vitest y V8. |
| ↳ TK-01.1 | Configurar `package.json` con scripts ESM y dependencias de test. | Técnica | 0.5h | Jhostyn | **Hecho** | Base operativa del proyecto. |
| ↳ TK-01.2 | Configurar `vitest.config.js` con soporte de cobertura V8 e informes. | Técnica | 0.5h | Jhostyn | **Hecho** | Medición objetiva de líneas y ramas ejecutadas. |
| **US-02** | **Laboratorio 1: Suite Unitaria de Dominio y Transición de Estados** | Testing Unitario | 2h | Jhostyn | **Hecho (Done)** | Garantiza que ninguna orden pase a estados ilegales en memoria. |
| ↳ TK-02.1 | Implementar máquina de estados en `order-status.js`. | Desarrollo | 1h | Jhostyn | **Hecho** | Lógica pura de negocio. |
| ↳ TK-02.2 | Crear pruebas parametrizadas con `describe.each` para 11 transiciones. | Testing | 0.5h | Jhostyn | **Hecho** | Validación exhaustiva del ciclo de vida. |
| ↳ TK-02.3 | Diseñar e implementar pruebas de Valores Límite (BVA) en cadena de frío. | Testing | 0.5h | Jhostyn | **Hecho** | Verificación de fronteras 1.9°C, 2.0°C, 8.0°C, 8.1°C. |
| **US-03** | **Laboratorio 2: Integración de Servicio de Entrega con Dobles de Prueba** | Testing Integración | 1.5h | Jhostyn | **Hecho (Done)** | Aísla la lógica de negocio de la base de datos y mensajería externa. |
| ↳ TK-03.1 | Diseñar fixtures sintéticos de órdenes médicas en `order.fixture.js`. | Testing | 0.5h | Jhostyn | **Hecho** | Cero exposición de datos reales de pacientes. |
| ↳ TK-03.2 | Implementar `delivery-service.js` con dependencias inyectables. | Desarrollo | 0.5h | Jhostyn | **Hecho** | Arquitectura desacoplada y comprobable. |
| ↳ TK-03.3 | Automatizar suite con Stubs (repo) y Mocks (notificador y save) en Vitest. | Testing | 0.5h | Jhostyn | **Hecho** | Verificación de interacción y efectos colaterales. |
| **US-04** | **Laboratorio 3: Regresión de API con Supertest** | Testing API / HTTP | 2h | Jhostyn | **Hecho (Done)** | Reemplaza la ejecución manual en Postman por suite automatizada. |
| ↳ TK-04.1 | Crear endpoints en `app.js` (`POST /auth/login`, `GET /orders/:id`, `PATCH /status`). | Desarrollo | 1h | Jhostyn | **Hecho** | API REST protegida y tipada. |
| ↳ TK-04.2 | Automatizar casos de autenticación (200 / 401) y consulta (200 / 401 / 404). | Testing | 0.5h | Jhostyn | **Hecho** | Seguridad perimetral y contratos HTTP. |
| ↳ TK-04.3 | Automatizar caso de transición rechazada 409 y actualización exitosa 200. | Testing | 0.5h | Jhostyn | **Hecho** | Regresión de frontera HTTP. |
| **US-05** | **Laboratorio 4: Microciclo TDD (Red-Green-Refactor)** | TDD / Diseño | 1h | Jhostyn | **Hecho (Done)** | Asegura diseño guiado por pruebas antes de codificar. |
| ↳ TK-05.1 | Escribir pruebas que fallan para `isValidEvidenceUrl` (Fase RED). | TDD | 0.3h | Jhostyn | **Hecho** | Expresión explícita del requisito. |
| ↳ TK-05.2 | Implementar código mínimo funcional para pasar la prueba (Fase GREEN). | TDD | 0.3h | Jhostyn | **Hecho** | Cumplimiento del contrato mínimo. |
| ↳ TK-05.3 | Refactorizar con regex estricto HTTPS y querystrings (Fase REFACTOR). | TDD | 0.4h | Jhostyn | **Hecho** | Código limpio, robusto y testeado. |
| **US-06** | **Documentación Metodológica y Cierre Scrum** | Gestión Scrum | 1.5h | Jhostyn | **Hecho (Done)** | Transparencia del incremento y trazabilidad completa. |
| ↳ TK-06.1 | Elaborar selección de candidatos y justificaciones (Actividad 3.4). | Análisis | 0.5h | Jhostyn | **Hecho** | Criterio técnico vs automatización ciega. |
| ↳ TK-06.2 | Elaborar matriz de trazabilidad y actualizar DoD y Retrospectiva. | Scrum / QA | 1h | Jhostyn | **Hecho** | Cumplimiento del 100% de la rúbrica evaluativa. |

---

### Registro de Impedimentos y Bloqueos Gestionados
1. **Bloqueo Inicial:** Riesgo de contaminación de estado entre pruebas consecutivas en Supertest.  
   *Solución:* Se diseñó `resetOrderStore()` invocado antes de cada prueba.
2. **Bloqueo de Cobertura:** Configuración inicial de V8 requería excluir fixtures sintéticos para no desvirtuar métricas de código de producción.  
   *Solución:* Filtro `exclude: ['src/fixtures/**']` en `vitest.config.js`.
