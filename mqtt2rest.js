/* mqtt2rest.js
Thanks to  smartthings-mqtt-bridge authors
 - st.john.johnson@gmail.com
 - jeremiah.wuenschel@gmail.com
 for the base code used here
*/
'use strict';
// for reading config file

const yaml = require('js-yaml'),
    path = require('path'),
    joi = require('joi'),
    expressJoi = require('./expressJoiValidator'),
    winston = require('winston'),
    bodyparser = require('body-parser'),
    expressWinston = require('express-winston'),
    fs = require('fs'),
    request = require('request'),
    mqtt = require('mqtt'),
    express = require('express');

const CONFIG_DIR = process.env.CONFIG_DIR || process.cwd(),
    CONFIG_FILE = path.join(CONFIG_DIR, 'config.yml'),
    garadgetCommands = ['open', 'close', 'stop', 'get-config', 'get-status','set-config'];

// load config
let config = {};
try {
    config = yaml.safeLoad(fs.readFileSync(CONFIG_FILE));
} catch (e) {
    console.log("Configuration reading error :", e);
    process.exit(1);
}

// global vars used throughout
let garageDoors = {};
let callback = null;

// logging setup
const tsFormat = () => (new Date()).toLocaleTimeString();
const logger = new (winston.Logger)({
    transports: [
        // colorize the output to the console
        new (winston.transports.Console)({
          timestamp: tsFormat,
          colorize: true,
          level: config.application.logLevel
        })
    ]
});

/* mqtt init */
const mqtt_client  = mqtt.connect(config.mqtt);
mqtt_client.on('connect', function () {
    let topicArray = [];
    if ("doors" in config.mqtt) {
        config.mqtt.doors.forEach(function(item) {
            // listening only to status and config reports
            topicArray.push([config.mqtt.prefix,item,"status"].join("/"), [config.mqtt.prefix,item,"config"].join("/"));
        });
    } else {
        // just listen to all available
        topicArray.push([config.mqtt.prefix,'+',"status"].join("/"), [config.mqtt.prefix,'+',"config"].join("/"));
    }
    logger.debug(topicArray);
    mqtt_client.subscribe(topicArray);
});

// forward MQTT packets to REST
mqtt_client.on('message', function (topic, message) {
    logger.debug("MQTT Received Packet from ",topic.toString()," : ",message.toString());
    let pieces = topic.split('/'),
        device = pieces[1],
        property = pieces[2];
    // keep object holding door names
    garageDoors[device] = device;
    logger.info("Pushing to:",callback," : ",device," : ",property," : ",message.toString());
    if (callback != null) {
        try {
            request.post({
                url: callback,
                json: {
                    name: device,
                    type: property,
                    value: message.toString()
                },
                localAddress: config.http.host,
                time: true,
                forever: true
            }, function (error, response, body) {
                if (error) {
                    logger.error('Error from the request layer: %s', error.toString());
                    logger.error(JSON.stringify(error, null, 4));
                    logger.error(JSON.stringify(response, null, 4));
                } else {
                    logger.debug("Response:", response.statusCode);
                    logger.debug("Body:", body);
                }
            });
        } catch (e) {
            logger.error('Error from the request layer: %s', e.message);
            // do nothing and hope for recovery
        }
    }
});

// REST server for processing SmartThings requests
const app = express();
app.use(bodyparser.json());

// Log all requests to console
app.use(expressWinston.logger({
    transports: [
        new winston.transports.Console({
            timestamp: tsFormat,
            json: false,
            colorize: true,
            level: config.application.logLevelHttp

        })
    ]
}));

app.post('/gmqtt/doors',
    expressJoi({
        body: {
            callback: joi.string().required()
        }
    }),function (req, res) {
    // try sending callback info in every command to avoid need for separate subscription call
        callback = req.body.callback;
        logger.debug("Received doors command; will call back to:",callback);
        res.set('Content-Type', 'application/json');
        res.json({ doors: Object.keys(garageDoors), type: "doors"})
        res.end();
});

app.post('/gmqtt/command',
    expressJoi({
        body: {
            name: joi.string().required(),
            command: joi.string().required(),
            parameter: joi.string().optional(),
            callback: joi.string().optional()
        }
    }),function (req, res) {
        logger.debug("Request :", req.body)
        // try sending callback info in every command to avoid need for separate subscription call
        callback = req.body.callback ? req.body.callback : callback;
        // ignore unconfigured door (if door names are explicitly configured)  or unsupported command
        if (garadgetCommands.includes(req.body.command) && (!("doors" in config.mqtt) || config.mqtt.doors.includes(req.body.name))) {
            logger.debug("Received command for:", req.body.name, ";value:", req.body.command, ";will call back to:",callback);
            logger.info("Calling mqtt publish:", [config.mqtt.prefix, req.body.name, 'command'].join('/'), " with value:", req.body.command);
            mqtt_client.publish([config.mqtt.prefix, req.body.name, 'command'].join('/'), req.body.command);
        } else {
            logger.error("Unsupported combination. Received command for:", req.body.name, ";value:", req.body.command);
            res.status(404);
        }
        res.end();
});

app.post('/gmqtt/set-config',
    expressJoi({
        body: {
            name: joi.string().required(),
            value: joi.object().required(),
            callback: joi.string().optional()
        }
    }),function (req, res) {
        logger.debug("Request :", req.body)
        callback = req.body.callback ? req.body.callback : callback;
        // ignore unconfigured door
        if (config.mqtt.doors.includes(req.body.name)) {
            logger.debug("Received set-config for:", req.body.name, ";value:", JSON.stringify(req.body.value))
            logger.info("Calling mqtt publish:", [config.mqtt.prefix, req.body.name, 'set-config'].join('/'), " with value:", req.body.value);
            mqtt_client.publish([config.mqtt.prefix, req.body.name, 'set-config'].join('/'), JSON.stringify(req.body.value));
        } else {
            logger.error("Unsupported combination. Received command for:", req.body.name, ";value:", req.body.command);
            res.status(404);
        }
        res.end();
});

// REST server init
const server = app.listen(config.http.port, config.http.host, function () {
    logger.info("Garadget mqtt2rest listening at http://%s:%s", server.address().address, server.address().port)
});

server.on('error', (e) => {
    logger.error("Server instantiation failure : %s", e);
    setTimeout(() => {
      server.close();
      process.exit(1);
      // let the pm2 deal with it
    }, 5000);
});