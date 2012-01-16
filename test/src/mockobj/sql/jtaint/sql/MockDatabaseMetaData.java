/*
 *  Copyright 2009-2012 Michael Dalton
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package jtaint.sql;

import java.sql.DatabaseMetaData;

public class MockDatabaseMetaData implements DatabaseMetaData {
    private final String name;
    private final int min, maj;

    public MockDatabaseMetaData(String name, int maj, int min) {
        this.name = name;
        this.min = min;
        this.maj = maj;
    }

    public String getDatabaseProductName() { return name; }

    public String getDatabaseProductVersion() { return name + maj + "." + min; }

    public int getDatabaseMajorVersion() { return maj; }

    public int getDatabaseMinorVersion() { return min; }
}
