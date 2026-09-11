import { beforeEach, describe, expect, it, vi } from 'vitest'
import { completeDelivery } from '../../src/services/delivery-service.js'
import { orderFixture } from '../../src/fixtures/order.fixture.js'
import { ORDER_STATUS } from '../../src/domain/order-status.js'

/**
 * Laboratorio 2: Fixtures, Stub y Mock en Servicio de Negocio
 *
 * NOTA METODOLÓGICA (Sustentación de dobles):
 * - repository.findById actúa como STUB: devuelve datos preestablecidos sin consultar base de datos real.
 * - repository.save y notifier.send actúan como MOCKS: además de simular éxito, permiten verificar
 *   mediante aserciones de interacción cuántas veces fueron invocados y con qué argumentos.
 *
 * ¿QUÉ DEPENDENCIA NO CONVENDRÍA MOCKEAR EN UNA PRUEBA DE INTEGRACIÓN POSTERIOR?
 * En una prueba de integración de persistencia (ej. Base de Datos PostgreSQL/Prisma/MongoDB),
 * NO convendría mockear el 'repository'. Mockear la base de datos oculta problemas reales
 * de conectividad, constraints de claves foráneas, serialización de tipos de fecha y sintaxis SQL.
 */

describe('Laboratorio 2: Servicio completeDelivery con Fixtures, Stubs y Mocks', () => {
  let repositoryStub
  let notifierMock

  beforeEach(() => {
    // Restaurar y limpiar todos los dobles de prueba entre ejecuciones para asegurar aislamiento
    vi.clearAllMocks()

    repositoryStub = {
      findById: vi.fn().mockResolvedValue(orderFixture({ status: ORDER_STATUS.IN_ROUTE })),
      save: vi.fn().mockResolvedValue(undefined),
    }

    notifierMock = {
      send: vi.fn().mockResolvedValue(undefined),
    }
  })

  it('completa la entrega exitosamente cuando la orden está en ruta y tiene evidencia válida', async () => {
    // Arrange
    const orderId = 'ORD-101'
    const evidenceUrl = 'https://cdn.entregasegura.test/evidences/ORD-101-firma.jpg'
    const recipientSignature = 'SIG_DATA_BASE64_STUB'

    // Act
    const result = await completeDelivery({
      orderId,
      evidenceUrl,
      recipientSignature,
      repository: repositoryStub,
      notifier: notifierMock,
    })

    // Assert
    expect(result.status).toBe(ORDER_STATUS.DELIVERED)
    expect(result.evidenceUrl).toBe(evidenceUrl)
    expect(result.recipientSignature).toBe(recipientSignature)
    expect(result.deliveredAt).toBeDefined()

    // Verificación de interacción con el Mock de persistencia
    expect(repositoryStub.save).toHaveBeenCalledOnce()
    expect(repositoryStub.save).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 'ORD-101',
        status: ORDER_STATUS.DELIVERED,
      })
    )

    // Verificación de interacción con el Mock de notificación
    expect(notifierMock.send).toHaveBeenCalledOnce()
    expect(notifierMock.send).toHaveBeenCalledWith(
      expect.objectContaining({
        orderId: 'ORD-101',
        status: ORDER_STATUS.DELIVERED,
      })
    )
  })

  it('falla con código EVIDENCE_REQUIRED cuando no se adjunta evidencia fotográfica', async () => {
    // Arrange
    const orderId = 'ORD-101'
    const missingEvidence = null

    // Act & Assert
    await expect(
      completeDelivery({
        orderId,
        evidenceUrl: missingEvidence,
        repository: repositoryStub,
        notifier: notifierMock,
      })
    ).rejects.toMatchObject({
      code: 'EVIDENCE_REQUIRED',
    })

    // Asegurar que no hubo efectos secundarios (Aislamiento de estado)
    expect(repositoryStub.save).not.toHaveBeenCalled()
    expect(notifierMock.send).not.toHaveBeenCalled()
  })

  it('falla con INVALID_STATUS_TRANSITION si la orden se encuentra en estado CANCELLED', async () => {
    // Arrange
    // Stub devolviendo una orden cancelada
    repositoryStub.findById.mockResolvedValueOnce(
      orderFixture({
        id: 'ORD-CANCELLED-99',
        status: ORDER_STATUS.CANCELLED,
      })
    )

    // Act & Assert
    await expect(
      completeDelivery({
        orderId: 'ORD-CANCELLED-99',
        evidenceUrl: 'https://cdn.entregasegura.test/evidences/foto.png',
        repository: repositoryStub,
        notifier: notifierMock,
      })
    ).rejects.toMatchObject({
      code: 'INVALID_STATUS_TRANSITION',
    })

    expect(repositoryStub.save).not.toHaveBeenCalled()
    expect(notifierMock.send).not.toHaveBeenCalled()
  })

  it('falla con ORDER_NOT_FOUND si la orden no existe en el repositorio', async () => {
    // Arrange
    // Stub simulando que la orden no fue encontrada
    repositoryStub.findById.mockResolvedValueOnce(null)

    // Act & Assert
    await expect(
      completeDelivery({
        orderId: 'ORD-INEXISTENTE',
        evidenceUrl: 'https://cdn.entregasegura.test/evidences/foto.jpg',
        repository: repositoryStub,
        notifier: notifierMock,
      })
    ).rejects.toMatchObject({
      code: 'ORDER_NOT_FOUND',
    })

    expect(repositoryStub.save).not.toHaveBeenCalled()
    expect(notifierMock.send).not.toHaveBeenCalled()
  })
})
