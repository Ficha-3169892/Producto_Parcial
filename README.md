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



---

## 7. Incremento Semana 8 — Servicios Web, Caché y Resiliencia (Android CTMA)

### Arquitectura Offline-First y Flujo Canónico de Datos
La aplicación implementa el patrón **Offline-First** recomendado por la guía de arquitectura Android:

$$\text{API REST} \longrightarrow \text{ActividadDto} \xrightarrow{\text{Mapper}} \text{ActividadEntity} \longrightarrow \text{Room DB} \xrightarrow{\text{Flow}} \text{UI (Compose)}$$

1. **Regla de fuente única**: Room es la única fuente de verdad observada por la UI a través de `Flow<List<ActividadFormativa>>`.
2. **Resiliencia de Caché**: Las respuestas HTTP fallidas (Timeout, 401, 5xx, Sin conexión) **nunca reemplazan ni vacían los datos locales** previamente almacenados en Room.
3. **Manejo Seguro de Autenticación**: El encabezado `Authorization: Bearer <token>` se inyecta centralizadamente mediante un interceptor en `NetworkModule` a través del contrato `TokenProvider`. No hay literales de token ni secretos hardcodeados en el código o en Git.
4. **Estados Independientes en UI**:
   - `ListadoUiState`: Representa el contenido local (`Cargando`, `Contenido`, `Vacio`, `Error`).
   - `OperacionUiState`: Representa la sincronización remota (`Inactiva`, `EnCurso`, `Exitosa`, `Fallida`).

### Contrato API REST (JSON)
- **Endpoint**: `GET /v1/actividades`
- **Respuesta 200 OK**:
```json
[
  {
    "id": 1,
    "titulo": "Revisión de Laboratorio CTMA",
    "descripcion": "Verificación de calibración de multímetros.",
    "fechaLimite": "2026-09-30",
    "estado": "PENDIENTE"
  }
]
```

### Casos de Aceptación Obligatorios (CA-01 a CA-08)
- **CA-01 (200 OK)**: Los datos de la API se parsean como `ActividadDto`, se mapean a `ActividadEntity` y se guardan atómicamente en Room.
- **CA-02 (200 OK Vacío)**: Una lista vacía válida no borra la caché previa en Room.
- **CA-03 (Timeout)**: Al agotarse el tiempo de espera (15s), Room conserva los datos y se notifica el fallo en `OperacionUiState.Fallida`.
- **CA-04 (Sin Red)**: `IOException` capturada y traducida a un estado recuperable con opción "Reintentar".
- **CA-05 (401 Unauthorized)**: Notifica "Sesión vencida" sin revelar tokens en logs ni en la interfaz.
- **CA-06 (500 Error)**: Clasificado como error de servidor; Room mantiene los datos válidos.
- **CA-07 (Doble Refresh Rápido)**: Controlado en `ActividadViewModel` mediante Jobs cancelables para evitar duplicados.
- **CA-08 (Cancelación de Corrutinas)**: `CancellationException` se relanza adecuadamente sin convertir la cancelación en un error visible.

### Mapeo con Historias de Usuario (HU) de la Semana 8

| ID Historia | Historia de Usuario | Criterio de Aceptación / Caso de Aceptación | Test Automatizado que lo Valida | Estado |
| :--- | :--- | :--- | :--- | :---: |
| **HU-07** | **Consulta de Actividades Offline**<br>Como aprendiz, quiero consultar mis actividades almacenadas previamente aunque no tenga conexión a internet. | **CA-04 / CA-03**: Al ocurrir falla de red o timeout, la app muestra los datos locales almacenados en Room sin borrar la lista y muestra la acción Reintentar. | `CA-03 - Timeout conserva el cache local y retorna operacion Fallida` | **PASÓ** |
| **HU-08** | **Sincronización Automática con API REST**<br>Como aprendiz, quiero que mis actividades se actualicen automáticamente con el servidor cuando haya conexión. | **CA-01**: Al recibir una respuesta 200 OK de la API `/v1/actividades`, la app inserta los datos en Room y actualiza la lista mediante `Flow`. | `CA-01 - 200 con actividades guarda en Room y actualiza el estado a Exitosa` | **PASÓ** |
| **HU-09** | **Protección de Sesión y Credenciales**<br>Como sistema, exijo autenticación mediante token para consumir la API sin exponer credenciales en la interfaz ni en logs. | **CA-05**: Ante un error 401 Unauthorized, la app notifica sesión vencida e inyecta el token en la cabecera HTTP sin imprimirlo en Logcat. | `CA-05 - 401 retorna estado Fallida clasificado con mensaje de sesion vencida` | **PASÓ** |
| **HU-10** | **Resiliencia ante Errores de Servidor**<br>Como usuario, quiero recibir mensajes accionables claros cuando el servidor falle, sin perder mis datos previos. | **CA-02 / CA-06**: Ante respuestas 500, JSON inválido o arreglos vacíos de la API, la app clasifica el error y preserva la caché previa intacta. | `CA-02 - 200 con arreglo vacio no borra la cache previa`<br>`CA-06 - 500 retorna estado Fallida clasificando error de servidor` | **PASÓ** |


