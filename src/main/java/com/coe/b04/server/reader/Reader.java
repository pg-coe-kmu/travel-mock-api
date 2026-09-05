package com.coe.b04.server.reader;

import java.util.List;

/*
 * Reads the mock data files (local folder or S3 bucket).
 */
public interface Reader {
    <T> List<T> load(String fileName, Class<T[]> clazz);
}
