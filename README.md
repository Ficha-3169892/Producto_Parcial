# EntregaSegura - Automatización de Pruebas (Semana 8)

> **Programa:** Análisis y Desarrollo de Software (ADSO) - SENA  
> **Ficha:** 3169892  
> **Guía de Aprendizaje:** Semana 8 - *Automatización de pruebas · Vitest/Jest · Supertest · TDD · Scrum + Shift-left*  
> **Caso Integrador:** EntregaSegura - PWA/API para gestión trazable de entregas de medicamentos

---

## 1. Descripción General del Proyecto

**EntregaSegura** es una solución diseñada para garantizar la trazabilidad inmutable en la entrega domiciliaria de medicamentos para pacientes con tratamientos sensibles. En esta Semana 8, se consolida la estrategia de **Shift-left testing** integrando calidad de software dentro del flujo Scrum.

Se pasa de ejecuciones manuales aisladas a una **suite rápida, determinista, aislada y ejecutable en milisegundos**, estructurada en la pirámide de pruebas:
- **Pruebas Unitarias y Parametrizadas (Vitest):** Máquina de estados y análisis de valores límite (BVA) en cadena de frío (2.0°C - 8.0°C).
- **Pruebas de Integración (Vitest + Fixtures + Doubles):** Servicio de entrega con Stubs de repositorio y Mocks de notificación.
- **Pruebas de Regresión de API (Supertest):** Verificación de contratos HTTP (`POST /api/auth/login`, `GET /api/orders/:id`, `PATCH /api/orders/:id/status`).
- **Desarrollo Guiado por Pruebas (TDD):** Microciclo *Red-Green-Refactor* para la regla de validación de evidencias seguras.

---

## 2. Requisitos y Configuración Rápida

### Requisitos Previos
- **Node.js:** Versión 18.x, 20.x o 22.x (Recomendado v20+)
- **npm:** Versión 9+

### Instalación de Dependencias
```bash
npm install
```

---

## 3. Comandos de Ejecución

### Modo Interactivo / Watch
```bash
npm test
```

### Ejecución de Suite Completa (Modo Run)
```bash
npm run test:run
```

### Generación de Reporte de Cobertura (V8)
```bash
npm run test:coverage
```
El reporte detallado en HTML se genera en la carpeta `coverage/index.html`.

---

## 4. Estructura del Código y Suites de Prueba

```text
├── src/
│   ├── api/
│   │   └── app.js                 # Servidor Express, middleware auth y endpoints
│   ├── domain/
│   │   └── order-status.js        # Máquina de estados y regla de cadena de frío
│   ├── fixtures/
│   │   └── order.fixture.js       # Factory de órdenes sintéticas para pruebas
│   ├── services/
│   │   └── delivery-service.js    # Servicio de negocio para completar entregas
│   └── utils/
│       └── evidence-validator.js  # Validador de URLs de evidencia (TDD)
│
├── tests/
│   ├── api/
│   │   └── api-orders.test.js            # Lab 3: Regresión HTTP con Supertest
│   ├── integration/
│   │   └── delivery-service.test.js      # Lab 2: Servicio con Stubs y Mocks
│   ├── tdd/
│   │   └── evidence-validation.test.js   # Lab 4: Microciclo TDD (Red-Green-Refactor)
│   └── unit/
│       └── order-status.test.js          # Lab 1: Pruebas unitarias parametrizadas y BVA
│
├── docs/
│   ├── Pruebas/
│   │   ├── CasosPrueba.md         # Actividad 3.4: Selección y justificación de candidatos
│   │   ├── Defectos.md            # Registro y causas de defectos resueltos
│   │   ├── MatrizTrazabilidad.md  # Trazabilidad Requisito ➔ Manual ➔ Automatizado
│   │   └── PlanPruebas.md         # Plan de pruebas, pirámide y criterios
│   └── Scrum/
│       ├── DefinitionOfDone.md    # DoD refinada con criterios de automatización
│       ├── ProductBacklog.md      # Backlog priorizado del producto
│       ├── ProductGoal.md         # Meta general del producto EntregaSegura
│       ├── Retrospective.md       # Mini retrospectiva (Conservar, Eliminar, Mejorar)
│       ├── SprintBacklog.md       # Tareas técnicas estimadas de calidad
│       └── SprintGoal.md          # Meta del Sprint 4 de Automatización
│
├── package.json
└── vitest.config.js
```

---

## 5. Resultados de Ejecución y Métricas de Cobertura

- **Total de Tests Automatizados:** 40 pruebas en 4 archivos.
- **Resultado:** 100% pasando (`40 passed`).
- **Tiempo de Ejecución:** ~2 segundos.
- **Repetibilidad:** Ejecutada más de 3 veces consecutivas con cero *flaky tests*.
- **Cobertura de Código (V8):**
  - **Líneas (% Lines):** ~91%
  - **Funciones (% Funcs):** 100%
  - **Ramas (% Branch):** >80% (100% en utilidades críticas y dominio de estados)

---

## 6. Documentación Metodológica y Scrum

Para consultar el soporte documental completo del entregable de la Semana 8:
- [Selección de Casos Candidatos (Actividad 3.4)](docs/Pruebas/CasosPrueba.md)
- [Matriz de Trazabilidad Integral](docs/Pruebas/MatrizTrazabilidad.md)
- [Plan de Pruebas de Software](docs/Pruebas/PlanPruebas.md)
- [Registro de Defectos](docs/Pruebas/Defectos.md)
- [Sprint Goal](docs/Scrum/SprintGoal.md)
- [Sprint Backlog Técnico](docs/Scrum/SprintBacklog.md)
- [Definition of Done Refinada](docs/Scrum/DefinitionOfDone.md)
- [Sprint Retrospective](docs/Scrum/Retrospective.md)
