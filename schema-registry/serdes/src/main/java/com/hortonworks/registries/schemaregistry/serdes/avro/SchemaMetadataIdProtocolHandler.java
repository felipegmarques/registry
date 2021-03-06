/*
 * Copyright 2016-2019 Cloudera, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.registries.schemaregistry.serdes.avro;

import com.hortonworks.registries.schemaregistry.SchemaIdVersion;
import com.hortonworks.registries.schemaregistry.serde.SerDesException;
import com.hortonworks.registries.schemaregistry.serdes.avro.exceptions.AvroRetryableException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 *
 */
public class SchemaMetadataIdProtocolHandler extends AbstractAvroSerDesProtocolHandler {

    public SchemaMetadataIdProtocolHandler() {
        super(SerDesProtocolHandlerRegistry.METADATA_ID_VERSION_PROTOCOL, new DefaultAvroSerDesHandler());
    }

    @Override
    protected void doHandleSchemaVersionSerialization(OutputStream outputStream,
                                                      SchemaIdVersion schemaIdVersion) throws SerDesException {
        // 8 bytes : schema metadata Id
        // 4 bytes : schema version
        try {
            outputStream.write(ByteBuffer.allocate(12)
                                         .putLong(schemaIdVersion.getSchemaMetadataId())
                                         .putInt(schemaIdVersion.getVersion()).array());
        } catch (IOException e) {
            throw new AvroRetryableException(e);
        }
    }

    @Override
    public SchemaIdVersion handleSchemaVersionDeserialization(InputStream inputStream) {
        // 8 bytes : schema metadata Id
        // 4 bytes : schema version
        ByteBuffer byteBuffer = ByteBuffer.allocate(12);
        try {
            inputStream.read(byteBuffer.array());
        } catch (IOException e) {
            throw new AvroRetryableException(e);
        }

        long schemaMetadataId = byteBuffer.getLong();
        int schemaVersion = byteBuffer.getInt();

        return new SchemaIdVersion(schemaMetadataId, schemaVersion);
    }

}
