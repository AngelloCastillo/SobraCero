// La URL del backend se toma de public/config.js (editable sin recompilar)
declare global {
  interface Window { SOBRACERO_API_URL?: string; }
}
export const environment = {
  apiUrl: (window.SOBRACERO_API_URL ?? 'http://localhost:8080').replace(/\/+$/, ''),
};
