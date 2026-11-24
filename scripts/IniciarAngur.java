import java.awt.Desktop;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.jar.JarFile;

/** Arranque local, con configuración privada, diagnóstico JDBC y modo portátil. */
class IniciarAngur {
    private final Path root = Path.of("").toAbsolutePath();
    private final Map<String, String> env = new HashMap<>(System.getenv());
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private boolean browser = true, docker = true, packaged, check;
    private int port;
    private Process app;

    public static void main(String[] args) {
        try {
            new IniciarAngur().run(args);
        } catch (Exception error) {
            System.err.println("\nNo se pudo iniciar Angur Gestión: " + error.getMessage());
            System.exit(1);
        }
    }

    private void run(String[] args) throws Exception {
        if (Runtime.version().feature() < 21)
            throw new IllegalStateException("se necesita Java 21 o posterior.");
        if (!Files.exists(root.resolve("pom.xml")))
            throw new IllegalStateException(
                    "extrae el proyecto completo y abre iniciar-angur desde esa carpeta.");
        load();
        docker = !"false".equalsIgnoreCase(env.getOrDefault("ANGUR_USE_DOCKER", "true"));
        port = Integer.parseInt(env.getOrDefault("SERVER_PORT", "18080"));
        for (int i = 0; i < args.length; i++)
            switch (args[i]) {
                case "--no-browser" -> browser = false;
                case "--no-docker" -> docker = false;
                case "--jar" -> packaged = true;
                case "--rebuild" -> packaged = false;
                case "--check" -> check = true;
                case "--port" -> {
                    if (++i == args.length)
                        throw new IllegalArgumentException("falta el número después de --port.");
                    port = Integer.parseInt(args[i]);
                }
                case "--help" -> {
                    System.out.println(
                            "iniciar-angur [--port 18080] [--no-browser] [--no-docker] [--jar]"
                                + " [--rebuild] [--check]");
                    return;
                }
                default -> throw new IllegalArgumentException("opción desconocida: " + args[i]);
            }
        if (port < 1024 || port > 65535)
            throw new IllegalArgumentException("elige un puerto entre 1024 y 65535.");
        if (check) {
            database(jar());
            return;
        }
        if (ready()) {
            database(jar());
            System.out.println("Angur Gestión ya está ejecutándose: " + url());
            open();
            return;
        }
        if (listening(port))
            throw new IllegalStateException(
                    "el puerto " + port + " está ocupado. Usa --port con otro número.");
        if (!Files.exists(root.resolve(".env"))) {
            createEnvironment();
            load();
            if (listening(3306))
                throw new IllegalStateException(
                        "ya existe MySQL en el puerto 3306. Ajusta DB_USER, DB_PASSWORD y DB_URL en"
                            + " .env para esa instancia; no se modificaron sus datos.");
        }
        env.put("SERVER_PORT", Integer.toString(port));
        String base = env.get("AUTH_BASE_URL");
        if (base == null || local(URI.create(base)))
            env.put("AUTH_BASE_URL", "http://localhost:" + port);
        services();
        if (!packaged) {
            System.out.println(
                    "Preparando la aplicación. La primera compilación descarga dependencias"
                        + " Maven.");
            List<String> cmd = new ArrayList<>();
            if (windows()) cmd.addAll(List.of("cmd.exe", "/d", "/c", "mvnw.cmd"));
            else cmd.addAll(List.of("bash", "mvnw"));
            cmd.addAll(List.of("-B", "-ntp", "-DskipTests", "package"));
            if (process(cmd).start().waitFor() != 0)
                throw new IllegalStateException(
                        "Maven no pudo compilar. Revisa el mensaje anterior, tu JDK y la conexión a"
                            + " Internet.");
        }
        Path jar = jar();
        database(jar);
        String java =
                Path.of(System.getProperty("java.home"), "bin", windows() ? "java.exe" : "java")
                        .toString();
        app = process(List.of(java, "-jar", jar.toString(), "--server.address=127.0.0.1")).start();
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    if (app != null && app.isAlive()) app.destroy();
                                }));
        long deadline = System.nanoTime() + Duration.ofMinutes(2).toNanos();
        while (!ready()) {
            if (!app.isAlive())
                throw new IllegalStateException(
                        "el servidor terminó antes de estar listo. Revisa el mensaje anterior.");
            if (System.nanoTime() > deadline) {
                app.destroy();
                throw new IllegalStateException("el servidor no respondió en dos minutos.");
            }
            Thread.sleep(400);
        }
        System.out.println("\nAngur Gestión está listo: " + url());
        System.out.println(
                "Deja esta ventana abierta. Ctrl+C detiene la aplicación; los datos de MySQL se"
                    + " conservan.");
        open();
        int result = app.waitFor();
        if (result != 0 && result != 130 && result != 143)
            throw new IllegalStateException("el servidor terminó con código " + result + ".");
    }

    private Path jar() throws IOException {
        if (!Files.isDirectory(root.resolve("target")))
            throw new IllegalStateException(
                    "falta target. Extrae TODO el paquete listo; no ejecutes solamente el .cmd."
                        + " Para compilar desde fuentes, inicia sin --jar.");
        try (var files = Files.list(root.resolve("target"))) {
            return files.filter(
                            p ->
                                    p.getFileName().toString().startsWith("angur-gestion-")
                                            && p.toString().endsWith(".jar"))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .orElseThrow(
                            () ->
                                    new IllegalStateException(
                                            "no se encontró el JAR de la aplicación. Usa --rebuild"
                                                + " con un JDK 21."));
        }
    }

    private void database(Path artifact) throws Exception {
        Path scratch = root.resolve(".tmp");
        Files.createDirectories(scratch);
        Path driverJar = Files.createTempFile(scratch, "mysql-driver-", ".jar");
        try {
            try (JarFile archive = new JarFile(artifact.toFile())) {
                var entry =
                        archive.stream()
                                .filter(
                                        e ->
                                                e.getName()
                                                                .startsWith(
                                                                        "BOOT-INF/lib/mysql-connector-j-")
                                                        && e.getName().endsWith(".jar"))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "falta el controlador MySQL. Usa"
                                                            + " --rebuild."));
                try (var input = archive.getInputStream(entry)) {
                    Files.copy(input, driverJar, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            try (URLClassLoader loader =
                    new URLClassLoader(
                            new URL[] {driverJar.toUri().toURL()},
                            ClassLoader.getPlatformClassLoader())) {
                Driver driver =
                        (Driver)
                                Class.forName("com.mysql.cj.jdbc.Driver", true, loader)
                                        .getDeclaredConstructor()
                                        .newInstance();
                Properties settings = new Properties();
                settings.setProperty("user", env.getOrDefault("DB_USER", "root"));
                settings.setProperty("password", env.getOrDefault("DB_PASSWORD", ""));
                settings.setProperty("connectTimeout", "3000");
                settings.setProperty("socketTimeout", "5000");
                try (Connection connection =
                        driver.connect(
                                env.getOrDefault(
                                        "DB_URL", "jdbc:mysql://localhost:3306/angur_gestion_admin"),
                                settings)) {
                    if (connection == null)
                        throw new IllegalStateException("DB_URL no es una dirección MySQL válida.");
                    Set<String> tables = new HashSet<>();
                    try (ResultSet rows =
                            connection
                                    .getMetaData()
                                    .getTables(
                                            connection.getCatalog(),
                                            null,
                                            "%",
                                            new String[] {"TABLE"})) {
                        while (rows.next())
                            tables.add(rows.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
                    }
                    if (!tables.contains("usuarios"))
                        throw new IllegalStateException(
                                "faltan las tablas. Ejecuta src/main/resources/db/schema.sql en"
                                    + " MySQL Workbench.");
                    if (!tables.containsAll(Set.of("auth_setup", "auth_tokens", "solicitudes_admin", "auditoria_eventos")))
                        throw new IllegalStateException(
                                "la base es anterior. Respalda y aplica las migraciones pendientes"
                                    + " de src/main/resources/db/migrations en orden.");
                    try (Statement st = connection.createStatement()) {
                        st.executeQuery(
                                        "SELECT correo_verificado,version_sesion FROM usuarios"
                                            + " LIMIT 1")
                                .close();
                        st.executeQuery("SELECT administrador_principal_id FROM auth_setup WHERE id=1").close();
                    }
                    try (ResultSet column = connection.getMetaData().getColumns(
                            connection.getCatalog(), null, "cotizaciones", "cliente_id")) {
                        if (!column.next() || column.getInt("NULLABLE") != DatabaseMetaData.columnNullable)
                            throw new IllegalStateException(
                                    "para cotizar sin cliente, aplica la migración "
                                    + "007-cotizaciones-sin-cliente.sql a la base administrativa.");
                    }
                    System.out.println("MySQL: conexión y esquema administrativo comprobados.");
                } catch (SQLException error) {
                    if (error.getErrorCode() == 1045 || "28000".equals(error.getSQLState()))
                        throw new IllegalStateException(
                                "MySQL rechazó las credenciales. Revisa DB_USER y DB_PASSWORD en"
                                    + " .env y las variables de la terminal que las reemplacen.");
                    if (error.getErrorCode() == 1049)
                        throw new IllegalStateException(
                                "la base de DB_URL no existe. Crea angur_gestion_admin ejecutando"
                                    + " src/main/resources/db/schema.sql.");
                    if (error.getErrorCode() == 1054)
                        throw new IllegalStateException(
                                "faltan columnas de autenticación. Revisa la migración"
                                    + " 001-autenticacion-web.sql.");
                    throw new IllegalStateException(
                            "no se pudo comprobar MySQL. Verifica que esté iniciado y revisa"
                                + " DB_URL; no se modificaron datos.");
                }
            }
        } finally {
            Files.deleteIfExists(driverJar);
        }
    }

    private void services() throws Exception {
        if (!docker) return;
        URI db =
                URI.create(
                        env.getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/angur_gestion_admin")
                                .substring(5));
        if (!local(db)) return;
        List<String> names = new ArrayList<>();
        if (!listening(db.getPort() < 0 ? 3306 : db.getPort())) names.add("mysql");
        String host = env.getOrDefault("SMTP_HOST", "localhost");
        if ("true".equalsIgnoreCase(env.getOrDefault("AUTH_MAIL_ENABLED", "false"))
                && (host.equals("localhost") || host.equals("127.0.0.1"))
                && env.getOrDefault("SMTP_PORT", "1025").equals("1025")
                && !listening(1025)) names.add("mailpit");
        if (names.isEmpty()) return;
        List<String> cmd = new ArrayList<>(List.of("docker", "compose", "up", "-d", "--wait"));
        cmd.addAll(names);
        System.out.println("Iniciando servicios locales: " + String.join(", ", names));
        try {
            if (process(cmd).start().waitFor() != 0)
                throw new IllegalStateException(
                        "Docker no pudo iniciar los servicios. Revisa Docker Desktop, .env y los"
                            + " puertos. Usa --no-docker para MySQL propio.");
        } catch (IOException missing) {
            throw new IllegalStateException(
                    "instala y abre Docker Desktop, o configura MySQL propio con --no-docker.");
        }
    }

    private void load() throws IOException {
        Path file = root.resolve(".env");
        if (!Files.exists(file)) return;
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int index = line.indexOf('=');
            if (index < 1)
                throw new IllegalArgumentException("hay una línea sin KEY=VALUE en .env.");
            String key = line.substring(0, index).strip();
            if (!key.matches(
                    "(?:DB_|MYSQL_|SMTP_|AUTH_|ANGUR_)[A-Z0-9_]+|SERVER_PORT|SESSION_COOKIE_SECURE|MAVEN_ARGS|MAVEN_OPTS|MAVEN_USER_HOME"))
                continue;
            String value = line.substring(index + 1).strip();
            if (value.length() >= 2
                    && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))))
                value = value.substring(1, value.length() - 1);
            env.putIfAbsent(key, value);
        }
    }

    private void createEnvironment() throws IOException {
        String content =
                Files.readString(root.resolve(".env.example"), StandardCharsets.UTF_8)
                        .replace("reemplaza-esta-clave-local", password())
                        .replace("reemplaza-esta-otra-clave-local", password());
        Path file = root.resolve(".env");
        if (Files.getFileStore(root).supportsFileAttributeView("posix")) {
            Files.createFile(
                    file,
                    PosixFilePermissions.asFileAttribute(
                            PosixFilePermissions.fromString("rw-------")));
            Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.WRITE);
        } else
            Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        System.out.println(
                "Se creó .env con claves aleatorias para desarrollo. Este archivo está excluido de"
                    + " Git.");
    }

    private static String password() {
        byte[] data = new byte[24];
        new SecureRandom().nextBytes(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private ProcessBuilder process(List<String> cmd) {
        var p = new ProcessBuilder(cmd).directory(root.toFile()).inheritIO();
        p.environment().putAll(env);
        return p;
    }

    private boolean ready() {
        try {
            var r =
                    http.send(
                            HttpRequest.newBuilder(URI.create(url()))
                                    .timeout(Duration.ofSeconds(2))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            return r.statusCode() == 200
                    && r.body().contains("Angur")
                    && r.body().contains("name=\"identifier\"");
        } catch (Exception error) {
            return false;
        }
    }

    private static boolean listening(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            return true;
        } catch (IOException error) {
            return false;
        }
    }

    private String url() {
        return "http://localhost:" + port + "/login";
    }

    private static boolean windows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean local(URI uri) {
        return "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
    }

    private void open() {
        if (!browser) return;
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Desktop.getDesktop().browse(URI.create(url()));
            else if (!windows()) new ProcessBuilder("xdg-open", url()).start();
            else new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url()).start();
        } catch (IOException unavailable) {
            System.out.println("Abre el navegador manualmente en " + url());
        }
    }
}
