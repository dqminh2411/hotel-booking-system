/**
 * Cac shape du lieu dung chung, khong gan voi 1 feature cu the.
 * File nay khong export gia tri runtime nao - chi de lai JSDoc lam tai
 * lieu tham khao ve cau truc response chung cua backend (Spring Boot).
 *
 * @typedef {Object} PageResponse
 * @property {Array} content
 * @property {number} page
 * @property {number} size
 * @property {number} totalElements
 * @property {number} totalPages
 */

/**
 * Khop voi ErrorResponse cua backend (GlobalExceptionHandler).
 * @typedef {Object} ApiErrorResponse
 * @property {string} timestamp
 * @property {number} status
 * @property {string} error
 * @property {string} message
 * @property {string} path
 */

/**
 * Loi da duoc chuan hoa de cac component/hook su dung thong nhat,
 * khong phu thuoc truc tiep vao Axios.
 * @typedef {Object} AppError
 * @property {number|null} status
 * @property {string} message
 * @property {string} [path]
 */
