/**
 * Proxy config for redirecting relative /api calls to the backend running on port 8080
 *
 * @see https://github.com/chimurai/http-proxy-middleware#options
 */
module.exports = {
  "/ui-api": {
    target: "http://localhost:8400",
    changeOrigin: true,
    secure: false,
    logLevel: "debug",
  },
}; // test
