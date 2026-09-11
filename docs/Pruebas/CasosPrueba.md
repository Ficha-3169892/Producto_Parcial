# Selección de Candidatos para Automatización de Pruebas (Actividad 3.4)

## Proyecto: EntregaSegura - Trazabilidad de Medicamentos
**Programa de Formación:** Análisis y Desarrollo de Software (ADSO) - Ficha 3169892  
**Semana:** 8  
**Herramientas:** Vitest, Supertest, V8 Coverage  

---

### Criterios de Selección y Evaluación de Candidatos
Para evitar el antipatrón de "automatizar por moda", el equipo evalúa los casos de prueba de las semanas 3 y 4 bajo la siguiente heurística de decisión:
1. **Frecuencia y Repetibilidad:** ¿Se ejecuta en cada build, pull request o ciclo de regresión?
2. **Determinismo:** ¿Las entradas y salidas son 100% predecibles sin ambigüedad humana?
3. **Costo / Valor / Nivel:** ¿En qué nivel de la pirámide (Unidad, Integración, API, E2E) es más rápida, barata y aislada su comprobación?
4. **Riesgo:** ¿Un fallo en esta funcionalidad pone en peligro la salud del paciente o viola regulaciones sanitarias (INVIMA)?

---

### Matriz de Selección de Candidatos (10 Casos Mínimos de Semanas 3 y 4)

| ID Caso | Descripción del Caso | Riesgo Asociado | Frecuencia | Determinista | Nivel Sugerido | ¿Automatizar? | Motivo Técnico de la Decisión | Referencia Origen |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: | :--- | :--- |
| **TC-01** | Transición de estado: `CANCELLED` -> `DELIVERED` rechazada | **Crítico** (Entrega fraudulenta o error de cobro de medicamento cancelado) | Alta (Cada cambio en core) | Sí | Unidad / Dominio | **SÍ** | Regla pura de negocio. Es instantánea en Vitest (<2ms) y previene errores críticos. | Semana 3 (Tabla de Estados) / HU-02 |
| **TC-02** | Matriz exhaustiva de transiciones permitidas (`CREATED` -> `ASSIGNED` -> `IN_ROUTE` -> `DELIVERED`) | **Alto** (Inconsistencia en el flujo de entrega de medicamentos) | Alta (En cada build) | Sí | Unidad (Parametrizada) | **SÍ** | Se convierte en tabla `describe.each`, compacta y de mantenimiento mínimo. | Semana 3 (Máquina de estados) / HU-01 |
| **TC-03** | Rango normativo de temperatura en cadena de frío (2.0°C a 8.0°C) con análisis de valores límite | **Crítico** (Pérdida de eficacia o toxicidad del fármaco biológico) | Alta | Sí | Unidad (BVA/Equivalencia) | **SÍ** | Se verifican fronteras (1.9, 2.0, 8.0, 8.1°C) de forma determinista y matemática. | Semana 3 (Valores límite) / HU-03 |
| **TC-04** | Intento de completar entrega sin URL de evidencia fotográfica | **Alto** (Imposibilidad de auditar entrega real ante reclamos) | Alta | Sí | Integración (Servicio) | **SÍ** | Comprueba regla TDD de orquestación y dobles de repositorio/notificador. | Semana 2 (CA-02.3) / HU-02 |
| **TC-05** | Formato y seguridad de URL de evidencia (Protocolo HTTPS y extensión .jpg/.png) | **Medio** (Inyección de archivos maliciosos o enlaces caídos) | Media | Sí | Unidad / TDD | **SÍ** | Validación regex aislada y rápida en microciclo TDD. | Semana 3 (Validación de entradas) / HU-02 |
| **TC-06** | Autenticación de mensajero mediante `POST /api/auth/login` con credenciales válidas e inválidas | **Crítico** (Suplantación de identidad en reparto de sustancias controladas) | Alta (Regresión continua) | Sí | API (Supertest) | **SÍ** | Convierte caso Postman repetitivo en prueba ejecutable de contrato HTTP. | Semana 4 (Postman Auth) / HU-04 |
| **TC-07** | Consulta de orden protegida `GET /api/orders/:id` con token y rechazo por falta de token | **Alto** (Fuga de información médica y datos de pacientes) | Alta | Sí | API (Supertest) | **SÍ** | Garantiza seguridad perimetral HTTP (401 Unauthorized y 200 OK). | Semana 4 (Postman Orders) / HU-01 |
| **TC-08** | Actualización de orden `PATCH /api/orders/:id/status` respondiendo HTTP 409 ante transición ilegal | **Alto** (Desincronización cliente PWA - backend API) | Alta | Sí | API (Supertest) | **SÍ** | Comprueba contrato HTTP, códigos de estado REST y persistencia en almacén. | Semana 4 (Postman Status) / HU-02 |
| **TC-09** | Sensibilidad y fluidez del trazo de firma táctil digital en pantalla táctil del móvil bajo luz solar | **Bajo** (Incomodidad de usuario mensajero) | Baja | **No** (Subjetivo/Hardware) | Manual (Usabilidad) | **NO (Manual)** | Depende del hardware del dispositivo, respuesta capacitiva de la pantalla y percepción táctil. Automatizarla generaría pruebas frágiles y de altísimo costo. | Semana 4 (Pruebas Exploratorias PWA) |
| **TC-10** | Comprobación física de integridad de precinto de seguridad del envase térmico de medicamentos | **Crítico** (Contaminación o manipulación del fármaco) | En cada despacho | **No** (Inspección física humana) | Manual (Física/Inspección) | **NO (Manual)** | Imposible de comprobar mediante código de software; requiere inspección visual y táctil del operario farmacéutico. | Semana 4 (Checklist Físico) |

---

### Justificación de Casos que Permanecen Deliberadamente Manuales
- **TC-09 (Firma táctil en PWA):** Las pruebas automatizadas a nivel de interfaz táctil requieren emuladores pesados y no evalúan la ergonomía ni la fricción del mensajero en movimiento o con guantes. Se mantiene en pruebas exploratorias de usabilidad en campo.
- **TC-10 (Inspección física del precinto):** La calidad total en EntregaSegura involucra protocolos de buenas prácticas de almacenamiento y transporte (BPAyT). El precinto físico es una barrera mecánica que debe validarse por procedimiento operacional estándar (SOP) humano, no por software.
