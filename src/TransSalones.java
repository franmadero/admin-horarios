import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class TransSalones {
    public static Scanner sc = new Scanner(System.in);
    //Consulta general de salones
    public static void consultaGeneral() {
        System.out.println("\n--- Consulta general de salones ---");

        String sql = "SELECT IDSalon, Capacidad, Tipo FROM Salon";

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("IDSalon\tCapacidad\tTipo");

            while (rs.next()) {
                String id = rs.getString("IDSalon");
                int cap = rs.getInt("Capacidad");
                String tipo = rs.getString("Tipo");

                System.out.println(id + "\t" + cap + "\t\t" + tipo);
            }

            System.out.println("\nFin de la consulta.");

        } catch (Exception e) {
            System.err.println("Error al consultar los salones.");
            e.printStackTrace();
        }
    }
    // Consulta filtrada de salones
    public static void consultaFiltrada() {

        System.out.println("\n--- Consulta filtrada de salones ---");
        System.out.println("¿Cómo desea filtrar?");
        System.out.println("1) Por ID de salón");
        System.out.println("2) Por capacidad mínima");
        System.out.println("3) Por tipo de salón (C, SC, A)");
        System.out.print("Seleccione una opción: ");

        int opcion;
        try {
            opcion = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Opción inválida.");
            return;
        }

        String sql = null;

        try (Connection conn = Conexion.getConnection()) {

            PreparedStatement ps = null;

            switch (opcion) {
                case 1:
                    System.out.print("Ingrese el ID del salón (ej. HU101): ");
                    String idSalon = sc.nextLine();
                    sql = "SELECT IDSalon, Capacidad, Tipo FROM Salon WHERE IDSalon = ?";
                    ps = conn.prepareStatement(sql);
                    ps.setString(1, idSalon);
                    break;

                case 2:
                    System.out.print("Ingrese la capacidad mínima: ");
                    int capMin = Integer.parseInt(sc.nextLine());
                    sql = "SELECT IDSalon, Capacidad, Tipo FROM Salon WHERE Capacidad >= ?";
                    ps = conn.prepareStatement(sql);
                    ps.setInt(1, capMin);
                    break;

                case 3:
                    System.out.print("Ingrese el tipo de salón (C, SC, A): ");
                    String tipo = sc.nextLine().trim();
                    sql = "SELECT IDSalon, Capacidad, Tipo FROM Salon WHERE Tipo = ?";
                    ps = conn.prepareStatement(sql);
                    ps.setString(1, tipo);
                    break;

                default:
                    System.out.println("Opción no válida.");
                    return;
            }

            try (ResultSet rs = ps.executeQuery()) {

                System.out.println("\nResultados:");
                System.out.println("IDSalon\tCapacidad\tTipo");

                boolean hayResultados = false;
                while (rs.next()) {
                    hayResultados = true;
                    String id = rs.getString("IDSalon");
                    int cap = rs.getInt("Capacidad");
                    String t = rs.getString("Tipo");

                    System.out.println(id + "\t" + cap + "\t\t" + t);
                }

                if (!hayResultados) {
                    System.out.println("No se encontraron salones con ese criterio.");
                }
            }

        } catch (Exception e) {
            System.err.println("Error en la consulta filtrada de salones.");
            e.printStackTrace();
        }
    }
    public static void consultaOcupacionSalon() {

        System.out.println("\n--- Consulta de ocupación de un salón ---");
        System.out.print("Ingrese el ID del salón (ej. HU101): ");
        String idSalon = sc.nextLine().trim();

        if (idSalon.isEmpty()) {
            System.out.println("ID de salón vacío. Operación cancelada.");
            return;
        }

        try (Connection conn = Conexion.getConnection()) {

            // RESERVACIONES
            String sqlRes = """
                    SELECT IDreservacion, FechaHora, Duracion, IDUsuario
                    FROM Reservacion
                    WHERE IDSalon = ?
                    ORDER BY FechaHora
                    """;

            try (PreparedStatement psRes = conn.prepareStatement(sqlRes)) {
                psRes.setString(1, idSalon);

                try (ResultSet rs = psRes.executeQuery()) {
                    System.out.println("\n--- Reservaciones puntuales ---");
                    System.out.println("IDRes\tFechaHora\t\tDuración(min)\tIDUsuario");

                    boolean hayRes = false;
                    while (rs.next()) {
                        hayRes = true;
                        int idRes = rs.getInt("IDreservacion");
                        String fechaHora = rs.getString("FechaHora");
                        int dur = rs.getInt("Duracion");
                        int idUsuario = rs.getInt("IDUsuario");

                        System.out.println(idRes + "\t" + fechaHora + "\t" + dur + "\t\t" + idUsuario);
                    }
                    if (!hayRes) {
                        System.out.println("No hay reservaciones puntuales para este salón.");
                    }
                }
            }

            //  OCUPACIONES
            String sqlHor = """
                    SELECT Clave, Secc, DiaSem, Hora, Minuto, Duracion, Titulo, Semestre
                    FROM Horario
                    WHERE IDSalon = ?
                    ORDER BY Titulo, DiaSem, Hora, Minuto
                    """;

            try (PreparedStatement psHor = conn.prepareStatement(sqlHor)) {
                psHor.setString(1, idSalon);

                try (ResultSet rsH = psHor.executeQuery()) {
                    System.out.println("\n--- Ocupaciones semanales por cursos (HORARIO) ---");
                    System.out.println("Clave\tSecc\tDía\tHora\tDur(min)\tPeríodo\tSem");

                    boolean hayHor = false;
                    while (rsH.next()) {
                        hayHor = true;

                        String clave = rsH.getString("Clave");
                        int secc = rsH.getInt("Secc");
                        int dia = rsH.getInt("DiaSem");
                        int hora = rsH.getInt("Hora");
                        int minuto = rsH.getInt("Minuto");
                        int dur = rsH.getInt("Duracion");
                        String periodo = rsH.getString("Titulo");   // aquí Titulo lo estoy tomando como período
                        int semestre = rsH.getInt("Semestre");

                        String diaTexto = diaSemanaTexto(dia);
                        String horaTexto = String.format("%02d:%02d", hora, minuto);

                        System.out.println(clave + "\t" + secc + "\t" + diaTexto + "\t" +
                                horaTexto + "\t" + dur + "\t\t" + periodo + "\t" + semestre);
                    }

                    if (!hayHor) {
                        System.out.println("No hay cursos programados en HORARIO para este salón.");
                    }
                }
            }

            System.out.println("\nFin de la consulta de ocupación para el salón " + idSalon + ".");

        } catch (Exception e) {
            System.err.println("Error al consultar la ocupación del salón.");
            e.printStackTrace();
        }
    }

    // Función para convertir DiaSem (1..7) en algo entendible
    private static String diaSemanaTexto(int d) {
        return switch (d) {
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miércoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            case 6 -> "Sábado";
            case 7 -> "Domingo";
            default -> "Día " + d;
        };
    }

    public static void consultaSalonesLibresEnDia() {

        System.out.println("\n--- T4: Consulta de salones libres en un día ---");

        // 1) Pedir fecha
        System.out.print("Ingrese la fecha (formato YYYY-MM-DD): ");
        String fechaStr = sc.nextLine().trim();

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaStr);  // formato 2025-03-10
        } catch (Exception e) {
            System.out.println("Fecha inválida. Use formato YYYY-MM-DD, por ejemplo 2025-03-10.");
            return;
        }

        // 2) Pedir horas opcionales
        System.out.print("Ingrese hora de inicio (HH:MM) o deje vacío para todo el día: ");
        String horaIniStr = sc.nextLine().trim();

        System.out.print("Ingrese hora de fin (HH:MM) o deje vacío para todo el día: ");
        String horaFinStr = sc.nextLine().trim();

        LocalTime horaIni;
        LocalTime horaFin;

        if (horaIniStr.isEmpty() || horaFinStr.isEmpty()) {

            horaIni = LocalTime.of(0, 0);
            horaFin = LocalTime.of(23, 59);
        } else {
            try {
                horaIni = LocalTime.parse(horaIniStr + (horaIniStr.length() == 5 ? "" : ":00"));
            } catch (Exception e) {
                System.out.println("Hora de inicio inválida. Use HH:MM, ej. 14:30");
                return;
            }

            try {
                horaFin = LocalTime.parse(horaFinStr + (horaFinStr.length() == 5 ? "" : ":00"));
            } catch (Exception e) {
                System.out.println("Hora de fin inválida. Use HH:MM, ej. 16:00");
                return;
            }

            if (!horaFin.isAfter(horaIni)) {
                System.out.println("La hora de fin debe ser mayor que la hora de inicio.");
                return;
            }
        }

        // Intervalo [inicio, fin) en ese día
        LocalDateTime inicio = fecha.atTime(horaIni);
        LocalDateTime fin = fecha.atTime(horaFin);

        Timestamp tsInicio = Timestamp.valueOf(inicio);
        Timestamp tsFin = Timestamp.valueOf(fin);

        // Día de la semana: 1=Lunes,...,7=Domingo
        int diaSem = switch (fecha.getDayOfWeek()) {
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
            case SUNDAY -> 7;
        };

        try (Connection conn = Conexion.getConnection()) {

            //  Obtener todos los salones
            Set<String> todosSalones = new HashSet<>();
            String sqlSal = "SELECT IDSalon FROM Salon";

            try (PreparedStatement psSal = conn.prepareStatement(sqlSal);
                 ResultSet rsSal = psSal.executeQuery()) {

                while (rsSal.next()) {
                    todosSalones.add(rsSal.getString("IDSalon"));
                }
            }

            if (todosSalones.isEmpty()) {
                System.out.println("No hay salones registrados en la base de datos.");
                return;
            }

            // Conjunto de salones ocupados
            Set<String> salonesOcupados = new HashSet<>();

            // Ocupación por RESERVACION
            String sqlRes = """
                SELECT DISTINCT IDSalon
                FROM Reservacion r
                WHERE DATE(r.FechaHora) = ?
                  AND ? < DATE_ADD(r.FechaHora, INTERVAL r.Duracion MINUTE)
                  AND ? > r.FechaHora
                """;

            try (PreparedStatement psRes = conn.prepareStatement(sqlRes)) {
                psRes.setDate(1, Date.valueOf(fecha));
                psRes.setTimestamp(2, tsInicio);
                psRes.setTimestamp(3, tsFin);

                try (ResultSet rs = psRes.executeQuery()) {
                    while (rs.next()) {
                        salonesOcupados.add(rs.getString("IDSalon"));
                    }
                }
            }

            //  Ocupación por HORARIO
            String sqlHor = """
                SELECT h.IDSalon,
                       h.Hora,
                       h.Minuto,
                       h.Duracion
                FROM Horario h
                JOIN Periodo p ON h.Titulo = p.Titulo
                WHERE h.DiaSem = ?
                  AND ? BETWEEN p.FechaInicio AND p.FechaFin
                """;

            try (PreparedStatement psHor = conn.prepareStatement(sqlHor)) {
                psHor.setInt(1, diaSem);
                psHor.setDate(2, Date.valueOf(fecha));

                try (ResultSet rsH = psHor.executeQuery()) {
                    while (rsH.next()) {
                        String idSalon = rsH.getString("IDSalon");
                        int h = rsH.getInt("Hora");
                        int m = rsH.getInt("Minuto");
                        int dur = rsH.getInt("Duracion");

                        LocalTime cursoInicio = LocalTime.of(h, m);
                        LocalTime cursoFin = cursoInicio.plusMinutes(dur);

                        LocalDateTime cursoIniDT = fecha.atTime(cursoInicio);
                        LocalDateTime cursoFinDT = fecha.atTime(cursoFin);

                        boolean seTraslapa =
                                inicio.isBefore(cursoFinDT) &&
                                        fin.isAfter(cursoIniDT);

                        if (seTraslapa) {
                            salonesOcupados.add(idSalon);
                        }
                    }
                }
            }

            //  Calcular y mostrar salones libres
            System.out.println("\n--- Salones libres el " + fecha +
                    " entre " + horaIni + " y " + horaFin + " ---");

            boolean hayLibres = false;

            for (String idS : todosSalones) {
                if (!salonesOcupados.contains(idS)) {
                    hayLibres = true;
                    System.out.println(idS);
                }
            }

            if (!hayLibres) {
                System.out.println("No hay salones libres en ese intervalo.");
            }

            System.out.println("\nFin de la consulta de salones libres.");

        } catch (Exception e) {
            System.err.println("Error al consultar salones libres en un día.");
            e.printStackTrace();
        }
    }

}
