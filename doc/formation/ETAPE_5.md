# ÉTAPE 5 : Authentification & JWT

## 📚 Table des matières

1. [Objectif](#objectif)
2. [Concepts clés](#concepts-clés)
3. [Architecture](#architecture)
4. [Fichiers implémentés](#fichiers-implémentés)
5. [Aller plus loin : Refresh token](#aller-plus-loin--refresh-token)
6. [Aller plus loin : Protection brute-force](#aller-plus-loin--protection-brute-force)
7. [Tests](#tests)
8. [Prochaines étapes](#prochaines-étapes)

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

## 🔄 Aller plus loin : Refresh token

### Pourquoi ?

Un `accessToken` JWT expiré force le re-login. En production, on émet deux tokens au login :

| Token | Durée | Stockage | Rôle |
|-------|-------|----------|------|
| `accessToken` | 15 min — 1h | Mémoire JS / header | Authentifier chaque requête |
| `refreshToken` | 7 — 30 jours | Base de données | Obtenir un nouvel `accessToken` |

Le `refreshToken` est un UUID opaque (pas un JWT) — sa valeur ne révèle rien, il est valide uniquement s'il existe en base et n'est pas expiré.

### Flux côté serveur

```
Login
  → accessToken (1h)  + refreshToken (7j, stocké en base)

Requête avec accessToken expiré → 401
  → POST /auth/refresh { refreshToken: "uuid..." }
      → vérifie en base (existe + non expiré + même utilisateur)
      → émet un nouveau accessToken
      → (optionnel) rotation : invalide l'ancien refreshToken, émet un nouveau

Logout
  → supprime le refreshToken de la base → impossibilité de renouveler
```

### Flux côté front — vision complète

```
┌─────────────────────────────────────────────────────────────────────┐
│                        NAVIGATEUR / APP                             │
│                                                                     │
│  1. Login                                                           │
│     POST /auth/login { email, motDePasse }                          │
│     ◄── { accessToken: "eyJ...", refreshToken: "uuid...", expiresIn: 3600 }
│                                                                     │
│     → Stocker accessToken  : mémoire (variable JS)                 │
│     → Stocker refreshToken : localStorage ou cookie HttpOnly        │
│                                                                     │
│  2. Requête normale                                                 │
│     GET /commandes                                                  │
│     Authorization: Bearer eyJ...                                    │
│     ◄── 200 OK                                                      │
│                                                                     │
│  3. accessToken expiré                                              │
│     GET /commandes                                                  │
│     Authorization: Bearer eyJ... (expiré)                           │
│     ◄── 401 Unauthorized                                            │
│                                                                     │
│  4. Refresh automatique (intercepteur)                              │
│     POST /auth/refresh { refreshToken: "uuid..." }                  │
│     ◄── { accessToken: "eyJ...nouveau", refreshToken: "uuid...", expiresIn: 3600 }
│                                                                     │
│     → Remplacer l'accessToken en mémoire                           │
│     → Rejouer la requête originale avec le nouveau token            │
│                                                                     │
│  5. GET /commandes (rejoué)                                         │
│     Authorization: Bearer eyJ...nouveau                             │
│     ◄── 200 OK                                                      │
│                                                                     │
│  6. refreshToken expiré ou révoqué                                  │
│     POST /auth/refresh { refreshToken: "uuid...périmé" }            │
│     ◄── 400 Bad Request                                             │
│     → Effacer les tokens, rediriger vers /login                     │
│                                                                     │
│  7. Logout                                                          │
│     POST /auth/logout { refreshToken: "uuid..." }                   │
│     ◄── 204 No Content                                              │
│     → Effacer accessToken + refreshToken, rediriger vers /login     │
└─────────────────────────────────────────────────────────────────────┘
```

### Où stocker les tokens ?

| Stockage | accessToken | refreshToken | Risque |
|----------|-------------|--------------|--------|
| **Mémoire JS** (variable) | ✅ recommandé | ❌ perdu au reload | Aucun — inaccessible hors JS |
| **localStorage** | ⚠️ acceptable | ⚠️ acceptable | Vol par XSS |
| **Cookie HttpOnly** | ❌ complexe avec CORS | ✅ idéal | Cookie inaccessible au JS |
| **sessionStorage** | ✅ | ❌ perdu à la fermeture | Limité à l'onglet |

**Recommandation :** `accessToken` en mémoire (variable module) + `refreshToken` en `localStorage`. Au rechargement de page, le front fait un appel `/auth/refresh` au démarrage pour récupérer un nouveau `accessToken`.

### Implémentation front — intercepteur Axios

```javascript
// api.js
import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

// Injecter le token sur chaque requête
api.interceptors.request.use(config => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

// Intercepter les 401 et rafraîchir automatiquement
let isRefreshing = false
let pendingRequests = []

api.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      if (isRefreshing) {
        // Une autre requête est déjà en train de rafraîchir — mettre en attente
        return new Promise(resolve => {
          pendingRequests.push(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(api(originalRequest))
          })
        })
      }

      isRefreshing = true
      try {
        const refreshToken = localStorage.getItem('refreshToken')
        const { data } = await axios.post('/api/auth/refresh', { refreshToken })

        accessToken = data.accessToken                         // mettre à jour en mémoire
        localStorage.setItem('refreshToken', data.refreshToken) // rotation

        pendingRequests.forEach(cb => cb(accessToken))         // rejouer les requêtes en attente
        pendingRequests = []

        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        return api(originalRequest)                            // rejouer la requête originale
      } catch {
        localStorage.removeItem('refreshToken')
        window.location.href = '/login'                        // session expirée → login
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(error)
  }
)

export default api
```

**`isRefreshing` + `pendingRequests`** : si plusieurs requêtes expirent simultanément (ex. page avec 3 appels parallèles), un seul refresh est émis. Les autres attendent et sont rejouées avec le nouveau token.

### Implémentation front — fetch natif (sans Axios)

```javascript
// authFetch.js
async function authFetch(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
  })

  if (response.status === 401) {
    const refreshed = await refreshAccessToken()
    if (!refreshed) {
      window.location.href = '/login'
      return
    }
    // Rejouer avec le nouveau token
    return fetch(url, {
      ...options,
      headers: {
        ...options.headers,
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
    })
  }
  return response
}

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return false

  const res = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })

  if (!res.ok) return false

  const data = await res.json()
  accessToken = data.accessToken
  localStorage.setItem('refreshToken', data.refreshToken)
  return true
}
```

### Démarrage de l'application (reload de page)

```javascript
// main.js — au chargement de l'app
async function init() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) {
    redirectToLogin()
    return
  }

  try {
    const { data } = await axios.post('/api/auth/refresh', { refreshToken })
    accessToken = data.accessToken
    localStorage.setItem('refreshToken', data.refreshToken)
    // L'utilisateur est restauré sans re-login
  } catch {
    localStorage.removeItem('refreshToken')
    redirectToLogin()
  }
}
```

L'utilisateur ne voit pas le login si son `refreshToken` est encore valide — même après F5.

### Migration — nouvelle table

```yaml
# 017-create-refresh-tokens.yaml
databaseChangeLog:
  - changeSet:
      id: 017-create-refresh-tokens
      author: formation
      changes:
        - createTable:
            tableName: refresh_tokens
            columns:
              - column: { name: id, type: BIGSERIAL, constraints: { primaryKey: true } }
              - column: { name: token, type: VARCHAR(255), constraints: { nullable: false, unique: true } }
              - column: { name: utilisateur_id, type: BIGINT, constraints: { nullable: false } }
              - column: { name: expiration, type: TIMESTAMP, constraints: { nullable: false } }
              - column: { name: revoque, type: BOOLEAN, defaultValueBoolean: false }
        - addForeignKeyConstraint:
            baseTableName: refresh_tokens
            baseColumnNames: utilisateur_id
            referencedTableName: utilisateurs
            referencedColumnNames: id
            constraintName: fk_refresh_token_utilisateur
            onDelete: CASCADE
```

### Entité

```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private UtilisateurEntity utilisateur;

    @Column(nullable = false)
    private LocalDateTime expiration;

    @Column(nullable = false)
    private boolean revoque = false;

    public boolean isExpire() {
        return LocalDateTime.now().isAfter(expiration);
    }
}
```

### Service

```java
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    public RefreshTokenEntity creer(UtilisateurEntity utilisateur) {
        // Invalider les anciens tokens de cet utilisateur (rotation)
        refreshTokenRepository.revoquerParUtilisateur(utilisateur.getId());

        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setToken(UUID.randomUUID().toString());
        token.setUtilisateur(utilisateur);
        token.setExpiration(LocalDateTime.now().plusDays(refreshExpirationDays));
        return refreshTokenRepository.save(token);
    }

    public RefreshTokenEntity valider(String token) {
        RefreshTokenEntity rt = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new ResetTokenInvalidException("Refresh token introuvable"));

        if (rt.isRevoque() || rt.isExpire()) {
            throw new ResetTokenInvalidException("Refresh token expiré ou révoqué");
        }
        return rt;
    }
}
```

### Endpoint

```java
// AuthController.java
@PostMapping("/refresh")
public AuthResponse refresh(@RequestBody RefreshRequest request) {
    RefreshTokenEntity rt = refreshTokenService.valider(request.getRefreshToken());
    String newAccessToken = jwtService.generateToken(rt.getUtilisateur());
    return new AuthResponse(newAccessToken);
}

// Logout — invalide le refreshToken
@PostMapping("/logout")
public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
    refreshTokenService.revoquer(request.getRefreshToken());
    return ResponseEntity.noContent().build();
}
```

### Réponse login enrichie

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "a3f7b8c9-1234-5678-abcd-ef0123456789",
  "expiresIn": 3600
}
```

### Pourquoi rotation des refresh tokens ?

Si un `refreshToken` est volé, l'attaquant peut obtenir des `accessTokens` indéfiniment. Avec la rotation, chaque utilisation du `refreshToken` en émet un nouveau et invalide l'ancien. Si l'attaquant utilise le token en premier, la prochaine tentative légitime échoue → l'utilisateur est alerté.

---

## 🛡️ Aller plus loin : Protection brute-force

### Pourquoi ?

Sans protection, un attaquant peut tenter des milliers de combinaisons email/mot de passe sans être bloqué. BCrypt ralentit chaque tentative (~100ms) mais n'empêche pas un volume élevé de requêtes parallèles.

### Stratégie : compteur en base

Stocker sur `UtilisateurEntity` le nombre de tentatives échouées et la date de déblocage :

```java
// UtilisateurEntity.java — colonnes à ajouter
@Column(nullable = false)
private int tentativesEchouees = 0;

@Column
private LocalDateTime blocageJusqua;

public boolean estBloque() {
    return blocageJusqua != null && LocalDateTime.now().isBefore(blocageJusqua);
}
```

```yaml
# 018-add-brute-force-columns.yaml
databaseChangeLog:
  - changeSet:
      id: 018-add-brute-force-columns
      author: formation
      changes:
        - addColumn:
            tableName: utilisateurs
            columns:
              - column: { name: tentatives_echouees, type: INT, defaultValueNumeric: 0, constraints: { nullable: false } }
              - column: { name: blocage_jusqu_a, type: TIMESTAMP }
```

### Logique dans AuthService

```java
private static final int MAX_TENTATIVES = 5;
private static final int BLOCAGE_MINUTES = 15;

public AuthResponse login(LoginRequest request) {
    UtilisateurEntity utilisateur = utilisateurRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect."));

    // 1. Compte bloqué ?
    if (utilisateur.estBloque()) {
        throw new BadCredentialsException("Compte temporairement bloqué. Réessayez dans quelques minutes.");
    }

    // 2. Vérifier le mot de passe
    if (!passwordEncoder.matches(request.getPassword(), utilisateur.getPassword())) {
        enregistrerEchec(utilisateur);
        throw new BadCredentialsException("Email ou mot de passe incorrect.");
    }

    // 3. Succès — réinitialiser le compteur
    utilisateur.setTentativesEchouees(0);
    utilisateur.setBlocageJusqua(null);
    utilisateurRepository.save(utilisateur);

    String token = jwtService.generateToken(utilisateur);
    return new AuthResponse(token);
}

private void enregistrerEchec(UtilisateurEntity utilisateur) {
    int tentatives = utilisateur.getTentativesEchouees() + 1;
    utilisateur.setTentativesEchouees(tentatives);

    if (tentatives >= MAX_TENTATIVES) {
        utilisateur.setBlocageJusqua(LocalDateTime.now().plusMinutes(BLOCAGE_MINUTES));
        log.warn("Compte {} bloqué après {} tentatives", utilisateur.getEmail(), tentatives);
    }
    utilisateurRepository.save(utilisateur);
}
```

### Ce que ça retourne au client

```json
// 5e tentative échouée
{
  "type": "about:blank",
  "title": "Authentification échouée",
  "status": 401,
  "detail": "Compte temporairement bloqué. Réessayez dans quelques minutes."
}
```

**Important :** le message d'erreur est identique pour email inexistant et mot de passe incorrect — on ne révèle pas si l'email existe en base (enumeration attack).

### Pourquoi pas de `@RateLimiter` Spring ?

`@RateLimiter` (Resilience4j) limite les appels globalement, pas par utilisateur ou par IP. Un attaquant qui cible 1000 comptes différents avec 1 tentative chacun ne serait pas bloqué. Le compteur par compte cible le comportement suspect par cible.

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
4. ➕ Refresh token : `POST /auth/refresh` + rotation en base
5. ➕ Brute-force : compteur de tentatives + blocage temporaire par compte

**Prochaine étape** : Gestion des erreurs & validation — `ProblemDetail` RFC 9457, `@Valid`, `@RestControllerAdvice`.

---

*Dernière mise à jour : 2026-10-16*
