package com.formation.step1;

import com.formation.security.TestSecurityConfig;
import com.formation.security.UtilisateurDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HelloController.class)
@Import(TestSecurityConfig.class)
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UtilisateurDetailsService utilisateurDetailsService;

    @MockitoBean
    private com.formation.security.JwtService jwtService;

    @Test
    void hello_retourne_hello_world() throws Exception {
        mockMvc.perform(get("/step1/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World!"));
    }

    @Test
    void greet_sans_param_retourne_bonjour_monde() throws Exception {
        mockMvc.perform(get("/step1/greet"))
                .andExpect(status().isOk())
                .andExpect(content().string("Bonjour Monde!"));
    }

    @Test
    void greet_avec_param_retourne_bonjour_nom() throws Exception {
        mockMvc.perform(get("/step1/greet").param("nom", "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Bonjour Alice!"));
    }

    @Test
    void info_retourne_json_avec_titre() throws Exception {
        mockMvc.perform(get("/step1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Application de formation"))
                .andExpect(jsonPath("$.version").value("v1.0"));
    }
}
