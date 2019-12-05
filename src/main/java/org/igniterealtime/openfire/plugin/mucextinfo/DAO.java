/*
 * Copyright (C) 2019 Ignite Realtime Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.igniterealtime.openfire.plugin.mucextinfo;

import org.jivesoftware.util.CacheableOptional;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object, responsible for storing and retrieving data in persistent storage.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class DAO
{
    private final static Cache<String, CacheableOptional<ArrayList<DataForm>>> EXTENSIONS_BY_ROOM = CacheFactory.createLocalCache("MUC Extended Service Discovery");

    private static final Logger Log = LoggerFactory.getLogger(DAO.class);

    public static void addForm( String roomName, DataForm dataForm )
    {
        Log.debug("Add Extension Element '{}' for room: '{}'", dataForm.getFormTypeName(), roomName);

        final CacheableOptional<ArrayList<DataForm>> old = EXTENSIONS_BY_ROOM.get(roomName);
        final ArrayList<DataForm> elements;
        if ( old == null || old.get() == null )
        {
            elements = new ArrayList<>();
        }
        else
        {
            elements = old.get();
        }
        elements.add(dataForm);
        EXTENSIONS_BY_ROOM.put(roomName, CacheableOptional.of(elements));
    }

    public static void removeForm( String roomName, String formTypeName )
    {
        Log.debug("Remove Extension Element '{}' for room: '{}'", formTypeName, roomName);

        final CacheableOptional<ArrayList<DataForm>> old = EXTENSIONS_BY_ROOM.get(roomName);
        final ArrayList<DataForm> elements;
        if ( old == null || old.get() == null )
        {
            elements = new ArrayList<>();
        }
        else
        {
            elements = old.get();
        }
        elements.removeIf(dataForm -> dataForm.getFormTypeName().equals(formTypeName));
        EXTENSIONS_BY_ROOM.put(roomName, CacheableOptional.of(elements.isEmpty() ? null : elements));
    }

    public static void addField( String roomName, String formTypeName, String varName, String label, String value )
    {
        if ( varName == null || varName.isEmpty() )
        {
            throw new IllegalArgumentException("Argument 'varName' cannot be a null or empty String.");
        }

        Log.debug("Adding Field '{}' from Extension Element '{}' for room: '{}'", varName, formTypeName, roomName);

        final CacheableOptional<ArrayList<DataForm>> old = EXTENSIONS_BY_ROOM.get(roomName);
        final ArrayList<DataForm> elements;
        if ( old == null || old.get() == null )
        {
            elements = new ArrayList<>();
        }
        else
        {
            elements = old.get();
        }

        for ( final DataForm element : elements )
        {
            if ( element.getFormTypeName().equals(formTypeName) )
            {
                final Optional<Field> oldField = element.getFields().stream().filter(f -> f.getVarName().equals(varName)).findAny();

                if ( oldField.isPresent() )
                {
                    // Add value if field already exists.
                    if ( value != null && !value.trim().isEmpty() )
                    {
                        final ArrayList<String> oldValues = new ArrayList<>();
                        for ( final String s : oldField.get().getValues() )
                        {
                            if ( s != null )
                            {
                                oldValues.add(s); // Filter out nulls
                            }
                        }

                        oldValues.add(value);
                        final Field field = new Field(varName, label, oldValues.toArray(new String[0]));

                        // Replace field.
                        element.getFields().remove(oldField.get());
                        element.getFields().add(field);
                    }

                }
                else
                {
                    final Field field = new Field(varName, label, value == null || value.trim().isEmpty() ? null : value.trim());
                    element.getFields().add(field);
                }
            }
        }
        EXTENSIONS_BY_ROOM.put(roomName, CacheableOptional.of(elements.isEmpty() ? null : elements));
    }


    public static void removeField( String roomName, String formTypeName, String varName )
    {
        Log.debug("Remove Field '{}' from Extension Element '{}' for room: '{}'", varName, formTypeName, roomName);

        final CacheableOptional<ArrayList<DataForm>> old = EXTENSIONS_BY_ROOM.get(roomName);
        final ArrayList<DataForm> elements;
        if ( old == null || old.get() == null )
        {
            elements = new ArrayList<>();
        }
        else
        {
            elements = old.get();
        }

        for ( final DataForm element : elements )
        {
            if ( element.getFormTypeName().equals(formTypeName) )
            {
                element.getFields().removeIf(field -> field.getVarName().equals(varName));
            }
        }
        EXTENSIONS_BY_ROOM.put(roomName, CacheableOptional.of(elements.isEmpty() ? null : elements));
    }

    public static List<DataForm> retrieveExtensionElementsForRoom( String roomName )
    {
        Log.debug("Get Extension Elements for room: '{}'", roomName);

        final CacheableOptional<ArrayList<DataForm>> optionalResult = EXTENSIONS_BY_ROOM.get(roomName);

        return optionalResult == null ? null : optionalResult.get();
    }

    public static void replaceExtensionElementsForRoom( String roomName, List<DataForm> dataForms )
    {
        Log.debug("Set {} Extension Element(s) for room: '{}'", dataForms.size(), roomName);

        EXTENSIONS_BY_ROOM.put(roomName, CacheableOptional.of(new ArrayList<>(dataForms)));
    }
}