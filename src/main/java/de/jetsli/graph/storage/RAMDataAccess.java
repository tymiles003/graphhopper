/*
 *  Copyright 2012 Peter Karich info@jetsli.de
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.jetsli.graph.storage;

import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * This is an in-memory data structure but with the possibility to be stored on flush().
 *
 * @author Peter Karich
 */
public class RAMDataAccess extends AbstractDataAccess {

    private String location;
    private int[] area;
    private float increaseFactor = 1.5f;
    private boolean closed = false;

    public RAMDataAccess(String location) {
        this.location = location;
    }

    @Override
    public void ensureCapacity(long bytes) {
        int intSize = (int) (bytes >> 2);
        if (area == null) {
            area = new int[intSize];
            return;
        }
        if (intSize <= area.length)
            return;

        area = Arrays.copyOf(area, (int) (intSize * increaseFactor));
    }

    @Override
    public boolean loadExisting() {
        try {
            if (area != null || closed)
                return false;

            RandomAccessFile in = new RandomAccessFile(location, "r");
            try {
                in.seek(0);
                if (!in.readUTF().equals(DATAACESS_MARKER))
                    return false;
                int len = (int) in.readLong();
                area = new int[len];
                in.seek(HEADER_INT);
                for (int i = 0; i < len; i++) {
                    area[i] = in.readInt();
                }

                return true;
            } finally {
                in.close();
            }
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public DataAccess flush() {
        try {
            RandomAccessFile out = new RandomAccessFile(location, "rw");
            try {
                out.seek(0);
                out.writeUTF(DATAACESS_MARKER);
                int len = area.length;
                out.writeLong(len);
                out.seek(HEADER_INT);
                for (int i = 0; i < len; i++) {
                    out.writeInt(area[i]);
                }
            } finally {
                out.close();
            }
            return this;
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't store integers to " + location, ex);
        }
    }

    @Override
    public void setInt(int index, int value) {
        area[index] = value;
    }

    @Override
    public int getInt(int index) {
        return area[index];
    }

    @Override
    public DataAccess close() {
        super.close();
        area = null;
        closed = true;
        return this;
    }

    public static DataAccess load(String location, int byteHint) {
        DataAccess da = new RAMDataAccess(location);
        if (da.loadExisting())
            return da;
        da.ensureCapacity(byteHint);
        return da;
    }
}
