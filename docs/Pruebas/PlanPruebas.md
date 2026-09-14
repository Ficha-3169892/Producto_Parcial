# Plan de Pruebas de Software - Semana 8 (Shift-Left & Automatización)

## 1. Información General del Proyecto
- **Sistema:** EntregaSegura (PWA / API para trazabilidad de medicamentos)
- **Alcance de la Semana:** Automatización en niveles Unidad, Integración, API (HTTP) e introducción a TDD
- **Frameworks:** Vitest 3.x, Supertest 7.x, @vitest/coverage-v8
- **Enfoque Metodológico:** Shift-left testing integrado en el ciclo Scrum (Sprint 4)

---

## 2. Objetivos de Prueba
1. Automatizar los casos de prueba de mayor frecuencia y riesgo definidos en las semanas 3 y 4.
2. Garantizar una retroalimentación rápida (Fast Feedback Loop) en menos de 5 segundos para pruebas unitarias e integración.
3. Asegurar el cumplimiento de las reglas normativas farmacéuticas (cadena de frío entre 2.0°C y 8.0°C y transiciones de estado de medicamentos).
4. Proveer pruebas deterministas, aisladas e independientes (cero dependencia de orden o datos residuales).

---

## 3. Estrategia y Pirámide de Testing
El equipo aplica el modelo de la pirámide de testing para optimizar costos de ejecución y mantenimiento:

```
          / \
         /   \      E2E (Pruebas futuras PWA - Semana 9)
        /-----\
       /  API  \    Regresión HTTP con Supertest (Contratos, Códigos HTTP, Headers)
      /---------\
     / INTEGRACIÓN\ Servicios con Stubs (Repo) y Mocks (Notificaciones)
    /-------------\
   /    UNIDAD     \ Lógica pura de dominio (canTransition, isColdChainValid, TDD)
  /-----------------\
```

### 3.1 Nivel Unitario (Base)
- **Enfoque:** Pruebas aisladas sobre funciones puras en `src/domain/` y utilidades en `src/utils/`.
- **Técnicas:** Pruebas parametrizadas (`describe.each`), Análisis de Valores Límite (BVA) y Particiones de Equivalencia.
- **Velocidad:** < 50ms por suite.

### 3.2 Nivel de Integración (Medio)
- **Enfoque:** Colaboración entre el servicio de entrega (`completeDelivery`) y sus dependencias externas.
- **Dobles de prueba:**
  - **Stub:** Simula respuestas controladas de repositorio (`findById`) sin base de datos real.
  - **Mock:** Verifica llamadas y parámetros de persistencia (`repository.save`) y notificación al paciente (`notifier.send`).
- **Aislamiento:** Limpieza estricta mediante `vi.clearAllMocks()` en el hook `beforeEach()`.

### 3.3 Nivel de API / Contrato HTTP
- **Enfoque:** Verificación de frontera REST con Supertest sin levantar un puerto de red real en el SO.
- **Endpoints evaluados:**
  - `POST /api/auth/login` (Autenticación y tokens de prueba).
  - `GET /api/orders/:id` (Control de acceso y respuestas 200, 401, 404).
  - `PATCH /api/orders/:id/status` (Transiciones válidas con evidencia y rechazo 409 `INVALID_STATUS_TRANSITION`).

---

## 4. Gestión de Datos de Prueba y Seguridad
- **Datos Sintéticos:** Implementación de factories/fixtures (`orderFixture`) con IDs controlados (`ORD-101`, `PAT-TEST-01`).
- **Privacidad:** Queda estrictamente prohibido el uso de cédulas, nombres o registros clínicos reales de pacientes.
- **Gestión de Secretos:** Se utilizan tokens estáticos de prueba (`TEST_COURIER_TOKEN`), nunca llaves de producción.

---

## 5. Criterios de Entrada, Suspensión y Salida

### Criterios de Entrada
- Historias de usuario refinadas con criterios de aceptación claros (Gherkin o checklist).
- Entorno de Node.js v20+ y Vitest configurado en el repositorio.

### Criterios de Suspensión
- Presencia de pruebas intermitentes (*flaky tests*) que fallen sin cambios en el código.
- Bloqueo en la instalación de dependencias npm o corrupción del runner.

### Criterios de Salida
- 100% de los tests automatizados pasando en verde (`40/40 passed`).
- Cobertura de código superior al 80% en líneas y ramas críticas.
- Ejecución repetible (mínimo 3 ejecuciones consecutivas con el mismo resultado).
- Microciclo TDD documentado en bitácora y código.
- Matriz de trazabilidad y artefactos Scrum actualizados.
