# Matriz de Trazabilidad Integral (Semana 8)

## Proyecto: EntregaSegura - Trazabilidad de Medicamentos
**Programa:** ADSO - Ficha 3169892  
**Estrategia de Calidad:** Shift-left Testing + Scrum  
**Objetivo:** Garantizar que todo test automatizado responda a un requisito, criterio de aceptación, riesgo mitigado o defecto de negocio conocido.

---

### Matriz de Trazabilidad: Requisito ➔ Criterio ➔ Riesgo ➔ Test Manual ➔ Test Automatizado

| Requisito / Historia de Usuario | Criterio de Aceptación (DoD / AC) | Riesgo Asociado | ID Caso Manual (Semana 3/4) | ID Test Automatizado (Semana 8) | Nivel de Prueba | Archivo de Prueba | Estado |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- | :---: |
| **HU-01: Gestión de Pedidos**<br>Como operador, requiero visualizar y actualizar los pedidos de medicamentos en ruta. | **AC-01.1:** Solo los usuarios autenticados con rol mensajero/operador pueden consultar órdenes.<br>**AC-01.2:** Si la orden no existe, responde 404. | Exposición no autorizada de datos de pacientes (Ley 1581 / Habeas Data). | `TC-MAN-07`<br>(Colección Postman: GET Order) | `TEST-API-03`<br>`TEST-API-04`<br>`TEST-API-05` | API (Supertest) | `tests/api/api-orders.test.js` | **PASÓ** |
| **HU-02: Transición de Estados**<br>Como despachador, requiero asegurar que el flujo de entrega respete el ciclo de vida del medicamento. | **AC-02.1:** Los estados son CREATED, ASSIGNED, IN_ROUTE, DELIVERED, CANCELLED.<br>**AC-02.2:** No se puede pasar de CANCELLED a DELIVERED ni de DELIVERED a IN_ROUTE. | Entregas fantasma, desvío de medicamentos o inconsistencia transaccional. | `TC-MAN-01`<br>`TC-MAN-02`<br>(Matriz de Transición de Estados) | `TEST-UNIT-01`<br>(Matriz parametrizada con 11 transiciones) | Unidad (Vitest) | `tests/unit/order-status.test.js` | **PASÓ** |
| **HU-02: Transición de Estados en API**<br>Como cliente PWA, envío PATCH para cambiar estado de orden. | **AC-02.3:** Si se solicita una transición no permitida, la API responde HTTP 409 Conflict con código `INVALID_STATUS_TRANSITION`. | Pérdida de integridad de datos por peticiones desordenadas de la PWA. | `TC-MAN-08`<br>(Colección Postman: PATCH Status) | `TEST-API-06` | API (Supertest) | `tests/api/api-orders.test.js` | **PASÓ** |
| **HU-03: Cadena de Frío**<br>Como químico farmacéutico, exijo validación de temperatura para fármacos termolábiles. | **AC-03.1:** Medicamentos refrigerados deben mantenerse entre 2.0°C y 8.0°C.<br>**AC-03.2:** Valores inferiores a 2.0°C o superiores a 8.0°C invalidan la cadena de frío. | Suministro de medicamento degradado o tóxico al paciente (Riesgo Vital). | `TC-MAN-03`<br>(Diseño BVA Semana 3) | `TEST-UNIT-02`<br>(BVA: 1.9, 2.0, 5.0, 8.0, 8.1°C, NaN) | Unidad (Vitest) | `tests/unit/order-status.test.js` | **PASÓ** |
| **HU-04: Confirmación con Evidencia (TDD)**<br>Como auditor de salud, requiero que toda entrega completada tenga soporte fotográfico. | **AC-04.1:** El servicio exige `evidenceUrl` no nulo para marcar DELIVERED.<br>**AC-04.2:** La URL debe ser segura (HTTPS) y formato imagen (.jpg/.png/.webp). | Imposibilidad de justificar entregas ante la EPS o reclamaciones por pérdida. | `TC-MAN-04`<br>`TC-MAN-05`<br>(Pruebas funcionales de entrega) | `TEST-TDD-01`<br>`TEST-TDD-02`<br>`TEST-TDD-03`<br>`TEST-INT-02` | Unidad / TDD / Integración | `tests/tdd/evidence-validation.test.js`<br>`tests/integration/delivery-service.test.js` | **PASÓ** |
| **HU-05: Integración del Servicio de Entrega**<br>Como mensajero, al marcar entrega se deben persistir datos y notificar al paciente. | **AC-05.1:** Persistencia obligatoria en repositorio.<br>**AC-05.2:** Disparo de notificación trazable al paciente.<br>**AC-05.3:** Aislamiento con dobles de prueba. | Fallo silencioso en notificación o falta de persistencia en base de datos. | `TC-MAN-04`<br>(Flujo completo de entrega) | `TEST-INT-01`<br>`TEST-INT-03`<br>`TEST-INT-04` | Integración (Vitest + Doubles) | `tests/integration/delivery-service.test.js` | **PASÓ** |
| **HU-06: Autenticación de Mensajeros**<br>Como sistema, restrinjo el acceso a las operaciones mediante credenciales seguras. | **AC-06.1:** Credenciales válidas retornan 200 + token.<br>**AC-06.2:** Credenciales erróneas retornan 401. | Ataques de fuerza bruta o suplantación en el reparto de medicamentos. | `TC-MAN-06`<br>(Postman Auth) | `TEST-API-01`<br>`TEST-API-02` | API (Supertest) | `tests/api/api-orders.test.js` | **PASÓ** |

---

### Resumen de Cobertura de Trazabilidad
- **Requisitos / Criterios cubiertos:** 100% de los criterios críticos de negocio de la Semana 8.
- **Trazabilidad horizontal:** Requisito ➔ Criterio ➔ Riesgo ➔ Caso Manual ➔ Test Automatizado ➔ Nivel de Pirámide.
- **Regla de Continuidad cumplida:** No existe ningún test en la suite sin justificación directa en la matriz.
