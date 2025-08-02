package com.example.EscapeCenter_Demo.DataBaseService;

public abstract class DefaultService {
    static String[] JdbcUrls = {
            "jdbc:mysql://localhost:3306/escapecenter",
            "jdbc:mysql://sql7.freesqldatabase.com:3306/sql7790226"}; //example to online DataBase
    static String[] JdbcUsers = {"root", "sql7790226"};
    static String[] JdbcPasswords = {"1234", "******"};
    public static final String JDBC_URL = JdbcUrls[0];
    public static final String DB_USER = JdbcUsers[0];
    public static final String DB_PASS = JdbcPasswords[0];
}
