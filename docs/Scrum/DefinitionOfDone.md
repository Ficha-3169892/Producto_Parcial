# Definition of Done (DoD) Refinada - Sprint 4 (Semana 8)

## Proyecto: EntregaSegura - PWA/API para Gestión Trazable de Medicamentos
**Versión:** 2.0 (Integración de Shift-left y Automatización de Pruebas)

---

### Compromiso de Calidad del Scrum Team

Para que cualquier Historia de Usuario, Tarea Técnica o Incremento de Producto sea considerado **Terminado (Done)** y potencialmente desplegable, debe satisfacer rigurosamente los siguientes 8 criterios de calidad:

```
                  ┌────────────────────────────────────────────────────────┐
                  │              DEFINITION OF DONE (DoD)                  │
                  ├────────────────────────────────────────────────────────┤
                  │ [✓] 1. Criterios de Aceptación (AC) verificados        │
                  │ [✓] 2. Pruebas Unitarias implementadas y en verde     │
                  │ [✓] 3. Regresión de API (Supertest) automatizada       │
                  │ [✓] 4. Cobertura analizada y huecos justificados       │
                  │ [✓] 5. Aislamiento, datos sintéticos y cero secretos   │
                  │ [✓] 6. Repetibilidad probada (0% Flaky Tests)          │
                  │ [✓] 7. Matriz de trazabilidad actualizada              │
                  │ [✓] 8. Revisión de código (Code Review / Peer Review)  │
                  └────────────────────────────────────────────────────────┘
```

---

### Detalle Operativo de los Criterios

1. **Criterios de Aceptación Satisfechos:**
   - La funcionalidad cumple con todas las reglas de negocio declaradas en la historia de usuario (Gherkin o checklist de criterios).

2. **Pruebas Unitarias Relevantes en Verde:**
   - Todo módulo de dominio o utilidad cuenta con tests unitarios bajo patrón Arrange-Act-Assert (AAA).
   - Uso de pruebas parametrizadas para máquinas de estado o tablas de decisión.
   - La suite ejecuta localmente con `npm run test:run` sin ningún fallo (`0 failed`).

3. **Regresión de API Automatizada con Supertest:**
   - Los endpoints HTTP modificados cuentan con pruebas de contrato que validan códigos de estado HTTP (200, 400, 401, 404, 409), estructura del cuerpo JSON y encabezados.

4. **Cobertura Analizada sin Dogmatismo Numérico:**
   - Se genera el reporte con `@vitest/coverage-v8`.
   - Se exige un análisis cualitativo de ramas (*branches*) en lógica crítica de medicamentos (p. ej. validación de cadena de frío y transiciones prohibidas).
   - Todo hueco de cobertura debe estar técnicamente justificado.

5. **Aislamiento, Datos Sintéticos y Seguridad:**
   - Queda estrictamente prohibido el uso de datos reales de pacientes (nombres, cédulas, diagnósticos).
   - Se emplean factories/fixtures (`orderFixture`) y dobles de prueba (stubs/mocks) con limpieza obligatoria (`vi.clearAllMocks()`).
   - No se almacenan secretos ni credenciales reales en el código de prueba; se usan tokens de prueba (`TEST_COURIER_TOKEN`).

6. **Repetibilidad y Ausencia de Flaky Tests:**
   - La suite debe ejecutarse al menos 3 veces consecutivas sin modificar código, arrojando exactamente el mismo resultado determinista.
   - Las pruebas no deben depender del orden de ejecución ni de persistencia compartida.

7. **Matriz de Trazabilidad y Evidencias Actualizadas:**
   - Cada prueba automatizada tiene su identificador registrado en `docs/Pruebas/MatrizTrazabilidad.md`, vinculada a un requisito o riesgo.

8. **Revisión de Código y Cero Defectos Críticos:**
   - No existen defectos abiertos de severidad Alta o Crítica asociados al incremento.
   - El código cumple con las directrices de formato, modularidad y legibilidad acordadas por el equipo.
