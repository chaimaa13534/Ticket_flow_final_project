package com.ticketflow.customerservice.exceptions;

/**
 * Levée quand une ressource demandée n'existe pas — déclenche une réponse 404.
 *
 * CdC §7.2 : "404 ou réponse publique si non publié ; pas de fuite de brouillon."
 */
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