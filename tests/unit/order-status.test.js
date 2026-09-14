import { describe, expect, it } from 'vitest'
import { canTransition, isColdChainValid, ORDER_STATUS } from '../../src/domain/order-status.js'

describe('Laboratorio 1: Suite Unitaria de Dominio - EntregaSegura', () => {
  describe('Matriz de Transición de Estados (Pruebas Parametrizadas)', () => {
    describe.each([
      // [From, To, Expected, Descripción]
      [ORDER_STATUS.CREATED, ORDER_STATUS.ASSIGNED, true, 'Permite asignar mensajero a orden creada'],
      [ORDER_STATUS.ASSIGNED, ORDER_STATUS.IN_ROUTE, true, 'Permite iniciar ruta de entrega'],
      [ORDER_STATUS.IN_ROUTE, ORDER_STATUS.DELIVERED, true, 'Permite completar entrega en destino'],
      [ORDER_STATUS.CREATED, ORDER_STATUS.CANCELLED, true, 'Permite cancelar orden recién creada'],
      [ORDER_STATUS.ASSIGNED, ORDER_STATUS.CANCELLED, true, 'Permite cancelar orden asignada antes de salir'],
      [ORDER_STATUS.IN_ROUTE, ORDER_STATUS.CANCELLED, true, 'Permite cancelar orden en ruta por contingencia'],
      [ORDER_STATUS.CANCELLED, ORDER_STATUS.DELIVERED, false, 'Rechaza entrega de una orden previamente cancelada'],
      [ORDER_STATUS.DELIVERED, ORDER_STATUS.IN_ROUTE, false, 'Rechaza reactivar ruta de una orden ya entregada'],
      [ORDER_STATUS.CREATED, ORDER_STATUS.DELIVERED, false, 'Rechaza saltar directamente de creada a entregada'],
      [ORDER_STATUS.DELIVERED, ORDER_STATUS.CANCELLED, false, 'Rechaza cancelar una orden ya entregada al paciente'],
      [ORDER_STATUS.CANCELLED, ORDER_STATUS.ASSIGNED, false, 'Rechaza reasignar una orden que fue cancelada'],
    ])('Transición: %s -> %s', (from, to, expected, description) => {
      it(`${description} -> expected=${expected}`, () => {
        // Arrange
        const currentStatus = from
        const targetStatus = to

        // Act
        const isAllowed = canTransition(currentStatus, targetStatus)

        // Assert
        expect(isAllowed).toBe(expected)
      })
    })

    it('rechaza transiciones con parámetros nulos o indefinidos', () => {
      // Arrange
      const nullFrom = null
      const validTo = ORDER_STATUS.DELIVERED

      // Act
      const result = canTransition(nullFrom, validTo)

      // Assert
      expect(result).toBe(false)
    })
  })

  describe('Técnica de Valores Límite (BVA) y Clases de Equivalencia - Cadena de Frío (2.0°C - 8.0°C)', () => {
    describe.each([
      // [Temperatura, Esperado, Justificación Técnica]
      [1.9, false, 'Valor límite inferior - 0.1 (Fuera de rango por subenfriamiento/congelación)'],
      [2.0, true, 'Frontera límite inferior exacta (Mínimo térmico reglamentario permitido)'],
      [2.1, true, 'Valor justo sobre el límite inferior (Dentro de partición válida)'],
      [5.0, true, 'Valor nominal típico en refrigeración farmacéutica (Partición válida)'],
      [7.9, true, 'Valor justo bajo el límite superior (Dentro de partición válida)'],
      [8.0, true, 'Frontera límite superior exacta (Máximo térmico reglamentario permitido)'],
      [8.1, false, 'Valor límite superior + 0.1 (Fuera de rango por desnaturalización biológica)'],
      [25.0, false, 'Temperatura ambiente (Violación severa de cadena de frío)'],
      [-10.0, false, 'Congelación severa (Violación por temperatura negativa)'],
      [NaN, false, 'Valor corrupto o desconexión del sensor IOT'],
      [null, false, 'Dato ausente de telemetría'],
    ])('Sensor: %s°C', (celsius, expected, rationale) => {
      it(`T=${celsius}°C debe ser ${expected ? 'ACEPTADA' : 'RECHAZADA'} [${rationale}]`, () => {
        // Arrange
        const tempReading = celsius

        // Act
        const isValid = isColdChainValid(tempReading)

        // Assert
        expect(isValid).toBe(Boolean(expected))
      })
    })
  })
})
