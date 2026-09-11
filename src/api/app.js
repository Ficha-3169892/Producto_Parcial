import express from 'express'
import { canTransition, ORDER_STATUS, DomainError } from '../domain/order-status.js'
import { completeDelivery } from '../services/delivery-service.js'
import { orderFixture } from '../fixtures/order.fixture.js'

/**
 * Generador de estado inicial en memoria para pruebas reproducibles.
 */
export function createInitialOrders() {
  return new Map([
    [
      'ORD-101',
      orderFixture({
        id: 'ORD-101',
        status: ORDER_STATUS.IN_ROUTE,
      }),
    ],
    [
      'ORD-CANCELLED-01',
      orderFixture({
        id: 'ORD-CANCELLED-01',
        status: ORDER_STATUS.CANCELLED,
      }),
    ],
    [
      'ORD-DELIVERED-01',
      orderFixture({
        id: 'ORD-DELIVERED-01',
        status: ORDER_STATUS.DELIVERED,
        evidenceUrl: 'https://cdn.entregasegura.test/evidence/ord-delivered-01.jpg',
      }),
    ],
  ])
}

// Almacén en memoria compartido para la app por defecto
let orderStore = createInitialOrders()

/**
 * Función para reiniciar los datos de prueba entre ejecuciones (evita estado compartido).
 */
export function resetOrderStore(customStore = null) {
  orderStore = customStore || createInitialOrders()
}

/**
 * Middleware de autenticación de pruebas.
 * Valida el token Bearer controlado sin exponer credenciales reales.
 */
function authMiddleware(req, res, next) {
  const authHeader = req.headers['authorization']
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      code: 'UNAUTHORIZED',
      message: 'Cabecera Authorization no suministrada o formato inválido',
    })
  }

  const token = authHeader.split(' ')[1]
  if (token !== 'TEST_COURIER_TOKEN' && token !== 'VALID_ADMIN_TOKEN') {
    return res.status(401).json({
      code: 'INVALID_TOKEN',
      message: 'Token de acceso no válido o expirado',
    })
  }

  req.user = {
    id: 'USR-COURIER-01',
    role: 'COURIER',
    name: 'Carlos Mensajero Test',
  }
  next()
}

export const app = express()
app.use(express.json())

/**
 * POST /api/auth/login
 * Autenticación controlada para pruebas de regresión.
 */
app.post('/api/auth/login', (req, res) => {
  const { username, password } = req.body || {}

  if (username === 'courier_test' && password === 'Password123*') {
    return res.status(200).json({
      token: 'TEST_COURIER_TOKEN',
      user: {
        id: 'USR-COURIER-01',
        username: 'courier_test',
        role: 'COURIER',
      },
    })
  }

  return res.status(401).json({
    code: 'INVALID_CREDENTIALS',
    message: 'Usuario o contraseña incorrectos',
  })
})

/**
 * GET /api/orders/:id
 * Consulta de orden por identificador único.
 */
app.get('/api/orders/:id', authMiddleware, (req, res) => {
  const { id } = req.params
  const order = orderStore.get(id)

  if (!order) {
    return res.status(404).json({
      code: 'ORDER_NOT_FOUND',
      message: `La orden con ID ${id} no existe en el sistema`,
    })
  }

  return res.status(200).json(order)
})

/**
 * PATCH /api/orders/:id/status
 * Actualización de estado de la orden con validación estricta de transiciones.
 */
app.patch('/api/orders/:id/status', authMiddleware, async (req, res) => {
  const { id } = req.params
  const { status: nextStatus, evidenceUrl, recipientSignature } = req.body || {}

  const order = orderStore.get(id)
  if (!order) {
    return res.status(404).json({
      code: 'ORDER_NOT_FOUND',
      message: `La orden con ID ${id} no fue encontrada`,
    })
  }

  // Validación de transición de negocio
  if (!canTransition(order.status, nextStatus)) {
    return res.status(409).json({
      code: 'INVALID_STATUS_TRANSITION',
      message: `No es posible realizar la transición de ${order.status} a ${nextStatus}`,
      currentStatus: order.status,
      requestedStatus: nextStatus,
    })
  }

  // Si la transición es a DELIVERED, coordinamos con el servicio de entrega y TDD
  if (nextStatus === ORDER_STATUS.DELIVERED) {
    try {
      const mockRepo = {
        findById: async () => order,
        save: async (savedOrder) => {
          orderStore.set(id, savedOrder)
        },
      }
      const mockNotifier = {
        send: async () => {},
      }

      const updated = await completeDelivery({
        orderId: id,
        evidenceUrl,
        recipientSignature,
        repository: mockRepo,
        notifier: mockNotifier,
      })

      return res.status(200).json({
        message: 'Orden entregada satisfactoriamente',
        order: updated,
      })
    } catch (error) {
      if (error instanceof DomainError) {
        return res.status(400).json({
          code: error.code,
          message: error.message,
        })
      }
      return res.status(500).json({ code: 'INTERNAL_ERROR', message: error.message })
    }
  }

  // Para otras transiciones válidas (ej. CREATED -> ASSIGNED o ASSIGNED -> IN_ROUTE)
  const updatedOrder = {
    ...order,
    status: nextStatus,
    updatedAt: new Date().toISOString(),
  }
  orderStore.set(id, updatedOrder)

  return res.status(200).json({
    message: 'Estado de la orden actualizado exitosamente',
    order: updatedOrder,
  })
})
