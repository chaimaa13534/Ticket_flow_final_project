package com.ticketflow.bookingservice.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String identifier;

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " " + identifier + " not found");
        this.resourceType = resourceType;
        this.identifier = identifier;
    }

    public String getResourceType() { return resourceType; }
    public String getIdentifier() { return identifier; }
}
