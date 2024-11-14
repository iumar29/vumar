package com.Jhipster.Jhipster_project.Model;



public class Relationship {
    private String type;
    private String source;
    private String sourceField;
    private String destination;
    private String destinationField;

    public Relationship(String type, String source, String sourceField, String destination, String destinationField) {
        this.type = type;
        this.source = source;
        this.sourceField = sourceField;
        this.destination = destination;
        this.destinationField = destinationField;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDestinationField() {
        return destinationField;
    }

    public void setDestinationField(String destinationField) {
        this.destinationField = destinationField;
    }
}

