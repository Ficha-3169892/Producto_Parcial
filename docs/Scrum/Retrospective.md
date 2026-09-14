# Sprint Retrospective - Sprint 4 (Semana 8)

## Proyecto: EntregaSegura
**Scrum Team:** Ficha ADSO 3169892  
**Fecha:** Septiembre de 2026  
**Enfoque de la Retrospectiva:** Mini Retrospective (Semana 8 - Shift-Left & Automatización)

---

### Dinámica de la Retrospectiva: Conservar, Eliminar y Mejorar

```
     ┌──────────────────────┬──────────────────────┬──────────────────────┐
     │   CONSERVAR (Keep)   │  ELIMINAR (Discard)  │   MEJORAR (Improve)  │
     ├──────────────────────┼──────────────────────┼──────────────────────┤
     │ Pruebas paramétricas │ Regresión manual de  │ Extender el pipeline │
     │ AAA y microciclos    │ Postman para casos   │ de CI con Vitest     │
     │ TDD en reglas clave. │ deterministas.       │ automatizado en PRs. │
     └──────────────────────┴──────────────────────┴──────────────────────┘
```

---

### 1. ¿Qué práctica debemos CONSERVAR? (Keep)
- **Uso de Pruebas Parametrizadas y Patrón AAA:**  
  La estructura de `describe.each` en `tests/unit/order-status.test.js` permitió probar 11 transiciones de estado y 11 escenarios de temperatura en menos de 20ms, con código limpio y sin repeticiones.
- **TDD en Reglas Críticas:**  
  Escribir primero la prueba que falla para la validación de evidencia garantizó que la función naciera exactamente con las especificaciones requeridas sin sobreingeniería.
- **Aislamiento Estricto con Hooks:**  
  El uso de `resetOrderStore()` y `vi.clearAllMocks()` en `beforeEach()` eliminó completamente los falsos positivos y pruebas intermitentes (*flaky tests*).

---

### 2. ¿Qué DESPERDICIO debemos ELIMINAR? (Discard)
- **Ejecución Manual Repetitiva de Postman en Regresión:**  
  Comprobar manualmente en Postman endpoints básicos en cada cambio consumía horas del equipo y generaba fatiga. Al convertirlos en tests de Supertest (`tests/api/api-orders.test.js`), la suite completa corre en ~200ms.
- **Mocking Excesivo de Lógica Pura:**  
  Se descarta la práctica de mockear funciones que no tienen efectos colaterales. Los dobles se reservan estrictamente para fronteras costosas o inestables (repositorio y notificador).
- **Aserciones Débiles:**  
  Se erradicó el uso de `toBeDefined()` o `toBeTruthy()` sin validar el código de error o el contenido del payload.

---

### 3. ¿Qué MEJORA proponemos para el siguiente Sprint? (Improve)
- **Integración Continua (CI - Shift-Left en Git):**  
  Para la Semana 9, integrar la ejecución automática de `npm run test:run` y `npm run test:coverage` en un workflow de GitHub Actions que bloquee el merge si algún test falla o la cobertura disminuye.
- **Migración a Pruebas E2E de Flujos Críticos:**  
  Incorporar pruebas End-to-End con Playwright sobre la PWA para validar el flujo completo del mensajero desde la interfaz móvil.
