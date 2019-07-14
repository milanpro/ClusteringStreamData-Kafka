const proxy = require("http-proxy-middleware");

module.exports = function(app) {
  app.use(proxy("/live", { target: "http://35.224.73.235:30136" }));
  app.use(proxy("/live", { target: "ws://35.224.73.235:30136", ws: true }));
};
