package com.actividad2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Conexión a PostgreSQL con un grupo (pool) de conexiones reutilizables.
 *
 * Abrir una conexión por petición saturaría el servidor si muchas personas
 * responden a la vez (PostgreSQL acepta unas 100 conexiones); con el pool, un
 * número fijo de conexiones atiende todas las peticiones por turno.
 *
 * Configuración: archivo db.properties en la carpeta backend (no se sube a git)
 * o variables de entorno DB_URL, DB_USER, DB_PASSWORD.
 */
public class Db {

    private static final int TAMANO_POOL = 10;
    private static BlockingQueue<Connection> pool;
    private static String url, usuario, clave;

    public interface Trabajo<T> {
        T con(Connection c) throws SQLException;
    }

    public static void iniciar() throws IOException, SQLException {
        Properties p = new Properties();
        Path archivo = Path.of("db.properties");
        if (Files.exists(archivo)) {
            try (var r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                p.load(r);
            }
        }
        url = valor(p, "url", "DB_URL", "jdbc:postgresql://localhost:5432/actividad2");
        usuario = valor(p, "user", "DB_USER", "actividad2");
        clave = valor(p, "password", "DB_PASSWORD", "");

        pool = new ArrayBlockingQueue<>(TAMANO_POOL);
        for (int i = 0; i < TAMANO_POOL; i++) pool.add(nueva());
        crearEsquema();
    }

    private static String valor(Properties p, String clave, String variable, String porDefecto) {
        String v = p.getProperty(clave);
        if (v == null || v.isBlank()) v = System.getenv(variable);
        return v == null || v.isBlank() ? porDefecto : v.trim();
    }

    private static Connection nueva() throws SQLException {
        return DriverManager.getConnection(url, usuario, clave);
    }

    /** Toma una conexión del pool, ejecuta el trabajo y la devuelve. */
    public static <T> T usar(Trabajo<T> trabajo) {
        Connection c = tomar();
        try {
            return trabajo.con(c);
        } catch (SQLException e) {
            throw new RuntimeException("Error de base de datos: " + e.getMessage(), e);
        } finally {
            devolver(c);
        }
    }

    /** Igual que usar(), pero todo el trabajo se confirma junto o no se guarda nada (transacción). */
    public static <T> T transaccion(Trabajo<T> trabajo) {
        Connection c = tomar();
        try {
            c.setAutoCommit(false);
            T r = trabajo.con(c);
            c.commit();
            return r;
        } catch (SQLException | RuntimeException e) {
            try {
                c.rollback();
            } catch (SQLException ignorada) {
                // la conexión se reemplaza al devolverla si quedó inválida
            }
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException("Error de base de datos: " + e.getMessage(), e);
        } finally {
            try {
                c.setAutoCommit(true);
            } catch (SQLException ignorada) {
                // idem
            }
            devolver(c);
        }
    }

    private static Connection tomar() {
        try {
            Connection c = pool.poll(10, TimeUnit.SECONDS);
            if (c == null) throw new IllegalStateException("La base de datos está ocupada, intente de nuevo");
            if (!c.isValid(2)) {
                c.close();
                c = nueva();
            }
            return c;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación interrumpida");
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo conectar a la base de datos: " + e.getMessage(), e);
        }
    }

    private static void devolver(Connection c) {
        if (!pool.offer(c)) {
            try {
                c.close();
            } catch (SQLException ignorada) {
                // nada que hacer
            }
        }
    }

    private static void crearEsquema() throws IOException {
        String sql;
        try (InputStream in = Db.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) throw new IOException("No se encontró schema.sql");
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        StringBuilder sinComentarios = new StringBuilder();
        for (String linea : sql.split("\n")) {
            int i = linea.indexOf("--");
            sinComentarios.append(i >= 0 ? linea.substring(0, i) : linea).append('\n');
        }
        usar(c -> {
            try (Statement st = c.createStatement()) {
                for (String sentencia : sinComentarios.toString().split(";")) {
                    if (!sentencia.isBlank()) st.execute(sentencia);
                }
            }
            return null;
        });
    }
}
