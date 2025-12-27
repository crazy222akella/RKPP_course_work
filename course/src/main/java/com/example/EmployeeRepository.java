package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class EmployeeRepository {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<Employee> loadEmployees() {
        try (InputStream is = EmployeeRepository.class.getResourceAsStream("/src/main/data/employees.json")) {
            if (is == null) {
                return Collections.emptyList();
            }
            return mapper.readValue(is, new TypeReference<List<Employee>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
