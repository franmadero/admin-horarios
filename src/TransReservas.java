import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Scanner;

public class TransReservas {

    //Reservar salón (elige puntual o periódica)
    public static void reservarSalon() {
        Scanner sc = new Scanner(System.in);

        System.out.println("\n--- T7: Reservar un salón ---");
        System.out.println("¿Qué tipo de reservación desea hacer?");
        System.out.println("1) Reserva puntual (fecha y hora exactas)");
        System.out.println("2) Reserva periódica (mismo día cada semana en un período)");
        System.out.print("Opción: ");

        int opcion;
        try {
            opcion = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Opción inválida.");
            return;
        }

        switch (opcion) {
            case 1 -> reservarPuntual(sc);
            case 2 -> reservarPeriodica(sc);
            default -> System.out.println("Opción no válida.");
        }
    }

    // 7a – Reserva puntual
    private static void reservarPuntual(Scanner sc) {

        System.out.println("\n--- T7a: Reserva puntual de un salón ---");

        System.out.print("Ingrese ID del salón (ej. HU101): ");
        String idSalon = sc.nextLine().trim();

        System.out.print("Ingrese su ID de usuario: ");
        int idUsuario = Integer.parseInt(sc.nextLine().trim());

        System.out.print("Ingrese la fecha (YYYY-MM-DD): ");
        String fechaStr = sc.nextLine().trim();

        System.out.print("Ingrese la hora (HH:MM): ");
        String horaStr = sc.nextLine().trim();

        System.out.print("Ingrese la duración en minutos: ");
        int duracion = Integer.parseInt(sc.nextLine().trim());

        LocalDate fecha;
        LocalTime hora;
        try {
            fecha = LocalDate.parse(fechaStr);   // ej. 2025-12-02
            hora = LocalTime.parse(horaStr);     // ej. 14:30
        } catch (Exception e) {
            System.out.println("Fecha u hora inválidas. Formatos: YYYY-MM-DD y HH:MM");
            return;
        }

        LocalDateTime inicio = fecha.atTime(hora);
        LocalDateTime fin = inicio.plusMinutes(duracion);

        Timestamp tsInicio = Timestamp.valueOf(inicio);
        Timestamp tsFin = Timestamp.valueOf(fin);

        // Día de la semana 1..7
        int diaSem = diaSemanaJavaToInt(fecha.getDayOfWeek());

        try (Connection conn = Conexion.getConnection()) {

            conn.setAutoCommit(false);

            //  Revisar conflictos con HORARIO
            String sqlHor = """
                    SELECT h.Hora,
                           h.Minuto,
                           h.Duracion
                    FROM Horario h
                    JOIN Periodo p ON h.Titulo = p.Titulo
                    WHERE h.IDSalon = ?
                      AND h.DiaSem = ?
                      AND ? BETWEEN p.FechaInicio AND p.FechaFin
                    """;

            try (PreparedStatement psHor = conn.prepareStatement(sqlHor)) {
                psHor.setString(1, idSalon);
                psHor.setInt(2, diaSem);
                psHor.setDate(3, Date.valueOf(fecha));

                try (ResultSet rsH = psHor.executeQuery()) {
                    while (rsH.next()) {
                        int hCur = rsH.getInt("Hora");
                        int mCur = rsH.getInt("Minuto");
                        int durCur = rsH.getInt("Duracion");

                        LocalTime curInicio = LocalTime.of(hCur, mCur);
                        LocalTime curFin = curInicio.plusMinutes(durCur);

                        LocalDateTime curIniDT = fecha.atTime(curInicio);
                        LocalDateTime curFinDT = fecha.atTime(curFin);

                        boolean seTraslapa =
                                inicio.isBefore(curFinDT) &&
                                        fin.isAfter(curIniDT);

                        if (seTraslapa) {
                            System.out.println(" El salón está ocupado por un curso en HORARIO en ese horario.");
                            conn.rollback();
                            return;
                        }
                    }
                }
            }

            //  Revisar conflictos con otras RESERVACIONES
            String sqlResConf = """
                    SELECT 1
                    FROM Reservacion r
                    WHERE r.IDSalon = ?
                      AND DATE(r.FechaHora) = ?
                      AND ? < DATE_ADD(r.FechaHora, INTERVAL r.Duracion MINUTE)
                      AND ? > r.FechaHora
                    LIMIT 1
                    """;

            try (PreparedStatement psRes = conn.prepareStatement(sqlResConf)) {
                psRes.setString(1, idSalon);
                psRes.setDate(2, Date.valueOf(fecha));
                psRes.setTimestamp(3, tsInicio); // inicio nueva
                psRes.setTimestamp(4, tsFin);    // fin nueva

                try (ResultSet rs = psRes.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Ya existe una reservación que se traslapa con ese horario.");
                        conn.rollback();
                        return;
                    }
                }
            }

            //  Generar nuevo ID y hacer INSERT
            int nuevoIdRes = generarNuevoIdReservacion(conn);

            String sqlIns = """
                    INSERT INTO Reservacion (IDreservacion, Duracion, FechaHora, IDSalon, IDUsuario)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement psIns = conn.prepareStatement(sqlIns)) {
                psIns.setInt(1, nuevoIdRes);
                psIns.setInt(2, duracion);
                psIns.setTimestamp(3, tsInicio);
                psIns.setString(4, idSalon);
                psIns.setInt(5, idUsuario);
                psIns.executeUpdate();
            }

            conn.commit();
            System.out.println(" Reservación puntual creada correctamente con ID " + nuevoIdRes);

        } catch (Exception e) {
            System.err.println("Error al crear reservación puntual.");
            e.printStackTrace();
        }
    }

    // 7b – Reserva periódica
    private static void reservarPeriodica(Scanner sc) {

        System.out.println("\n--- T7b: Reserva periódica de un salón ---");

        System.out.print("Ingrese ID del salón (ej. HU101): ");
        String idSalon = sc.nextLine().trim();

        System.out.print("Ingrese su ID de usuario: ");
        int idUsuario = Integer.parseInt(sc.nextLine().trim());

        System.out.print("Ingrese el Título del período (ej. PRIMAVERA-25): ");
        String tituloPeriodo = sc.nextLine().trim();

        System.out.print("Ingrese el día de la semana (1=Lun ... 7=Dom): ");
        int diaSem;
        try {
            diaSem = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Día de semana inválido.");
            return;
        }
        if (diaSem < 1 || diaSem > 7) {
            System.out.println("Día de semana fuera de rango (1 a 7).");
            return;
        }

        System.out.print("Ingrese la hora (HH:MM): ");
        String horaStr = sc.nextLine().trim();

        System.out.print("Ingrese la duración en minutos: ");
        int duracion = Integer.parseInt(sc.nextLine().trim());

        LocalTime hora;
        try {
            hora = LocalTime.parse(horaStr);
        } catch (Exception e) {
            System.out.println("Hora inválida. Use HH:MM, ej. 14:00");
            return;
        }

        LocalTime finHora = hora.plusMinutes(duracion);

        try (Connection conn = Conexion.getConnection()) {

            conn.setAutoCommit(false);

            // Obtener fechas del período
            LocalDate fechaInicioPeriodo = null;
            LocalDate fechaFinPeriodo = null;

            String sqlPer = "SELECT FechaInicio, FechaFin FROM Periodo WHERE Titulo = ?";
            try (PreparedStatement psPer = conn.prepareStatement(sqlPer)) {
                psPer.setString(1, tituloPeriodo);
                try (ResultSet rsPer = psPer.executeQuery()) {
                    if (rsPer.next()) {
                        fechaInicioPeriodo = rsPer.getDate("FechaInicio").toLocalDate();
                        fechaFinPeriodo = rsPer.getDate("FechaFin").toLocalDate();
                    } else {
                        System.out.println("No existe un período con ese título.");
                        conn.rollback();
                        return;
                    }
                }
            }

            //  Revisar conflictos con HORARIO (mismo salón, día y periodo)
            String sqlHor = """
                    SELECT h.Hora,
                           h.Minuto,
                           h.Duracion
                    FROM Horario h
                    WHERE h.IDSalon = ?
                      AND h.DiaSem = ?
                      AND h.Titulo = ?
                    """;

            try (PreparedStatement psHor = conn.prepareStatement(sqlHor)) {
                psHor.setString(1, idSalon);
                psHor.setInt(2, diaSem);
                psHor.setString(3, tituloPeriodo);

                try (ResultSet rsH = psHor.executeQuery()) {
                    while (rsH.next()) {
                        int hCur = rsH.getInt("Hora");
                        int mCur = rsH.getInt("Minuto");
                        int durCur = rsH.getInt("Duracion");

                        LocalTime curIni = LocalTime.of(hCur, mCur);
                        LocalTime curFin = curIni.plusMinutes(durCur);

                        boolean seTraslapa =
                                hora.isBefore(curFin) &&
                                        finHora.isAfter(curIni);

                        if (seTraslapa) {
                            System.out.println("⚠ El salón está ocupado por un curso en HORARIO en ese horario durante el período.");
                            conn.rollback();
                            return;
                        }
                    }
                }
            }

            // Encontrar la primera fecha del período que caiga en ese diaSem
            LocalDate fecha = fechaInicioPeriodo;
            while (diaSemanaJavaToInt(fecha.getDayOfWeek()) != diaSem) {
                fecha = fecha.plusDays(1);
            }

            //  Para cada semana dentro del período:
            while (!fecha.isAfter(fechaFinPeriodo)) {

                LocalDateTime inicio = fecha.atTime(hora);
                LocalDateTime fin = fecha.atTime(finHora);

                Timestamp tsInicio = Timestamp.valueOf(inicio);
                Timestamp tsFin = Timestamp.valueOf(fin);

                // Revisar conflictos con RESERVACION
                String sqlResConf = """
                        SELECT 1
                        FROM Reservacion r
                        WHERE r.IDSalon = ?
                          AND DATE(r.FechaHora) = ?
                          AND ? < DATE_ADD(r.FechaHora, INTERVAL r.Duracion MINUTE)
                          AND ? > r.FechaHora
                        LIMIT 1
                        """;

                try (PreparedStatement psRes = conn.prepareStatement(sqlResConf)) {
                    psRes.setString(1, idSalon);
                    psRes.setDate(2, Date.valueOf(fecha));
                    psRes.setTimestamp(3, tsInicio); // inicio nueva
                    psRes.setTimestamp(4, tsFin);    // fin nueva

                    try (ResultSet rs = psRes.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("⚠ Conflicto de reservación en la fecha " + fecha +
                                    ". No se puede realizar la reserva periódica completa.");
                            conn.rollback();
                            return;
                        }
                    }
                }

                // Insertar reservación para esa fecha
                int nuevoIdRes = generarNuevoIdReservacion(conn);

                String sqlIns = """
                        INSERT INTO Reservacion (IDreservacion, Duracion, FechaHora, IDSalon, IDUsuario)
                        VALUES (?, ?, ?, ?, ?)
                        """;

                try (PreparedStatement psIns = conn.prepareStatement(sqlIns)) {
                    psIns.setInt(1, nuevoIdRes);
                    psIns.setInt(2, duracion);
                    psIns.setTimestamp(3, tsInicio);
                    psIns.setString(4, idSalon);
                    psIns.setInt(5, idUsuario);
                    psIns.executeUpdate();
                }

                // siguiente semana
                fecha = fecha.plusDays(7);
            }

            conn.commit();
            System.out.println("Reservación periódica creada correctamente para todas las semanas del período.");

        } catch (Exception e) {
            System.err.println("Error al crear reservación periódica.");
            e.printStackTrace();
        }
    }

    // Generar nuevo IDreservacion = MAX + 1
    private static int generarNuevoIdReservacion(Connection conn) throws Exception {
        String sql = "SELECT MAX(IDreservacion) FROM Reservacion";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int max = rs.getInt(1);
                return max + 1;
            } else {
                return 1;
            }
        }
    }

    //  Día de la semana Java → 1..7
    private static int diaSemanaJavaToInt(java.time.DayOfWeek d) {
        return switch (d) {
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
            case SUNDAY -> 7;
        };
    }


        //  Anular reservación (elige puntual o por intervalo)
        public static void anularReservacion() {
            Scanner sc = new Scanner(System.in);

            System.out.println("\n--- T8: Anular reservación ---");
            System.out.println("¿Qué tipo de cancelación desea hacer?");
            System.out.println("1) Cancelación puntual");
            System.out.println("2) Cancelación por intervalo (reservas periódicas de un usuario)");
            System.out.print("Opción: ");

            int opcion;
            try {
                opcion = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Opción inválida.");
                return;
            }

            switch (opcion) {
                case 1 -> anularReservacionPuntual(sc);
                case 2 -> anularReservacionIntervalo(sc);
                default -> System.out.println("Opción no válida.");
            }
        }
        //  Cancelación puntual: salón + fecha + hora exactas
        private static void anularReservacionPuntual(Scanner sc) {

            System.out.println("\n--- 8a: Cancelación puntual ---");

            System.out.print("Ingrese ID del salón (ej. HU101): ");
            String idSalon = sc.nextLine().trim();

            System.out.print("Ingrese la fecha de la reservación (YYYY-MM-DD): ");
            String fechaStr = sc.nextLine().trim();

            System.out.print("Ingrese la hora de la reservación (HH:MM): ");
            String horaStr = sc.nextLine().trim();

            LocalDate fecha;
            LocalTime hora;
            try {
                fecha = LocalDate.parse(fechaStr);
                hora = LocalTime.parse(horaStr);
            } catch (Exception e) {
                System.out.println("Fecha u hora inválidas. Formatos: YYYY-MM-DD y HH:MM");
                return;
            }

            LocalDateTime fechaHora = fecha.atTime(hora);
            Timestamp ts = Timestamp.valueOf(fechaHora);

            try (Connection conn = Conexion.getConnection()) {

                conn.setAutoCommit(false);

                // Vrificar si existe esa reservación
                String sqlCheck = """
                    SELECT IDreservacion
                    FROM Reservacion
                    WHERE IDSalon = ?
                      AND FechaHora = ?
                    """;

                Integer idResEncontrada = null;

                try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                    psCheck.setString(1, idSalon);
                    psCheck.setTimestamp(2, ts);

                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) {
                            idResEncontrada = rs.getInt("IDreservacion");
                        }
                    }
                }

                if (idResEncontrada == null) {
                    System.out.println("No se encontró una reservación exactamente en ese salón, fecha y hora.");
                    conn.rollback();
                    return;
                }

                // Eiminar la reservación encontrada
                String sqlDel = """
                    DELETE FROM Reservacion
                    WHERE IDreservacion = ?
                    """;

                try (PreparedStatement psDel = conn.prepareStatement(sqlDel)) {
                    psDel.setInt(1, idResEncontrada);
                    int filas = psDel.executeUpdate();

                    if (filas > 0) {
                        conn.commit();
                        System.out.println("Reservación " + idResEncontrada + " anulada correctamente.");
                    } else {
                        System.out.println("No se pudo eliminar la reservación (ninguna fila afectada).");
                        conn.rollback();
                    }
                }

            } catch (Exception e) {
                System.err.println("Error al anular reservación puntual.");
                e.printStackTrace();
            }
        }
        //  Cancelación por intervalo de fechas (solo reservas de un usuario, salón y hora)
        private static void anularReservacionIntervalo(Scanner sc) {

            System.out.println("\n--- 8b: Cancelación por intervalo de fechas (reservas periódicas de un usuario) ---");

            System.out.print("Ingrese su ID de usuario: ");
            int idUsuario;
            try {
                idUsuario = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("ID de usuario inválido.");
                return;
            }

            System.out.print("Ingrese ID del salón (ej. HU101): ");
            String idSalon = sc.nextLine().trim();

            System.out.print("Ingrese fecha inicial (YYYY-MM-DD): ");
            String fIniStr = sc.nextLine().trim();

            System.out.print("Ingrese fecha final (YYYY-MM-DD): ");
            String fFinStr = sc.nextLine().trim();

            System.out.print("Ingrese la hora de la reservación (HH:MM): ");
            String horaStr = sc.nextLine().trim();

            LocalDate fIni, fFin;
            LocalTime hora;
            try {
                fIni = LocalDate.parse(fIniStr);
                fFin = LocalDate.parse(fFinStr);
            } catch (Exception e) {
                System.out.println("Fechas inválidas. Use formato YYYY-MM-DD.");
                return;
            }

            if (fFin.isBefore(fIni)) {
                System.out.println("La fecha final no puede ser anterior a la inicial.");
                return;
            }

            try {
                hora = LocalTime.parse(horaStr);
            } catch (Exception e) {
                System.out.println("Hora inválida. Use HH:MM, ej. 14:00");
                return;
            }

            try (Connection conn = Conexion.getConnection()) {

                conn.setAutoCommit(false);

                // Borramos SOLO las reservas de ese usuario, en ese salón, en ese horario, dentro del intervalo
                String sqlDel = """
                    DELETE FROM Reservacion
                    WHERE IDUsuario = ?
                      AND IDSalon = ?
                      AND DATE(FechaHora) BETWEEN ? AND ?
                      AND TIME(FechaHora) = ?
                    """;

                int borradas;

                try (PreparedStatement psDel = conn.prepareStatement(sqlDel)) {
                    psDel.setInt(1, idUsuario);
                    psDel.setString(2, idSalon);
                    psDel.setDate(3, java.sql.Date.valueOf(fIni));
                    psDel.setDate(4, java.sql.Date.valueOf(fFin));
                    psDel.setTime(5, java.sql.Time.valueOf(hora));

                    borradas = psDel.executeUpdate();
                }

                conn.commit();

                System.out.println(" Se cancelaron " + borradas +
                        " reservaciones del usuario " + idUsuario +
                        " en el salón " + idSalon +
                        " entre " + fIni + " y " + fFin +
                        " a las " + hora + ".");

            } catch (Exception e) {
                System.err.println("Error al anular reservaciones por intervalo.");
                e.printStackTrace();
            }
        }
    }


