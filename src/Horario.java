import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Horario {
    int dia, hora, minuto, duracion, semestre;
    String clave, titulo, salon;
    int secc;

    Horario(int dia, int hora, int minuto, int duracion, int semestre,
            String clave, int secc, String titulo, String salon) {

        this.dia = dia;
        this.hora = hora;
        this.minuto = minuto;
        this.duracion = duracion;
        this.semestre = semestre;
        this.clave = clave;
        this.secc = secc;
        this.titulo = titulo;
        this.salon = salon;
    }
    public static Horario obtenerHorario(Connection conn, String clave, int secc, String titulo) throws Exception {
        String sql = """
            SELECT DiaSem, Hora, Minuto, Duracion, Semestre, IDSalon
            FROM horario
            WHERE Clave = ? AND Secc = ? AND Titulo = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            ps.setInt(2, secc);
            ps.setString(3, titulo);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Horario(
                    rs.getInt("DiaSem"),
                    rs.getInt("Hora"),
                    rs.getInt("Minuto"),
                    rs.getInt("Duracion"),
                    rs.getInt("Semestre"),
                    clave,
                    secc,
                    titulo,
                    rs.getString("IDSalon")
                );
            }
        }
        return null;
    }

}
