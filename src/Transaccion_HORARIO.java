import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Transaccion_HORARIO {

    private static final Scanner sc = new Scanner(System.in);

    private static boolean existeCurso(String clave, int secc) {
        String sql = "SELECT 1 FROM Curso WHERE Clave = ? AND Secc = ?";
        try (Connection conn = Conexion.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clave);
            ps.setInt(2, secc);

            return ps.executeQuery().next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private static String obtenerCarreraDelAlumno(int idUsuario) {
        String sql = "SELECT Carrera FROM alumno WHERE IDUsuario = ?";

        try (Connection conn = Conexion.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("Carrera");
            }

        } catch (Exception e) {
            System.err.println("Error al obtener la carrera del alumno.");
            e.printStackTrace();
        }

        return null;
    }
    private static boolean existeSalon(String salon) {
        String sql = "SELECT 1 FROM Salon WHERE IDSalon = ?";
        try (Connection conn = Conexion.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, salon);
            return ps.executeQuery().next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private static LocalDate[] obtenerFechasPeriodo(String periodo) {
        String sql = "SELECT FechaInicio, FechaFin FROM Periodo WHERE Titulo = ?";
        try (Connection conn = Conexion.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, periodo);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                LocalDate ini = rs.getDate("FechaInicio").toLocalDate();
                LocalDate fin = rs.getDate("FechaFin").toLocalDate();
                System.out.println(ini);
                System.out.println(fin);
                return new LocalDate[]{ini, fin};
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private static List<LocalDate> generarFechasPorDia(LocalDate ini, LocalDate fin, int diaSemana) {
        List<LocalDate> fechas = new ArrayList<>();

        LocalDate fecha = ini.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(diaSemana)));

        while (!fecha.isAfter(fin)) {
            fechas.add(fecha);
            fecha = fecha.plusWeeks(1);
        }

        return fechas;
    }
    private static boolean salonDisponible(String idSalon, List<LocalDate> fechas, int hora, int minuto, int duracion){
        String sql = """
            SELECT COUNT(*)
            FROM reservacion
            WHERE IDSalon = ?
            AND FechaHora < ?
            AND ADDTIME(FechaHora, SEC_TO_TIME(Duracion*60)) > ?
        """;

        try (Connection conn = Conexion.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            for (LocalDate fecha : fechas) {

                // Crear datetime "YYYY-MM-DD HH:MM:SS"
                String inicio = fecha.toString() + " " + String.format("%02d:%02d:00", hora, minuto);

                ps.setString(1, idSalon);
                ps.setString(2, inicio);
                ps.setString(3, inicio);

                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return false; // hay choque
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true; // libre
    }


    private static void insertarHorarioYReservaciones(String clave, int secc, String periodo,
                                                  int dia, int hora, int minuto,
                                                  int duracion, int semestre, String salon) {

        String sqlInsertHorario = """
            INSERT INTO Horario
            (Clave, Secc, Titulo, DiaSem, Hora, Minuto, Duracion, Semestre, IDSalon)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String sqlInsertReservacion = """
            INSERT INTO Reservacion
            (IDSalon, Fecha, Hora, Minuto, Duracion, Clave, Secc)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        Connection conn = null;
        PreparedStatement psHorario = null;
        PreparedStatement psReserv = null;

        try {
            conn = Conexion.getConnection();
            conn.setAutoCommit(false); 

            LocalDate[] rango = obtenerFechasPeriodo(periodo);

            if (rango == null) {
                System.out.println("Error: El período no existe.");
                return; // no iniciar transacción
            }

            LocalDate ini = rango[0];
            LocalDate fin = rango[1];

            // Generar fechas en las que aplica la clase
            List<LocalDate> fechas = generarFechasPorDia(ini, fin, dia);

            psHorario = conn.prepareStatement(sqlInsertHorario);

            psHorario.setString(1, clave);
            psHorario.setInt(2, secc);
            psHorario.setString(3, periodo);
            psHorario.setInt(4, dia);
            psHorario.setInt(5, hora);
            psHorario.setInt(6, minuto);
            psHorario.setInt(7, duracion);
            psHorario.setInt(8, semestre);
            psHorario.setString(9, salon);

            psHorario.executeUpdate();

            psReserv = conn.prepareStatement(sqlInsertReservacion);

            for (LocalDate fecha : fechas) {
                psReserv.setString(1, salon);
                psReserv.setDate(2, java.sql.Date.valueOf(fecha));
                psReserv.setInt(3, hora);
                psReserv.setInt(4, minuto);
                psReserv.setInt(5, duracion);
                psReserv.setString(6, clave);
                psReserv.setInt(7, secc);

                psReserv.addBatch();
            }

            psReserv.executeBatch();

            System.out.println("\n---------------------------------------------");
            System.out.println(" Se va a registrar lo siguiente:");
            System.out.println(" Curso: " + clave + "  Sección: " + secc);
            System.out.println(" Día: " + dia + "  Hora: " + hora + ":" + minuto);
            System.out.println(" Período: " + periodo);
            System.out.println(" Salón: " + salon);
            System.out.println(" Fechas generadas: " + fechas.size());
            System.out.println("---------------------------------------------");

            System.out.print("¿Confirmar registro? (s/n): ");
            String resp = sc.nextLine().trim().toLowerCase();

            if (resp.equals("s")) {
                conn.commit();
                System.out.println("Transacción confirmada y guardada.");
            } else {
                conn.rollback();
                System.out.println("Transacción cancelada.");
            }

        } catch (Exception e) {
            System.err.println("Error inesperado. Realizando rollback...");
            e.printStackTrace();

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ex) {
                    System.err.println("Error durante el rollback:");
                    ex.printStackTrace();
                }
            }

        } finally {
            // Cerrar statements
            if (psReserv != null) try { psReserv.close(); } catch (Exception ignored) {}
            if (psHorario != null) try { psHorario.close(); } catch (Exception ignored) {}

            // Restaurar auto-commit y cerrar conexión
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (Exception ignored) {}
            }
        }
    }



    // =========================
    // 5.A Consulta de Horario Completo
    // =========================
    public static void verHorarioCompleto(int idUsuario) {

        // Verificar tipo
        String tipo = MenuHorarios.obtenerTipoUsuario(idUsuario);

        if (tipo == null) {
            System.out.println("Usuario no encontrado.");
            return;
        }

        String sql = """
            SELECT 
                h.Clave,
                h.Secc,
                c.Titulo AS CursoTitulo,
                h.IDSalon,
                h.DiaSem,
                h.Hora,
                h.Minuto,
                h.Duracion,
                h.Semestre,
                h.Titulo AS Periodo
            FROM horario h
            INNER JOIN curso c ON h.Clave = c.Clave AND h.Secc = c.Secc
            ORDER BY h.Semestre, h.DiaSem, h.Hora, h.Minuto
        """;

        try (Connection conn = Conexion.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            System.out.println("\n===== HORARIO COMPLETO =====\n");

            while (rs.next()) {

                System.out.println("Curso: " + rs.getString("Clave") +
                                "-" + rs.getInt("Secc") +
                                " | " + rs.getString("CursoTitulo"));

                System.out.println("Periodo: " + rs.getString("Periodo"));
                System.out.println("Salón: " + rs.getString("IDSalon"));
                System.out.println("Día: " + rs.getInt("DiaSem"));
                System.out.println("Hora: " + rs.getInt("Hora") +
                                ":" + String.format("%02d", rs.getInt("Minuto")));
                System.out.println("Duración: " + rs.getInt("Duracion") + " min");
                System.out.println("Semestre: " + rs.getInt("Semestre"));
                System.out.println("------------------------------");
            }

        } catch (Exception e) {
            System.err.println("Error al consultar el horario completo.");
            e.printStackTrace();
        }
    }
    // ======================================
    // 6.A Consulta de Horario Parcial (Filtrado)
    // Solo profesores y administrativos
    // ======================================
    public static void verHorarioParcial(int idUsuario) {

        String tipo = MenuHorarios.obtenerTipoUsuario(idUsuario);

        if (tipo == null) {
            System.out.println("Usuario no encontrado.");
            return;
        }
        System.out.println("\n== Consulta de Horario Parcial ==");
        System.out.println("Puedes dejar cualquier campo vacío para no filtrarlo.\n");
        sc.nextLine(); // limpiar buffer
        //Filtros
        System.out.print("Filtrar por período (texto): ");
        String filtroPeriodo = sc.nextLine().trim();

        System.out.print("Filtrar por día de la semana (1-7): ");
        String filtroDia = sc.nextLine().trim();

        System.out.print("Filtrar por clave de curso: ");
        String filtroClave = sc.nextLine().trim();

        System.out.print("Filtrar por salón: ");
        String filtroSalon = sc.nextLine().trim();


        // Construcción dinámica del SQL
        StringBuilder sql = new StringBuilder("""
            SELECT 
                h.Clave,
                h.Secc,
                c.Titulo AS CursoTitulo,
                h.IDSalon,
                h.DiaSem,
                h.Hora,
                h.Minuto,
                h.Duracion,
                h.Semestre,
                h.Titulo AS Periodo
            FROM horario h
            INNER JOIN curso c ON h.Clave = c.Clave AND h.Secc = c.Secc
            WHERE 1=1
        """);

        // Lista para parámetros
        List<Object> params = new ArrayList<>();

        if (!filtroPeriodo.isEmpty()) {
            sql.append(" AND h.Titulo LIKE ? ");
            params.add("%" + filtroPeriodo + "%");
        }

        if (!filtroDia.isEmpty()) {
            sql.append(" AND h.DiaSem = ? ");
            params.add(Integer.parseInt(filtroDia));
        }

        if (!filtroClave.isEmpty()) {
            sql.append(" AND h.Clave = ? ");
            params.add(filtroClave);
        }

        if (!filtroSalon.isEmpty()) {
            sql.append(" AND h.IDSalon = ? ");
            params.add(filtroSalon);
        }

        sql.append("""
            ORDER BY h.Semestre, h.DiaSem, h.Hora, h.Minuto
        """);

        try (Connection conn = Conexion.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // Asignar los valores puestos por el usuario a la categoría correspondiente
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();

            System.out.println("\n===== RESULTADOS FILTRADOS =====\n");

            boolean hayResultados = false;

            while (rs.next()) {
                hayResultados = true;

                System.out.println("Curso: " + rs.getString("Clave") +
                                "-" + rs.getInt("Secc") +
                                " | " + rs.getString("CursoTitulo"));
                System.out.println("Período: " + rs.getString("Periodo"));
                System.out.println("Salón: " + rs.getString("IDSalon"));
                System.out.println("Día: " + rs.getInt("DiaSem"));
                System.out.println("Hora: " + rs.getInt("Hora") + ":" +
                                String.format("%02d", rs.getInt("Minuto")));
                System.out.println("Duración: " + rs.getInt("Duracion") + " min");
                System.out.println("Semestre: " + rs.getInt("Semestre"));
                System.out.println("------------------------------");
            }

            if (!hayResultados) {
                System.out.println("No se encontraron coincidencias con los filtros ingresados.");
            }

        } catch (Exception e) {
            System.err.println("Error al consultar horario parcial.");
            e.printStackTrace();
        }
    }
    // =============================================================
    // 5.B Consulta de horario completo del alumno (solo alumnos)
    // =============================================================
    public static void verHorarioAlumnoCompleto(int idUsuario) {

        String tipo = MenuHorarios.obtenerTipoUsuario(idUsuario);

        if (tipo == null) {
            System.out.println("Usuario no encontrado.");
            return;
        }

        String carrera = obtenerCarreraDelAlumno(idUsuario);

        if (carrera == null) {
            System.out.println("No se pudo determinar la carrera del alumno.");
            return;
        }

        System.out.println("\n== Horario completo de la carrera: " + carrera + " ==\n");

        String sql = """
            SELECT 
                h.Clave,
                h.Secc,
                c.Titulo AS CursoTitulo,
                h.IDSalon,
                h.DiaSem,
                h.Hora,
                h.Minuto,
                h.Duracion,
                h.Semestre,
                h.Titulo AS Periodo
            FROM horario h
            INNER JOIN curso c ON h.Clave = c.Clave AND h.Secc = c.Secc
            WHERE c.Carrera = ?
            ORDER BY h.Titulo, h.DiaSem, h.Hora, h.Minuto
        """;

        try (Connection conn = Conexion.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, carrera);
            ResultSet rs = ps.executeQuery();

            boolean hayResultados = false;

            while (rs.next()) {
                hayResultados = true;

                System.out.println("Curso: " + rs.getString("Clave") +
                                "-" + rs.getInt("Secc") +
                                " | " + rs.getString("CursoTitulo"));
                System.out.println("Período: " + rs.getString("Periodo"));
                System.out.println("Salón: " + rs.getString("IDSalon"));
                System.out.println("Día: " + rs.getInt("DiaSem"));
                System.out.println("Hora: " + rs.getInt("Hora") + ":" +
                                String.format("%02d", rs.getInt("Minuto")));
                System.out.println("Duración: " + rs.getInt("Duracion") + " min");
                System.out.println("Semestre: " + rs.getInt("Semestre"));
                System.out.println("------------------------------");
            }

            if (!hayResultados) {
                System.out.println("No hay horario registrado para esta carrera.");
            }

        } catch (Exception e) {
            System.err.println("Error al consultar el horario del alumno.");
            e.printStackTrace();
        }
    }
    // ================================================
    // 6.B Consulta de Horario Parcial del Alumno
    // Solo alumnos, filtrado por su carrera
    // ================================================
    public static void verHorarioParcialAlumno(int idUsuario) {

        String tipo = MenuHorarios.obtenerTipoUsuario(idUsuario);

        if (tipo == null) {
            System.out.println("Usuario no encontrado.");
            return;
        }

        if (!tipo.equals("Alumno")) {
            System.out.println("Solo los alumnos pueden usar esta opción.");
            return;
        }

        // Obtener carrera del alumno
        String carrera = obtenerCarreraDelAlumno(idUsuario);

        if (carrera == null) {
            System.out.println("No se pudo determinar la carrera del alumno.");
            return;
        }

        System.out.println("\n== Consulta de Horario Parcial del Alumno ==");
        System.out.println("Carrera detectada: " + carrera);
        System.out.println("Puedes dejar cualquier campo vacío para no filtrarlo.\n");

        sc.nextLine();  // limpiar buffer

        // Filtros opcionales
        System.out.print("Filtrar por período (texto): ");
        String filtroPeriodo = sc.nextLine().trim();

        System.out.print("Filtrar por día de la semana (1-7): ");
        String filtroDia = sc.nextLine().trim();

        System.out.print("Filtrar por clave de curso: ");
        String filtroClave = sc.nextLine().trim();

        System.out.print("Filtrar por salón: ");
        String filtroSalon = sc.nextLine().trim();


        // Construcción dinámica del SQL
        StringBuilder sql = new StringBuilder("""
            SELECT 
                h.Clave,
                h.Secc,
                c.Titulo AS CursoTitulo,
                h.IDSalon,
                h.DiaSem,
                h.Hora,
                h.Minuto,
                h.Duracion,
                h.Semestre,
                h.Titulo AS Periodo
            FROM horario h
            INNER JOIN curso c 
                ON h.Clave = c.Clave AND h.Secc = c.Secc
            WHERE c.Carrera = ?
        """);

        // Lista de parámetros
        List<Object> params = new ArrayList<>();
        params.add(carrera);

        // Filtros opcionales
        if (!filtroPeriodo.isEmpty()) {
            sql.append(" AND h.Titulo LIKE ? ");
            params.add("%" + filtroPeriodo + "%");
        }

        if (!filtroDia.isEmpty()) {
            sql.append(" AND h.DiaSem = ? ");
            params.add(Integer.parseInt(filtroDia));
        }

        if (!filtroClave.isEmpty()) {
            sql.append(" AND h.Clave = ? ");
            params.add(filtroClave);
        }

        if (!filtroSalon.isEmpty()) {
            sql.append(" AND h.IDSalon = ? ");
            params.add(filtroSalon);
        }

        sql.append("""
            ORDER BY h.Semestre, h.DiaSem, h.Hora, h.Minuto
        """);

        try (Connection conn = Conexion.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // Rellenar parámetros dinámicos
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();

            System.out.println("\n===== RESULTADOS FILTRADOS (Carrera: " + carrera + ") =====\n");

            boolean hay = false;

            while (rs.next()) {
                hay = true;

                System.out.println("Curso: " + rs.getString("Clave") +
                                "-" + rs.getInt("Secc") +
                                " | " + rs.getString("CursoTitulo"));
                System.out.println("Período: " + rs.getString("Periodo"));
                System.out.println("Salón: " + rs.getString("IDSalon"));
                System.out.println("Día: " + rs.getInt("DiaSem"));
                System.out.println("Hora: " + rs.getInt("Hora") + ":" +
                                String.format("%02d", rs.getInt("Minuto")));
                System.out.println("Duración: " + rs.getInt("Duracion") + " min");
                System.out.println("Semestre: " + rs.getInt("Semestre"));
                System.out.println("------------------------------");
            }

            if (!hay) {
                System.out.println("No se encontraron resultados con los filtros indicados.");
            }

        } catch (Exception e) {
            System.err.println("Error al consultar horario del alumno.");
            e.printStackTrace();
        }
    }
    // ====================================================
    // 9. Agregar Entrada al Horario
    // ====================================================
    public static void agregarEntradaHorario() {

        System.out.println("\n== AGREGAR ENTRADA AL HORARIO ==");

        sc.nextLine(); // limpiar buffer

        System.out.print("Clave del curso: ");
        String clave = sc.nextLine().trim();

        System.out.print("Sección: ");
        int secc = sc.nextInt();
        sc.nextLine();

        System.out.print("Período: ");
        String periodo = sc.nextLine().trim();

        System.out.print("Día de la semana (1=Lunes ... 7=Domingo): ");
        int dia = sc.nextInt();

        System.out.print("Hora (0-23): ");
        int hora = sc.nextInt();

        System.out.print("Minuto (0-59): ");
        int minuto = sc.nextInt();

        System.out.print("Duración en minutos: ");
        int duracion = sc.nextInt();

        System.out.print("Semestre: ");
        int semestre = sc.nextInt();
        sc.nextLine();

        System.out.print("Salón: ");
        String salon = sc.nextLine().trim();


        // === VALIDACIONES ===
        if (!existeCurso(clave, secc)) {
            System.out.println("Error: El curso no existe.");
            return;
        }

        if (!existeSalon(salon)) {
            System.out.println("Error: El salón no existe.");
            return;
        }

        LocalDate[] fechasPeriodo = obtenerFechasPeriodo(periodo);
        if (fechasPeriodo == null) {
            System.out.println(">" + periodo + "< (len=" + periodo.length() + ")");
            System.out.println("Error: El período no existe.");
            return;
        }

        LocalDate ini = fechasPeriodo[0];
        LocalDate fin = fechasPeriodo[1];

        if (dia < 1 || dia > 7) {
            System.out.println("Día de semana inválido.");
            return;
        }

        if (hora < 0 || hora > 23 || minuto < 0 || minuto > 59 || duracion <= 0) {
            System.out.println("Datos de hora/minuto/duración inválidos.");
            return;
        }

        // === GENERAR TODAS LAS FECHAS ===
        List<LocalDate> fechas = generarFechasPorDia(ini, fin, dia);

        // === VERIFICAR DISPONIBILIDAD ===
        try {
            if (!salonDisponible(salon, fechas, hora, minuto, duracion)) {
                System.out.println("Conflicto: El salón no está disponible en todas las fechas.");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error al consultar disponibilidad del salón.");
            return;
        }

        // === INSERTAR EN HORARIO Y RESERVACIONES ===
        insertarHorarioYReservaciones(clave, secc, periodo, dia, hora, minuto, duracion, semestre, salon);

        System.out.println("Entrada agregada exitosamente.");
    }
    public static void modificarHorario() {

        System.out.print("Clave del curso: ");
        String clave = sc.nextLine();

        System.out.print("Sección: ");
        int secc = sc.nextInt();
        sc.nextLine();

        System.out.print("Periodo (Titulo): ");
        String periodo = sc.nextLine();

        try (Connection conn = Conexion.getConnection()) {

            Horario actual = Horario.obtenerHorario(conn, clave, secc, periodo);
            if (actual == null) {
                System.out.println("No existe un horario para ese curso.");
                return;
            }

            // Solicitar nueva info
            System.out.print("Nuevo salón: ");
            String nuevoSalon = sc.nextLine();

            System.out.print("Nuevo día (1-7): ");
            int nuevoDia = sc.nextInt();

            System.out.print("Nueva hora: ");
            int nuevaHora = sc.nextInt();

            System.out.print("Nuevo minuto: ");
            int nuevoMin = sc.nextInt();

            System.out.print("Nueva duración (minutos): ");
            int nuevaDur = sc.nextInt();

            // Rango del período
            LocalDate[] rango = obtenerFechasPeriodo(periodo);
            if (rango == null) {
                System.out.println("El período no existe.");
                return;
            }

            List<LocalDate> fechas = generarFechasPorDia(rango[0], rango[1], nuevoDia);

            // ----------------- INICIA TRANSACCIÓN ------------------
            conn.setAutoCommit(false);

            boolean libre = salonDisponible(nuevoSalon, fechas, nuevaHora, nuevoMin, nuevaDur);

            if (!libre) {
                System.out.println("Conflicto: el salón NO está disponible.");
                conn.rollback();
                return;
            }

            // CONFIRMAR
            sc.nextLine(); // limpiar
            System.out.print("¿Confirmar cambios? (S/N): ");
            String resp = sc.nextLine();

            if (!resp.equalsIgnoreCase("s")) {
                conn.rollback();
                System.out.println("Cambios cancelados.");
                return;
            }

            // 1) Eliminar reservaciones previas
            String delRes = """
                DELETE FROM reservacion
                WHERE IDSalon = ? AND IDUsuario = 
                    (SELECT IDUsuario FROM curso WHERE Clave=? AND Secc=?)
            """;
            try (PreparedStatement ps = conn.prepareStatement(delRes)) {
                ps.setString(1, actual.salon);
                ps.setString(2, clave);
                ps.setInt(3, secc);
                ps.executeUpdate();
            }

            // 2) Actualizar el horario
            String upd = """
                UPDATE horario
                SET DiaSem=?, Hora=?, Minuto=?, Duracion=?, IDSalon=?
                WHERE Clave=? AND Secc=? AND Titulo=?
            """;
            try (PreparedStatement ps = conn.prepareStatement(upd)) {
                ps.setInt(1, nuevoDia);
                ps.setInt(2, nuevaHora);
                ps.setInt(3, nuevoMin);
                ps.setInt(4, nuevaDur);
                ps.setString(5, nuevoSalon);

                ps.setString(6, clave);
                ps.setInt(7, secc);
                ps.setString(8, periodo);

                ps.executeUpdate();
            }

            // 3) Crear nuevas reservaciones
            String ins = """
                INSERT INTO reservacion (Duracion, FechaHora, IDSalon, IDUsuario)
                VALUES (?, ?, ?, (SELECT IDUsuario FROM curso WHERE Clave=? AND Secc=?))
            """;

            try (PreparedStatement ps = conn.prepareStatement(ins)) {

                for (LocalDate f : fechas) {
                    LocalDateTime dt = f.atTime(nuevaHora, nuevoMin);

                    ps.setInt(1, nuevaDur);
                    ps.setString(2, dt.toString().replace("T", " "));
                    ps.setString(3, nuevoSalon);
                    ps.setString(4, clave);
                    ps.setInt(5, secc);

                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            System.out.println("Modificación realizada con éxito.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
