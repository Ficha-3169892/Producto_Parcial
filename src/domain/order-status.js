/**
 * Dominio de EntregaSegura: Gestión de Estados y Reglas de Medicamentos
 */

export const ORDER_STATUS = Object.freeze({
  CREATED: 'CREATED',
  ASSIGNED: 'ASSIGNED',
  IN_ROUTE: 'IN_ROUTE',
  DELIVERED: 'DELIVERED',
  CANCELLED: 'CANCELLED',
})

/**
 * Matriz de transiciones permitidas según el ciclo de vida de la orden de medicamentos.
 */
const VALID_TRANSITIONS = {
  [ORDER_STATUS.CREATED]: [ORDER_STATUS.ASSIGNED, ORDER_STATUS.CANCELLED],
  [ORDER_STATUS.ASSIGNED]: [ORDER_STATUS.IN_ROUTE, ORDER_STATUS.CANCELLED],
  [ORDER_STATUS.IN_ROUTE]: [ORDER_STATUS.DELIVERED, ORDER_STATUS.CANCELLED],
  [ORDER_STATUS.DELIVERED]: [], // Estado final
  [ORDER_STATUS.CANCELLED]: [], // Estado final
}

/**
 * Evalúa si una transición de estado es válida según las reglas de negocio.
 * @param {string} from - Estado actual
 * @param {string} to - Estado destino deseado
 * @returns {boolean}
 */
export function canTransition(from, to) {
  if (!from || !to) return false
  const allowed = VALID_TRANSITIONS[from]
  if (!allowed) return false
  return allowed.includes(to)
}

/**
 * Rango normativo para medicamentos con cadena de frío: 2.0°C a 8.0°C inclusive.
 * Aplicación de análisis de valores límite (BVA) y partición de equivalencia.
 * @param {number} celsius - Temperatura registrada en el sensor del contenedor térmico
 * @returns {boolean}
 */
export function isColdChainValid(celsius) {
  if (typeof celsius !== 'number' || Number.isNaN(celsius)) {
    return false
  }
  return celsius >= 2.0 && celsius <= 8.0
}

/**
 * Excepción de dominio para reglas de negocio incumplidas.
 */
export class DomainError extends Error {
  constructor(code, message) {
    super(message || code)
    this.name = 'DomainError'
    this.code = code
  }
}
