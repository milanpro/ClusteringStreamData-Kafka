const proxy = require("http-proxy-middleware");

const ip = "35.224.73.235:31552"

module.exports = function(app) {
  app.use(proxy("/live", { target: "http://"+ ip }));
  app.use(proxy("/setval", { target: "http://"+ ip }));
  app.use(proxy("/getval", { target: "http://"+ ip }));
  app.use(proxy("/live", { target:"ws://"+ ip, ws: true }));
};
