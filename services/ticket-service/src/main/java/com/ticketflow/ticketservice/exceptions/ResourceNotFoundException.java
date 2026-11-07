package com.ticketflow.ticketservice.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " " + identifier + " not found");
    }
}
