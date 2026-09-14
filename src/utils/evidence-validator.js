/**
 * Validador de evidencia fotográfica de entrega (Microciclo TDD).
 * Regla de negocio: Toda evidencia de entrega médica debe ser una URL segura (HTTPS)
 * y apuntar a un formato de imagen autorizado (.jpg, .jpeg, .png, .webp).
 */

const SECURE_IMAGE_URL_REGEX = /^https:\/\/[a-zA-Z0-9-._~:/?#[\]@!$&'()*+,;=]+\.(jpg|jpeg|png|webp)(\?.*)?$/i

/**
 * Valida si la URL de la evidencia de entrega cumple con los estándares de seguridad y formato.
 * @param {string} url - URL de la imagen de evidencia subida por el mensajero
 * @returns {boolean}
 */
export function isValidEvidenceUrl(url) {
  if (typeof url !== 'string' || url.trim() === '') {
    return false
  }

  return SECURE_IMAGE_URL_REGEX.test(url.trim())
}
