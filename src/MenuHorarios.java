import java.sql.*;
import java.util.Scanner;

public class MenuHorarios {
    public static Scanner sc = new Scanner(System.in);
    // obtener el tipo de usuario
    public static String obtenerTipoUsuario(int idUsuario) {
        String tipo = null;
        String sql = "SELECT Tipo FROM Usuario WHERE IDUsuario = ?";

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    tipo = rs.getString("Tipo"); // 'Alumno', 'Profesor' o 'Administrativo'
                }
            }
        } catch (Exception e) {
            System.err.println("Error al consultar tipo de usuario");
            e.printStackTrace();
        }
        return tipo;
    }




    //Menú principal
    public static void portada() {
        int eleccion = -1;

        System.out.println("Bienvenido al sistema de horarios y reservación de salones");
        System.out.println("¿Qué desea hacer?");
        System.out.println("1) Ingresar usuario");
        System.out.println("2) Salir");

        while (eleccion != 1 && eleccion != 2) {
            System.out.print("Ingrese una opción (1 o 2): ");
            try {
                eleccion = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Entrada no válida, intente de nuevo.");
            }
        }

        if (eleccion == 1) {
            System.out.print("Ingrese su ID de usuario: ");
            int id = Integer.parseInt(sc.nextLine());

            String tipo = obtenerTipoUsuario(id);
            if (tipo == null) {
                System.out.println("Usuario no encontrado en la base de datos.");
                return;
            }

            System.out.println("Bienvenido, usuario tipo: " + tipo);

            switch (tipo) {
                case "Alumno":
                    menuAlumno(id);
                    break;
                case "Profesor":
                    menuProfesor(id);
                    break;
                case "Administrativo":
                    menuAdministrativo(id);
                    break;
                default:
                    System.out.println("Tipo de usuario desconocido.");
            }

        } else if (eleccion == 2) {
            System.out.println("Saliendo del sistema...");
            return;
        }
    }

    //Menu alumno
    private static void menuAlumno(int id) {
        while (true) {
            System.out.println("\n--- MENÚ ALUMNO ---");
            System.out.println("1) Ver todos los salones");
            System.out.println("2) Consultar salones por criterio");
            System.out.println("3) Consultar ocupación de un salón");
            System.out.println("4) Consultar salones libres en un día");
            System.out.println("5) Ver horario completo de mi carrera");
            System.out.println("6) Ver horario filtrado");
            System.out.println("7) Reservar salon");
            System.out.println("8) Anular una reservación");
            System.out.println("9) Salir");
            System.out.print("Seleccione una opción: ");
            int op = Integer.parseInt(sc.nextLine());

            switch (op) {
                case 1:
                    System.out.println("\n>>> 1 – Consulta general de salones");
                    TransSalones.consultaGeneral();
                    break;

                case 2:
                    System.out.println("\n>>> 2 – Consulta filtrada de salones");
                    TransSalones.consultaFiltrada();
                    break;

                case 3:
                    System.out.println("\n>>> 3 - Consulta de ocupación de un salón");
                    TransSalones.consultaOcupacionSalon();
                    break;
                case 4:
                    System.out.println("\n>>> 4 – Consulta de salones libres en un día ");
                    TransSalones.consultaSalonesLibresEnDia();
                    break;

                case 5:
                    System.out.println("\n>>> 5.B – Consulta de horario completo del alumno");
                    Transaccion_HORARIO.verHorarioAlumnoCompleto(id);
                    break;
                case 6:
                    System.out.println("\n>>> 6.B – Consulta de horario parcial del alumno");
                    Transaccion_HORARIO.verHorarioParcialAlumno(id);
                    break;
                case 7:
                    System.out.println("\n>>> 7 - Reservar un salón");
                    TransReservas.reservarSalon();
                    return;

                case 8:
                    System.out.println("\n>>> 8 - Anular una reservación");
                    TransReservas.anularReservacion();
                    return;

                case 9:
                    System.out.println("Saliendo del menú Alumno...");
                    return;

                default:
                    System.out.println("Opción inválida. Intente nuevamente.");
            }
        }
    }


    // Menú para Profesor
    private static void menuProfesor(int id) {
        while (true) {
            System.out.println("\n--- MENÚ PROFESOR ---");
            System.out.println("1) Ver todos los salones");
            System.out.println("2) Consultar salones por criterio");
            System.out.println("3) Consultar ocupación de un salón");
            System.out.println("4) Consultar salones libres en un día");
            System.out.println("5) Ver horario completo");
            System.out.println("6) Ver horario parcial");
            System.out.println("7) Reservar salon");
            System.out.println("8) Anular una reservación");
            System.out.println("9) Salir");
            System.out.print("Seleccione una opción: ");

            int op = Integer.parseInt(sc.nextLine());

            switch (op) {
                case 1:
                    System.out.println("\n>>> 1 – Consulta general de salones");
                    TransSalones.consultaGeneral();
                    break;

                case 2:
                    System.out.println("\n>>> 2 – Consulta filtrada de salones");
                    TransSalones.consultaFiltrada();
                    break;

                case 3:
                    System.out.println("\n>>> 3 – Consulta de ocupación de un salón");
                    TransSalones.consultaOcupacionSalon();
                    break;

                case 4:
                    System.out.println("\n>>> 4 – Consulta de salones libres en un día");
                    TransSalones.consultaSalonesLibresEnDia();
                    break;

                case 5:
                    System.out.println("\n>>> 5.A – Consulta de horario completo");
                    Transaccion_HORARIO.verHorarioCompleto(id);
                    break;

                case 6:
                    System.out.println("\n>>> 6.A – Consulta de horario parcial");
                    Transaccion_HORARIO.verHorarioParcial(id);
                    break;

                case 7:
                    System.out.println("\n>>> 7 - Reservar un salón");
                    TransReservas.reservarSalon();
                    return;

                    case 8:
                        System.out.println("\n>>> 8 - Anular una reservación");
                        TransReservas.anularReservacion();
                        return;
                case 9:
                    System.out.println("Saliendo del menú Profesor...");
                    return;



                default:
                    System.out.println("Opción inválida. Intente nuevamente.");
            }
        }
    }

    //Menú para Administrativo
    private static void menuAdministrativo(int id) {
        while (true) {
            System.out.println("\n--- MENÚ ADMINISTRATIVO ---");
            System.out.println("1) Ver todos los salones");
            System.out.println("2) Consultar salones por criterio");
            System.out.println("3) Consultar ocupación de un salón");
            System.out.println("4) Consultar salones libres en un día");
            System.out.println("5) Ver horario completo");
            System.out.println("6) Ver horario parcial");
            System.out.println("7) Reservar un salón");
            System.out.println("8) Anular una reservación");
            System.out.println("9) Agregar entrada al horario");
            System.out.println("10) Modificar programación de un curso");
            System.out.println("11) Cambiar profesor asignado a un curso");
            System.out.println("12) Crear usuario");
            System.out.println("13) Salir");
            System.out.print("Seleccione una opción: ");

            int op = Integer.parseInt(sc.nextLine());

            switch (op) {
                case 1:
                    System.out.println("\n>>> 1 – Consulta general de salones");
                    TransSalones.consultaGeneral();
                    break;

                case 2:
                    System.out.println("\n>>> 2 – Consulta filtrada de salones");
                    TransSalones.consultaFiltrada();
                    break;

                case 3:
                    System.out.println("\n>>> 3 – Consulta de ocupación de un salón");
                    TransSalones.consultaOcupacionSalon();
                    break;

                case 4:
                    System.out.println("\n>>> 4 – Consulta de salones libres en un día");
                    TransSalones.consultaSalonesLibresEnDia();
                    break;

                case 5:
                    System.out.println("\n>>> 5.A – Consulta de horario completo");
                    Transaccion_HORARIO.verHorarioCompleto(id);
                    break;

                case 6:
                    System.out.println("\n>>> 6.A – Consulta de horario parcial");
                    Transaccion_HORARIO.verHorarioParcial(id);
                    break;

                case 7:
                    System.out.println("\n>>> 7 – Reservación de un salón");
                    TransReservas.reservarSalon();
                    break;

                case 8:
                    System.out.println("\n>>> 8 – Anular reservación");
                    TransReservas.anularReservacion();

                    break;

                case 9:
                    System.out.println("\n>>> 9 – Agregar entrada al horario");
                    Transaccion_HORARIO.agregarEntradaHorario();
                    break;

                case 10:
                    System.out.println("\n>>> 10 – Modificar programación de un curso");
                    Transaccion_HORARIO.modificarHorario();;
                    break;

                case 11:
                    System.out.println("\n>>> 11 – Cambiar profesor asignado a un curso");
                    TransAdministracion.cambiarProfesorCurso();
                    break;

                case 12:
                    System.out.println("\n>>> 12 – Crear usuario en el sistema");
                    TransAdministracion.crearUsuario();
                    break;

                case 13:
                    System.out.println("Saliendo del menú Administrativo...");

                    return;

                default:
                    System.out.println("Opción inválida. Intente nuevamente.");
            }

        }
    }
}