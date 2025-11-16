package com.reliaquest.api.model;

import java.util.List;
import lombok.Data;

@Data
public class EmployeesResponse {
    private List<Employee> data;
    private String status;
}
