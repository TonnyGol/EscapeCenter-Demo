package com.example.escapecenter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MainAppTest {

    @Test
    void sessionSingletonExists() {
        assertNotNull(com.example.escapecenter.service.Session.getInstance());
    }
}
