const proxy = require("http-proxy-middleware");

module.exports = function(app) {
  app.use(proxy("/live", { target: "http://localhost:5656" }));
  app.use(proxy("/setval", { target: "http://localhost:5656" }));
  app.use(proxy("/live", { target: "ws://localhost:5656", ws: true }));
};
