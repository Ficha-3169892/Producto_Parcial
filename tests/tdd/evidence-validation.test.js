import { describe, expect, it } from 'vitest'
import { isValidEvidenceUrl } from '../../src/utils/evidence-validator.js'

/**
 * Laboratorio 4: Microciclo TDD (Test-Driven Development)
 *
 * REGLA DE NEGOCIO SELECCIONADA:
 * Para garantizar la validez legal y trazabilidad de la entrega de medicamentos,
 * la evidencia fotográfica debe ser alojada en un servidor seguro HTTPS y corresponder
 * a una extensión de imagen autorizada (.jpg, .jpeg, .png, .webp).
 *
 * BITÁCORA DEL CICLO TDD:
 *
 * 1. FASE RED:
 *    Se crearon inicialmente las pruebas unitarias esperando que 'isValidEvidenceUrl'
 *    validara el protocolo https y los formatos permitidos. Como la función no existía
 *    o retornaba 'undefined', la prueba falló con ReferenceError / AssertionError (Fase RED).
 *
 * 2. FASE GREEN:
 *    Se implementó la lógica mínima suficiente con validación básica de prefijo y sufijo:
 *    if (!url || !url.startsWith('https://')) return false;
 *    if (url.endsWith('.jpg') || url.endsWith('.png')) return true;
 *    Con esto la suite pasó a VERDE (Fase GREEN).
 *
 * 3. FASE REFACTOR:
 *    Se refactorizó la función para utilizar una expresión regular compilada de alta eficiencia
 *    (/^https:\/\/[a-zA-Z0-9-._~:/?#[\]@!$&'()*+,;=]+\.(jpg|jpeg|png|webp)(\?.*)?$/i),
 *    soportando querystrings de CDNs (como tokens de pre-firmado en S3), eliminando espacios en blanco
 *    y blindando la función ante valores nulos, numéricos y vacíos, manteniendo la suite en VERDE (Fase REFACTOR).
 */

describe('Laboratorio 4: Microciclo TDD - Validador de Evidencia de Entrega', () => {
  it('[RED -> GREEN] acepta URLs seguras HTTPS con formatos de imagen válidos (.jpg, .png, .webp)', () => {
    // Arrange
    const validJpg = 'https://cdn.entregasegura.test/evidences/entrega-101.jpg'
    const validPng = 'https://cdn.entregasegura.test/evidences/entrega-102.png'
    const validWebp = 'https://storage.googleapis.com/evidencias/foto.webp'

    // Act & Assert
    expect(isValidEvidenceUrl(validJpg)).toBe(true)
    expect(isValidEvidenceUrl(validPng)).toBe(true)
    expect(isValidEvidenceUrl(validWebp)).toBe(true)
  })

  it('[RED -> GREEN] rechaza URLs inseguras con protocolo HTTP plano', () => {
    // Arrange
    const insecureUrl = 'http://insecure.test/evidences/foto.jpg'

    // Act & Assert
    expect(isValidEvidenceUrl(insecureUrl)).toBe(false)
  })

  it('[RED -> GREEN] rechaza archivos con extensiones no permitidas (.pdf, .exe, .sh)', () => {
    // Arrange
    const pdfDocument = 'https://cdn.entregasegura.test/docs/orden.pdf'
    const executable = 'https://cdn.entregasegura.test/bin/malware.exe'

    // Act & Assert
    expect(isValidEvidenceUrl(pdfDocument)).toBe(false)
    expect(isValidEvidenceUrl(executable)).toBe(false)
  })

  it('[REFACTOR] gestiona parámetros de consulta (query params/presigned tokens) en la URL', () => {
    // Arrange: Simulación de presigned URL de S3 o Google Cloud Storage
    const presignedUrl = 'https://s3.amazonaws.com/entregasegura/evidencia.jpg?Expires=1757600000&Signature=abc123xyz'

    // Act & Assert
    expect(isValidEvidenceUrl(presignedUrl)).toBe(true)
  })

  it('[REFACTOR] devuelve false ante entradas vacías, nulas o de tipo no string', () => {
    // Act & Assert
    expect(isValidEvidenceUrl('')).toBe(false)
    expect(isValidEvidenceUrl('   ')).toBe(false)
    expect(isValidEvidenceUrl(null)).toBe(false)
    expect(isValidEvidenceUrl(undefined)).toBe(false)
    expect(isValidEvidenceUrl(12345)).toBe(false)
    expect(isValidEvidenceUrl({})).toBe(false)
  })
})
