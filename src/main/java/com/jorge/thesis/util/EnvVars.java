package com.jorge.thesis.util;

public final class EnvVars {

    public static final String API_KEY = System.getenv("COMM_EXP_GCM_SERV_API_KEY");
    public static final String PORT = System.getenv("COMM_EXP_GCM_SERV_PORT");

    private EnvVars() throws IllegalAccessException {
        throw new IllegalAccessException("DO NOT CONSTRUCT " + EnvVars.class.getName());
        //Forbid construction even through reflection
    }
}
