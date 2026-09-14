import request from 'supertest'
import { beforeEach, describe, expect, it } from 'vitest'
import { app, resetOrderStore } from '../../src/api/app.js'

describe('Laboratorio 3: Regresión de API HTTP con Supertest - EntregaSegura', () => {
  beforeEach(() => {
    // Reiniciar el estado de órdenes en memoria antes de cada prueba para garantizar repetibilidad
    resetOrderStore()
  })

  describe('POST /api/auth/login (Autenticación de Mensajero)', () => {
    it('retorna 200 y token de sesión para credenciales válidas de prueba', async () => {
      // Arrange
      const credentials = {
        username: 'courier_test',
        password: 'Password123*',
      }

      // Act
      const response = await request(app)
        .post('/api/auth/login')
        .send(credentials)

      // Assert
      expect(response.status).toBe(200)
      expect(response.body).toHaveProperty('token', 'TEST_COURIER_TOKEN')
      expect(response.body.user).toEqual(
        expect.objectContaining({
          username: 'courier_test',
          role: 'COURIER',
        })
      )
    })

    it('retorna 401 y código INVALID_CREDENTIALS para credenciales erróneas', async () => {
      // Arrange
      const badCredentials = {
        username: 'courier_test',
        password: 'ClaveEquivocada999',
      }

      // Act
      const response = await request(app)
        .post('/api/auth/login')
        .send(badCredentials)

      // Assert
      expect(response.status).toBe(401)
      expect(response.body).toEqual(
        expect.objectContaining({
          code: 'INVALID_CREDENTIALS',
        })
      )
    })
  })

  describe('GET /api/orders/:id (Consulta de Orden Médica)', () => {
    it('retorna 200 y el detalle de la orden cuando el usuario está autenticado', async () => {
      // Arrange & Act
      const response = await request(app)
        .get('/api/orders/ORD-101')
        .set('Authorization', 'Bearer TEST_COURIER_TOKEN')

      // Assert
      expect(response.status).toBe(200)
      expect(response.body).toEqual(
        expect.objectContaining({
          id: 'ORD-101',
          status: 'IN_ROUTE',
          patientId: 'PAT-TEST-01',
        })
      )
    })

    it('retorna 401 UNAUTHORIZED cuando no se envía la cabecera Authorization', async () => {
      // Act
      const response = await request(app).get('/api/orders/ORD-101')

      // Assert
      expect(response.status).toBe(401)
      expect(response.body.code).toBe('UNAUTHORIZED')
    })

    it('retorna 404 ORDER_NOT_FOUND cuando la orden solicitada no existe', async () => {
      // Act
      const response = await request(app)
        .get('/api/orders/ORD-NO-EXISTE')
        .set('Authorization', 'Bearer TEST_COURIER_TOKEN')

      // Assert
      expect(response.status).toBe(404)
      expect(response.body.code).toBe('ORDER_NOT_FOUND')
    })
  })

  describe('PATCH /api/orders/:id/status (Actualización de Estado y Reglas de Transición)', () => {
    it('rechaza con 409 una transición inválida: CANCELLED -> DELIVERED', async () => {
      // Arrange & Act
      const response = await request(app)
        .patch('/api/orders/ORD-CANCELLED-01/status')
        .set('Authorization', 'Bearer TEST_COURIER_TOKEN')
        .send({
          status: 'DELIVERED',
          evidenceUrl: 'https://cdn.entregasegura.test/evidences/foto.jpg',
        })

      // Assert
      expect(response.status).toBe(409)
      expect(response.body).toEqual(
        expect.objectContaining({
          code: 'INVALID_STATUS_TRANSITION',
          currentStatus: 'CANCELLED',
          requestedStatus: 'DELIVERED',
        })
      )
    })

    it('retorna 200 y persiste estado DELIVERED cuando la orden en ruta incluye evidencia válida', async () => {
      // Arrange
      const payload = {
        status: 'DELIVERED',
        evidenceUrl: 'https://cdn.entregasegura.test/evidences/entrega-exitosa.jpg',
        recipientSignature: 'FIRMA_DIGITAL_BASE64',
      }

      // Act
      const response = await request(app)
        .patch('/api/orders/ORD-101/status')
        .set('Authorization', 'Bearer TEST_COURIER_TOKEN')
        .send(payload)

      // Assert
      expect(response.status).toBe(200)
      expect(response.body.order).toEqual(
        expect.objectContaining({
          id: 'ORD-101',
          status: 'DELIVERED',
          evidenceUrl: payload.evidenceUrl,
        })
      )

      // Verificación de persistencia mediante consulta subsiguiente (efecto colateral observable)
      const checkResponse = await request(app)
        .get('/api/orders/ORD-101')
        .set('Authorization', 'Bearer TEST_COURIER_TOKEN')

      expect(checkResponse.status).toBe(200)
      expect(checkResponse.body.status).toBe('DELIVERED')
    })

    it('retorna 400 EVIDENCE_REQUIRED cuando se intenta entregar sin foto de evidencia', async () => {
      // Act
      const response = await request(app)
        .patch('/api/orders/ORD-101/status')
        .set('Authorization', 'Bearer TEST_COURIER_TOKEN')
        .send({ status: 'DELIVERED' })

      // Assert
      expect(response.status).toBe(400)
      expect(response.body.code).toBe('EVIDENCE_REQUIRED')
    })
  })
})
