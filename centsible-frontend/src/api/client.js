import axios from 'axios'

export const AUTH_TOKEN_KEY = 'centsible_token'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
})

// Attach Bearer token to all outgoing requests if present
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(AUTH_TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Intercept 401 responses and notify auth state
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      localStorage.removeItem(AUTH_TOKEN_KEY)
      window.dispatchEvent(new CustomEvent('centsible:unauthorized'))
    }
    return Promise.reject(error)
  }
)

export default api
