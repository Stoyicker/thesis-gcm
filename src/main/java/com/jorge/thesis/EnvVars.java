package com.jorge.thesis;

public final class EnvVars {

    public static final String API_KEY = System.getenv("COMM_EXP_API_KEY");

    private EnvVars() throws IllegalAccessException {
        throw new IllegalAccessException("DO NOT CONSTRUCT " + EnvVars.class.getName());
        //Forbid construction
    }
}
