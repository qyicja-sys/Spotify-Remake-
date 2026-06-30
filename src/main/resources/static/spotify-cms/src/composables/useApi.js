import { ref } from 'vue'

const API_BASE = '/admin/spotify'

export function useApi() {
  const loading = ref(false)
  const error = ref(null)

  async function apiFetch(url, options = {}) {
    const token = localStorage.getItem('adminToken')
    if (!token) {
      localStorage.removeItem('adminToken')
      window.dispatchEvent(new Event('auth:logout'))
      return null
    }

    loading.value = true
    error.value = null

    try {
      const response = await fetch(API_BASE + url, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          'token': token,
          ...options.headers
        }
      })

      if (response.status === 401) {
        localStorage.removeItem('adminToken')
        window.dispatchEvent(new Event('auth:logout'))
        return null
      }

      const result = await response.json()
      return result
    } catch (e) {
      error.value = 'Network error'
      return null
    } finally {
      loading.value = false
    }
  }

  async function login(username, password) {
    loading.value = true
    error.value = null
    try {
      const response = await fetch(API_BASE + '/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      })
      const result = await response.json()
      if (result.code === 200) {
        localStorage.setItem('adminToken', result.data.token)
        return result.data
      }
      error.value = result.msg || 'Login failed'
      return null
    } catch (e) {
      error.value = 'Network error'
      return null
    } finally {
      loading.value = false
    }
  }

  function logout() {
    localStorage.removeItem('adminToken')
    window.dispatchEvent(new Event('auth:logout'))
  }

  return { apiFetch, login, logout, loading, error }
}
