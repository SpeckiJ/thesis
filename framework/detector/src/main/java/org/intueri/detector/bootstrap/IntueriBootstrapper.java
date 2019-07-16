/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector.bootstrap;

import io.debezium.connector.postgresql.connection.PostgresConnection;
import org.intueri.detector.IntueriDatabaseConnector;
import org.intueri.detector.IntueriDetectorMessageHandler;
import org.intueri.exception.IntueriException;
import org.intueri.util.DetectorStatus;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

@Component
public class IntueriBootstrapper {

    private final Logger logger = LoggerFactory.getLogger(IntueriBootstrapper.class);

    @Autowired
    private SchemaGenerator generator;

    @Autowired
    private IntueriDetectorMessageHandler messageHandler;

    @Autowired
    private IntueriDatabaseConnector databaseConnector;

    /**
     * Bootstraps the Intueri detector application. Generates schemata and uploads them to Intueri orchestrators.
     */
    public void bootstrap(JSONObject detectorConfig) throws IntueriException {
        logger.info("Starting Bootstrapping");
        Set<IntueriDatabaseTable> databaseTables = new HashSet<>();
        try (PostgresConnection connection = new PostgresConnection(
                databaseConnector.generateConfig(detectorConfig).subset("database.", true))) {
            messageHandler.updateStatus(DetectorStatus.UPDATING_SCHEMA);
            DatabaseMetaData metaData = connection.connection().getMetaData();
            ResultSet tables = metaData.getTables(null, null, null, new String[]{"TABLE"});
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                //TODO(specki): Account for table blacklisting
                logger.trace("Parsing " + tableName);
                Set<String> comparableColumns = new HashSet<>();
                ResultSet columns = metaData.getColumns(null, null, tableName, null);
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    switch (columns.getInt("DATA_TYPE")) {
                        case Types.DOUBLE:
                        case Types.FLOAT:
                        case Types.INTEGER:
                        case Types.NUMERIC:
                            comparableColumns.add(columnName);
                            break;
                        default:
                            break;
                    }
                }
                databaseTables.add(new IntueriDatabaseTable(tableName, comparableColumns, null));
            }
        } catch (Exception e) {
            logger.error("Could not connect to database: {} {}", e.getMessage(), e.getCause().getMessage());
            throw new IntueriException(e.getMessage());
        }

        JSONObject schema;
        try {
            schema = generator.createSchema(databaseTables);
        } catch (IOException e) {
            logger.error("Could not generate schema: {}", e.getMessage());
            throw new IntueriException(e.getMessage());
        }
        messageHandler.updateSchema(schema);
    }
}
