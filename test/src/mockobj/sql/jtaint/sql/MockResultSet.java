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

import java.util.ArrayList;
import java.util.List;

import java.sql.ResultSet;

public class MockResultSet implements ResultSet {
    private static List results = new ArrayList();
    private static int offset;
    private static boolean wrapAroundRetval;

    public static void setResults(List results, boolean wrapAroundRetval) {
        MockResultSet.results = results;
        MockResultSet.offset = 0;
        MockResultSet.wrapAroundRetval = wrapAroundRetval;
    }

    public boolean next() { 
        if (offset >= results.size()) {
            offset = 0;
            return wrapAroundRetval;
        } else if (results.get(offset) == null) {
            offset++;
            return false;
        }

        return true;
    }

    public String getString(int columnIndex) { 
        return (String) results.get(offset++);
    }
}
