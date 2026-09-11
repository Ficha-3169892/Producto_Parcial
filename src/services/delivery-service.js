import { canTransition, ORDER_STATUS, DomainError } from '../domain/order-status.js'
import { isValidEvidenceUrl } from '../utils/evidence-validator.js'

/**
 * Servicio de negocio para completar la entrega de medicamentos.
 * Coordina la verificación de reglas de negocio, persistencia en repositorio y notificaciones.
 *
 * @param {Object} params
 * @param {string} params.orderId - Identificador único de la orden
 * @param {string} params.evidenceUrl - URL de la fotografía o soporte de entrega
 * @param {string} [params.recipientSignature] - Firma digital opcional del receptor
 * @param {Object} params.repository - Puerto/interfaz de repositorio de órdenes
 * @param {Object} params.notifier - Puerto/interfaz de servicio de mensajería/notificación
 * @returns {Promise<Object>} Orden actualizada con estado DELIVERED
 */
export async function completeDelivery({
  orderId,
  evidenceUrl,
  recipientSignature = null,
  repository,
  notifier,
}) {
  if (!orderId) {
    throw new DomainError('ORDER_ID_REQUIRED', 'El ID de la orden es obligatorio.')
  }

  // Regla TDD: Exigencia y validación de evidencia fotográfica
  if (!evidenceUrl) {
    throw new DomainError('EVIDENCE_REQUIRED', 'Se requiere evidencia fotográfica para confirmar la entrega.')
  }

  if (!isValidEvidenceUrl(evidenceUrl)) {
    throw new DomainError('INVALID_EVIDENCE_FORMAT', 'La URL de evidencia debe ser HTTPS y apuntar a una imagen válida (.jpg, .jpeg, .png, .webp).')
  }

  const order = await repository.findById(orderId)
  if (!order) {
    throw new DomainError('ORDER_NOT_FOUND', `No se encontró la orden con ID: ${orderId}`)
  }

  // Validación de transición de estado
  if (!canTransition(order.status, ORDER_STATUS.DELIVERED)) {
    throw new DomainError(
      'INVALID_STATUS_TRANSITION',
      `Transición de estado no permitida: de ${order.status} a ${ORDER_STATUS.DELIVERED}.`
    )
  }

  // Mutación controlada de la entidad
  const updatedOrder = {
    ...order,
    status: ORDER_STATUS.DELIVERED,
    evidenceUrl,
    recipientSignature,
    deliveredAt: new Date().toISOString(),
  }

  // Efectos secundarios
  if (repository && typeof repository.save === 'function') {
    await repository.save(updatedOrder)
  }

  if (notifier && typeof notifier.send === 'function') {
    await notifier.send({
      orderId: updatedOrder.id,
      patientId: updatedOrder.patientId,
      status: updatedOrder.status,
      message: 'Su medicamento ha sido entregado exitosamente y la cadena de frío fue preservada.',
    })
  }

  return updatedOrder
}
