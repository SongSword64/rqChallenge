package com.reliaquest.api.model;

import lombok.Data;

@Data
public class SingleEmployeeResponse {
    private Employee data;
    private String status;
}
