/**
 * Fixture sintético para órdenes de EntregaSegura.
 * Genera datos de prueba aislados y controlados sin comprometer datos reales de pacientes.
 */

export function orderFixture(overrides = {}) {
  return {
    id: 'ORD-101',
    patientId: 'PAT-TEST-01',
    patientName: 'Paciente Sintético Demo',
    courierId: 'USR-COURIER-01',
    status: 'IN_ROUTE',
    medications: [
      { id: 'MED-01', name: 'Insulina NPH 100 UI/ml', requiresColdChain: true }
    ],
    deliveryAddress: 'Calle 50 # 10-20, Bogotá D.C.',
    evidenceUrl: null,
    recipientSignature: null,
    temperatureLogs: [4.5],
    createdAt: '2026-09-11T10:00:00.000Z',
    updatedAt: '2026-09-11T10:30:00.000Z',
    ...overrides,
  }
}
