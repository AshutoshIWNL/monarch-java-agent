package com.asm.mja.config;

import com.asm.mja.logging.AgentLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class ConfigParser {

    public static Config parse(String configFile) {
        AgentLogger.debug("Parsing config file - " + configFile);
        Config config = null;
        ObjectMapper om = new ObjectMapper();
        try {
            File configFileObj = new File(configFile);
            config = om.readValue(configFileObj, Config.class);
        } catch (IOException e) {
            AgentLogger.error(e.getMessage());
            AgentLogger.dumpException(e);
            throw new RuntimeException("Config file parsing failed");
        }
        AgentLogger.debug("Config file parsed and config object built");
        AgentLogger.debug(config.toString());
        return config;
    }
}
