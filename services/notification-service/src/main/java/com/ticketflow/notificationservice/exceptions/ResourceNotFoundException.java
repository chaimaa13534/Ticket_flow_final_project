package com.ticketflow.notificationservice.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " " + identifier + " not found");
    }
}