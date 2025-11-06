package de.flix29.sprout.annotations.model;

public enum Endpoint {

    GET_ALL("getAll"),
    GET_BY_ID("getById"),
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete");

    private final String name;

    Endpoint(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
