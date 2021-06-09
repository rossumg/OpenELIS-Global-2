package org.openelisglobal.logging.controller;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoggingController {

    @GetMapping(path = "/logging")
    public void changeLoggingLevel(@RequestParam(name = "logLevel", defaultValue = "E") String logLevel,
            @RequestParam(name = "logger", defaultValue = "org.openelisglobal") String logger,
            @RequestParam(name = "rootLogLevel", defaultValue = "E") String rootLogLevel) {
        org.apache.logging.log4j.Level log4jLogLevel;
        org.apache.logging.log4j.Level rootLog4jLogLevel;
        switch (logLevel) {
        case "A":
            log4jLogLevel = Level.ALL;
            break;
        case "I":
            log4jLogLevel = Level.INFO;
            break;
        case "T":
            log4jLogLevel = Level.TRACE;
            break;
        case "D":
            log4jLogLevel = Level.DEBUG;
            break;
        case "W":
            log4jLogLevel = Level.WARN;
            break;
        case "E":
            log4jLogLevel = Level.ERROR;
            break;
        case "F":
            log4jLogLevel = Level.FATAL;
            break;
        case "O":
            log4jLogLevel = Level.OFF;
            break;
        default:
            log4jLogLevel = Level.ERROR;
            break;
        }
        if ("root".equals(logger)) {
            Configurator.setRootLevel(log4jLogLevel);
        } else {
            Configurator.setLevel(logger, log4jLogLevel);
        }

    }

}
