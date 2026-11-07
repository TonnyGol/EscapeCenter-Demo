package Entities;

// To add a role: add a value here, then give it access in SecurityConfig. The database stores the name, so no migration is needed.
public enum WorkerRole {
    ADMIN,
    STAFF
}
