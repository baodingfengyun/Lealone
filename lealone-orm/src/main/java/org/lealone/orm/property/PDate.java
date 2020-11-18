/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lealone.orm.property;

import java.io.IOException;
import java.sql.Date;

import org.lealone.db.value.Value;
import org.lealone.db.value.ValueDate;
import org.lealone.orm.Model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NumericNode;

/**
 * Java sql date property.
 *
 * @param <R> the root model bean type
 */
public class PDate<R> extends PBaseDate<R, Date> {

    private Date value;

    /**
     * Construct with a property name and root instance.
     *
     * @param name property name
     * @param root the root model bean instance
     */
    public PDate(String name, R root) {
        super(name, root);
    }

    private PDate<R> P(Model<?> model) {
        return this.<PDate<R>> getModelProperty(model);
    }

    public final R set(Date value) {
        Model<?> model = getModel();
        if (model != root) {
            return P(model).set(value);
        }
        if (!areEqual(this.value, value)) {
            this.value = value;
            expr().set(name, ValueDate.get(value));
        }
        return root;
    }

    @Override
    public R set(Object value) {
        return set(Date.valueOf(value.toString()));
    }

    public final Date get() {
        Model<?> model = getModel();
        if (model != root) {
            return P(model).get();
        }
        return value;
    }

    @Override
    public R serialize(JsonGenerator jgen) throws IOException {
        jgen.writeNumberField(getName(), value.getTime());
        return root;
    }

    @Override
    public R deserialize(JsonNode node) {
        node = getJsonNode(node);
        if (node == null) {
            return root;
        }
        Date date = new Date(((NumericNode) node).asLong());
        set(date);
        return root;
    }

    @Override
    protected void deserialize(Value v) {
        value = v.getDate();
    }
}
