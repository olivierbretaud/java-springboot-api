# ÉTAPE 5 : Authentification & JWT

## 📚 Table des matières

1. [Objectif](#objectif)
2. [Concepts clés](#concepts-clés)
3. [Architecture](#architecture)
4. [Fichiers implémentés](#fichiers-implémentés)
5. [Tests](#tests)
6. [Prochaines étapes](#prochaines-étapes)

---

## 🎯 Objectif

**Protéger les endpoints de l'API avec Spring Security et des tokens JWT.**

Après cette étape, vous allez pouvoir :
- ✅ Comprendre le flux d'authentification stateless avec JWT
- ✅ Configurer Spring Security pour bloquer les endpoints non authentifiés
- ✅ Implémenter l'inscription et la connexion d'un utilisateur
- ✅ Générer et valider un token JWT
- ✅ Protéger des endpoints selon le rôle de l'utilisateur (`ADMIN`, `USER`)

### Le flux d'authentification JWT

```
Client                          Serveur
  │                                │
  │  POST /api/auth/register       │
  │  { email, password }  ───────► │  hash le mot de passe (BCrypt)
  │                                │  sauvegarde l'utilisateur en base
  │ ◄─────────────────────────── 201 Created
  │
  │  POST /api/auth/login          │
  │  { email, password }  ───────► │  vérifie les credentials
  │                                │  génère un token JWT signé
  │ ◄──────────── 200 { token: "eyJ..." }
  │
  │  GET /api/commandes            │
  │  Authorization: Bearer eyJ... ►│  valide le token (signature + expiration)
  │                                │  extrait l'utilisateur du token
  │ ◄──────────────────────────── 200 [...]
  │
  │  GET /api/admin/users          │
  │  Authorization: Bearer eyJ... ►│  vérifie le rôle ADMIN
  │ ◄──────────────────────────── 403 Forbidden (si rôle USER)
```

---

## 📖 Concepts clés

### 1. JWT — JSON Web Token

Un JWT est une chaîne encodée en Base64 composée de trois parties séparées par des points :

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9   ← Header  (algorithme + type)
.eyJzdWIiOiJ1c2VyQHRlc3QuY29tIiwicm9sZSI6IlVTRVIiLCJleHAiOjE3MzAwMDAwMDB9
                                         ← Payload (claims : sujet, rôle, expiration)
.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
                                         ← Signature (HMAC-SHA256 avec le secret)
```

**Propriétés** :
- **Stateless** : le serveur ne stocke pas de session, toutes les informations sont dans le token
- **Signé** : la signature garantit que le token n'a pas été modifié
- **Expirant** : le token a une durée de vie limitée (`exp` claim)

### 2. Spring Security — la chaîne de filtres

Spring Security fonctionne comme une série de filtres HTTP qui s'exécutent avant d'atteindre le Controller :

```
Requête HTTP
    │
    ▼
JwtAuthenticationFilter     ← notre filtre custom : lit le token, charge l'utilisateur
    │
    ▼
UsernamePasswordAuthenticationFilter
    │
    ▼
SecurityContextHolder       ← contient l'utilisateur authentifié pour cette requête
    │
    ▼
Controller
```

### 3. `UserDetailsService` — charger l'utilisateur depuis la base

Spring Security délègue le chargement de l'utilisateur à cette interface :

```java
@Service
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));
    }
}
```

### 4. BCrypt — hacher les mots de passe

Les mots de passe ne sont **jamais** stockés en clair. BCrypt est un algorithme de hachage lent conçu pour résister aux attaques par force brute :

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// Lors de l'inscription
String hash = passwordEncoder.encode("monMotDePasse");  // "$2a$10$..."

// Lors de la connexion
boolean valide = passwordEncoder.matches("monMotDePasse", hash);  // true
```

**Important** : BCrypt inclut un sel aléatoire — deux hachages du même mot de passe sont différents.

### 5. `SecurityConfig` — configurer les règles d'accès

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())                        // API REST stateless
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()     // login/register publics
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()                    // tout le reste : token requis
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

---

## 🏗️ Architecture

```
src/main/java/com/formation/
│
├── engine/
│   ├── entities/UtilisateurEntity.java          ← password + roles
│   └── repositories/UtilisateurRepository.java  ← findByEmail
│
├── core/auth/
│   ├── dto/
│   │   ├── LoginRequest.java                    ← { email, password }
│   │   ├── RegisterRequest.java                 ← { email, password, nom }
│   │   └── AuthResponse.java                    ← { token }
│   ├── service/
│   │   └── AuthService.java                     ← register() + login()
│   └── controller/
│       └── AuthController.java                  ← POST /api/auth/register + /login
│
└── security/
    ├── SecurityConfig.java                      ← règles d'accès
    ├── JwtService.java                          ← générer + valider les tokens
    ├── JwtAuthenticationFilter.java             ← filtre HTTP custom
    └── UtilisateurDetailsService.java           ← UserDetailsService
```

---

## 💻 Fichiers implémentés

### Dépendances — `build.gradle.kts`

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
}
```

### `JwtService.java`

```java
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms:86400000}") // 24h par défaut
    private long expirationMs;

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
```

### `JwtAuthenticationFilter.java`

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### `AuthService.java`

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        UtilisateurEntity utilisateur = new UtilisateurEntity();
        utilisateur.setEmail(request.getEmail());
        utilisateur.setNom(request.getNom());
        utilisateur.setPassword(passwordEncoder.encode(request.getPassword()));
        utilisateur.setRole(Role.USER);

        utilisateurRepository.save(utilisateur);

        String token = jwtService.generateToken(utilisateur);
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        // Si on arrive ici, les credentials sont valides
        UtilisateurEntity utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow();

        String token = jwtService.generateToken(utilisateur);
        return new AuthResponse(token);
    }
}
```

### `application.yml` — configuration JWT

```yaml
app:
  jwt:
    secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970   # 256 bits en Base64
    expiration-ms: 86400000  # 24 heures
```

---

## 🧪 Tests

### Tester `JwtService` (test unitaire)

```java
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86400000L);
    }

    @Test
    void generateToken_puis_extractUsername_retourne_le_bon_email() {
        UserDetails userDetails = User.withUsername("alice@test.com")
                .password("hash").roles("USER").build();

        String token = jwtService.generateToken(userDetails);
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("alice@test.com");
    }

    @Test
    void isTokenValid_retourne_true_pour_un_token_valide() {
        UserDetails userDetails = User.withUsername("bob@test.com")
                .password("hash").roles("USER").build();

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }
}
```

### Tester `AuthController` avec `@WebMvcTest`

```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void login_retourne_token_si_credentials_valides() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("eyJ..."));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@test.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_retourne_401_si_credentials_invalides() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Mauvais credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@test.com\",\"password\":\"mauvais\"}"))
                .andExpect(status().isUnauthorized());
    }
}
```

### Tester un endpoint protégé

```java
@WebMvcTest(ProduitController.class)
class ProduitControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IProduitService produitService;

    @Test
    void listProduits_retourne_401_sans_token() throws Exception {
        mockMvc.perform(get("/api/produit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void listProduits_retourne_200_avec_utilisateur_authentifie() throws Exception {
        when(produitService.listProduits()).thenReturn(List.of());

        mockMvc.perform(get("/api/produit"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void endpointAdmin_retourne_403_pour_un_user() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }
}
```

**`@WithMockUser`** : simule un utilisateur authentifié dans le `SecurityContext` sans passer par le filtre JWT — parfait pour les tests `@WebMvcTest`.

---

## 💡 Pièges courants

### Exposer les endpoints `/api/auth/**`

Sans cette règle, le login lui-même serait bloqué :

```java
.requestMatchers("/api/auth/**").permitAll()  // ← indispensable
```

### Stocker le secret JWT en clair

```yaml
# ❌ Ne jamais faire ça en production
app.jwt.secret: monsecret

# ✅ Variable d'environnement
app.jwt.secret: ${JWT_SECRET}
```

### Boucle infinie dans `JwtAuthenticationFilter`

Le filtre s'applique à **toutes** les requêtes, y compris `/api/auth/login`. Il faut vérifier la présence du header `Authorization` avant de tenter de valider le token, ce qui est déjà fait dans l'implémentation ci-dessus (le `return` anticipé si pas de header).

---

## ✅ Prochaines étapes

1. ✅ Vous savez générer et valider un token JWT
2. ✅ Vous savez configurer Spring Security pour protéger les endpoints
3. ✅ Vous savez tester des endpoints sécurisés avec `@WithMockUser`

**Prochaine étape** : Tests avancés — pyramide des tests, `@WebMvcTest`, `@DataJpaTest`, TestContainers.

---

*Dernière mise à jour : 2026-10-16*
