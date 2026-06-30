function isLocalHost(hostname) {
  return hostname === 'localhost' || hostname === '127.0.0.1'
}

function isProxyMode() {
  const { hostname, port } = window.location

  if (!isLocalHost(hostname)) {
    return false
  }

  if (port === '90') {
    return true
  }

  return import.meta.env.DEV && port === '5173'
}

function getApiBase() {
  const envBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()
  if (envBaseUrl) {
    return envBaseUrl.replace(/\/$/, '')
  }

  if (isProxyMode()) {
    return '/api'
  }

  const { protocol, hostname, port } = window.location
  if (isLocalHost(hostname) && port === '8080') {
    return ''
  }

  if (protocol.startsWith('http') && port === '8080') {
    return ''
  }

  return 'http://localhost:8080'
}

function withBase(path) {
  return `${getApiBase()}${path}`
}

function createError(message, responseData, status) {
  const error = new Error(message)
  error.response = {
    data: responseData,
    status,
  }
  return error
}

async function request(method, path, payload) {
  const url = withBase(path)
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  }

  console.log(`[API Request] ${method} ${url}`, payload)
  console.log(`[API Request] Headers:`, options.headers)

  if (payload != null) {
    options.body = JSON.stringify(payload)
  }

  let response
  try {
    response = await fetch(url, options)
    console.log(`[API Response] Status: ${response.status}, OK: ${response.ok}`)
    console.log(`[API Response] Headers:`, Object.fromEntries(response.headers.entries()))
  } catch (error) {
    console.error(`[API Error] Network error for ${url}:`, error)
    throw createError('Network error, please try again', null, 0)
  }

  const text = await response.text()
  console.log(`[API Response] Raw text:`, text)
  let data = {}

  if (text) {
    try {
      data = JSON.parse(text)
      console.log(`[API Response] Parsed data:`, data)
    } catch {
      console.error(`[API Error] Failed to parse JSON from:`, text)
      throw createError(`Unexpected response from ${url}`, { message: text }, response.status)
    }
  }

  if (!response.ok) {
    const message = data?.message || data?.msg || `Request failed: ${response.status}`
    console.error(`[API Error] Request failed:`, { status: response.status, message, data })
    throw createError(message, data, response.status)
  }

  console.log(`[API Success] Returning:`, { data, status: response.status })
  return {
    data,
    status: response.status,
  }
}

export function login(payload) {
  return request('POST', '/spotify/login', payload)
}

export function signUp(payload) {
  return request('POST', '/spotify/signup', payload)
}

export function resetPassword(payload) {
  return request('POST', '/spotify/login/forgetPassword', payload)
}

export function getCaptcha() {
  return request('GET', '/captcha/get')
}

export function checkCaptcha(payload) {
  return request('POST', '/captcha/check', payload)
}