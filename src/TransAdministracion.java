import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class TransAdministracion {
    public static final Scanner sc = new Scanner(System.in);
    // Crear usuario nuevo
    public static void crearUsuario() {
    
        System.out.println("Creación de usuario:");

        //
        int id = -1;

        try (Connection conn = Conexion.getConnection()) {

            id = generarNuevoID(conn);
            System.out.println("ID generado automáticamente: " + id);

            System.out.print("Ingrese nombre: ");
            String nombre = sc.nextLine();

            System.out.println("Tipo de usuario:");
            System.out.println("1) Alumno");
            System.out.println("2) Profesor");
            System.out.println("3) Administrativo");
            int tipoOpcion = Integer.parseInt(sc.nextLine());

            String tipoStr = null;
            if (tipoOpcion == 1) tipoStr = "Alumno";
            else if (tipoOpcion == 2) tipoStr = "Profesor";
            else if (tipoOpcion == 3) tipoStr = "Administrativo";
            else {
                System.out.println("Tipo inválido.");
                return;
            }

            conn.setAutoCommit(false);

            // Insertar en Usuario
            String sqlUsuario = "INSERT INTO Usuario(IDUsuario, Nombre, Tipo) VALUES(?, ?, ?)";
            try (PreparedStatement psU = conn.prepareStatement(sqlUsuario)) {
                psU.setInt(1, id);
                psU.setString(2, nombre);
                psU.setString(3, tipoStr);
                psU.executeUpdate();
            }

            // Si es alumno
            if (tipoStr.equals("Alumno")) {
                System.out.print("Ingrese Carrera del alumno: ");
                String carrera = sc.nextLine();
                String sqlAlumno = "INSERT INTO Alumno(IDUsuario, NombreAlumno, Carrera) VALUES(?, ?, ?)";
                try (PreparedStatement psA = conn.prepareStatement(sqlAlumno)) {
                    psA.setInt(1, id);
                    psA.setString(2, nombre);
                    psA.setString(3, carrera);
                    psA.executeUpdate();
                }
            }

            // Si es profesor
            if (tipoStr.equals("Profesor")) {
                System.out.println("Tipo de contrato de profesor: 1) parcial  2) completo");
                int t = Integer.parseInt(sc.nextLine());
                String tipoProf = (t == 2) ? "completo" : "parcial";

                String sqlProf = "INSERT INTO Profesor(IDUsuario, ProfNom, Tipo) VALUES(?, ?, ?)";
                try (PreparedStatement psP = conn.prepareStatement(sqlProf)) {
                    psP.setInt(1, id);
                    psP.setString(2, nombre);
                    psP.setString(3, tipoProf);
                    psP.executeUpdate();
                }
            }

            conn.commit();
            System.out.println("Usuario creado correctamente en la base de datos.");

        } catch (Exception e) {
            System.err.println("Error al crear usuario");
            e.printStackTrace();
        }
    }
    private static int generarNuevoID(Connection conn) throws SQLException {
        String sql = "SELECT MAX(IDUsuario) FROM Usuario";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int max = rs.getInt(1);
                return max + 1; // nuevo ID
            } else {
                return 1; // tabla vacía
            }
        }
    }
    public static void cambiarProfesorCurso() {

        System.out.println("\n--- Cambiar profesor de un curso ---");

        System.out.print("Ingrese CLAVE del curso: ");
        String clave = sc.nextLine();

        System.out.print("Ingrese secciom del curso (número): ");
        int secc = Integer.parseInt(sc.nextLine());

        System.out.print("Ingrese ID del nuevo profesor: ");
        int nuevoProfID = Integer.parseInt(sc.nextLine());

        try (Connection conn = Conexion.getConnection()) {
            conn.setAutoCommit(false); // inicio de la transacción

            //  Verificar que el curso exista y obtener profesor actual
            String sqlCurso = "SELECT IDUsuario FROM Curso WHERE Clave = ? AND Secc = ?";
            Integer profesorActual = null;

            try (PreparedStatement psCurso = conn.prepareStatement(sqlCurso)) {
                psCurso.setString(1, clave);
                psCurso.setInt(2, secc);

                try (ResultSet rs = psCurso.executeQuery()) {
                    if (rs.next()) {
                        profesorActual = rs.getInt("IDUsuario");
                    } else {
                        System.out.println("No existe un curso con esa clave y sección.");
                        conn.rollback();
                        return;
                    }
                }
            }

            System.out.println("Profesor actual (IDUsuario): " + profesorActual);

            //Verificar que el nuevo profesor exista y sea de tipo Profesor
            String sqlUsuario = "SELECT Tipo FROM Usuario WHERE IDUsuario = ?";
            String tipo = null;

            try (PreparedStatement psUser = conn.prepareStatement(sqlUsuario)) {
                psUser.setInt(1, nuevoProfID);
                try (ResultSet rs = psUser.executeQuery()) {
                    if (rs.next()) {
                        tipo = rs.getString("Tipo");
                    } else {
                        System.out.println("El ID de usuario " + nuevoProfID + " no existe en la tabla Usuario.");
                        conn.rollback();
                        return;
                    }
                }
            }

            if (!"Profesor".equalsIgnoreCase(tipo)) {
                System.out.println("El usuario " + nuevoProfID + " no es de tipo Profesor.");
                conn.rollback();
                return;
            }

            //Verificar que también exista en la tabla Profesor
            String sqlProf = "SELECT COUNT(*) FROM Profesor WHERE IDUsuario = ?";
            try (PreparedStatement psProf = conn.prepareStatement(sqlProf)) {
                psProf.setInt(1, nuevoProfID);
                try (ResultSet rs = psProf.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        System.out.println("El usuario " + nuevoProfID + " no está registrado en la tabla Profesor.");
                        conn.rollback();
                        return;
                    }
                }
            }

            // Si el profesor nuevo es el mismo que el actual, no hacemos nada
            if (profesorActual != null && profesorActual == nuevoProfID) {
                System.out.println("El nuevo profesor es el mismo que el profesor actual. No se realizaron cambios.");
                conn.rollback();
                return;
            }

            // Actualizar el curso con el nuevo profesor
            String sqlUpdate = "UPDATE Curso SET IDUsuario = ? WHERE Clave = ? AND Secc = ?";
            try (PreparedStatement psUpd = conn.prepareStatement(sqlUpdate)) {
                psUpd.setInt(1, nuevoProfID);
                psUpd.setString(2, clave);
                psUpd.setInt(3, secc);

                int filas = psUpd.executeUpdate();
                if (filas > 0) {
                    conn.commit();
                    System.out.println("Profesor del curso actualizado correctamente.");
                } else {
                    System.out.println("No se pudo actualizar el curso (ninguna fila afectada).");
                    conn.rollback();
                }
            }

        } catch (Exception e) {
            System.err.println("Error al cambiar profesor del curso.");
            e.printStackTrace();
        }
    }

}
